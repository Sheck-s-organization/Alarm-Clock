package com.smartalarm.app.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartalarm.app.data.entities.*
import com.smartalarm.app.data.repository.AlarmRepository
import com.smartalarm.app.data.repository.HolidayRepository
import com.smartalarm.app.data.repository.LocationRepository
import com.smartalarm.app.data.repository.WorkScheduleRepository
import com.smartalarm.app.receiver.AlarmReceiver
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Central engine that evaluates smart conditions and schedules alarms via AlarmManager.
 *
 * Evaluation order for each alarm at trigger time:
 *  1. Is today a holiday?  → skip (work-schedule alarms)
 *  2. Is today a work day? → skip if not (work-schedule alarms)
 *  3. Time-of-month check  → skip or override time
 *  4. Location check       → skip or override time
 *  5. Charging check       → skip if charging state doesn't match
 */
class SmartAlarmManager(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val workScheduleRepository: WorkScheduleRepository,
    private val holidayRepository: HolidayRepository,
    private val locationRepository: LocationRepository
) {

    private val systemAlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val gson = Gson()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Schedule or reschedule ALL enabled alarms */
    suspend fun scheduleAllAlarms() = withContext(Dispatchers.Default) {
        val alarms = alarmRepository.getAllEnabledAlarms()
        alarms.forEach { scheduleAlarm(it) }
    }

    /** Schedule or reschedule a single alarm */
    suspend fun scheduleAlarm(alarm: Alarm) = withContext(Dispatchers.Default) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return@withContext
        }

        val triggerMillis = computeNextTriggerTime(alarm) ?: run {
            cancelAlarm(alarm)
            return@withContext
        }

        alarmRepository.updateNextTriggerTime(alarm.id, triggerMillis)

        val pendingIntent = buildAlarmPendingIntent(alarm)
        systemAlarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent),
            pendingIntent
        )
    }

    /** Cancel a scheduled alarm */
    fun cancelAlarm(alarm: Alarm) {
        val pendingIntent = buildAlarmPendingIntent(alarm)
        systemAlarmManager.cancel(pendingIntent)
    }

    /**
     * Called at actual alarm trigger time.
     * Returns an [AlarmDecision] describing whether to fire or skip,
     * and what time override (if any) to apply.
     */
    suspend fun evaluateAlarmAtTrigger(alarm: Alarm): AlarmDecision =
        withContext(Dispatchers.Default) {
            val now = Calendar.getInstance()
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH) + 1
            val day = now.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
            val isoDayOfWeek = toIsoDayOfWeek(dayOfWeek)

            // 1. Work-schedule alarms: holiday and work-day checks
            if (alarm.alarmType == AlarmType.WORK_SCHEDULE && alarm.workScheduleId != null) {
                val schedule = workScheduleRepository.getScheduleById(alarm.workScheduleId)
                    ?: return@withContext AlarmDecision.Skip(SkipReason.NOT_WORK_DAY)

                // Holiday check
                if (holidayRepository.isTodayHoliday()) {
                    return@withContext AlarmDecision.Skip(SkipReason.HOLIDAY)
                }

                // Work-day check
                if (!isWorkDay(schedule, now)) {
                    return@withContext AlarmDecision.Skip(SkipReason.NOT_WORK_DAY)
                }

                // Work-schedule repeat check
                val matchesRepeat = when (alarm.workScheduleRepeat) {
                    WorkScheduleRepeat.EVERY_WORKDAY -> true
                    WorkScheduleRepeat.LAST_WORKDAY_OF_WEEK ->
                        isLastWorkdayOfWeek(schedule, now)
                    WorkScheduleRepeat.LAST_WORKDAY_OF_MONTH ->
                        isLastWorkdayOfMonth(schedule, now)
                    WorkScheduleRepeat.LAST_WORKDAY_OF_WEEK_AND_MONTH ->
                        isLastWorkdayOfWeek(schedule, now) || isLastWorkdayOfMonth(schedule, now)
                }
                if (!matchesRepeat) {
                    return@withContext AlarmDecision.Skip(SkipReason.NOT_WORK_DAY)
                }

                // Time-of-month override on work schedule
                val monthOverride = getMonthlyOverride(schedule.monthlyOverridesJson, day)
                if (monthOverride != null) {
                    return@withContext AlarmDecision.FireWithOverride(
                        monthOverride.hour,
                        monthOverride.minute
                    )
                }

                // Day-of-week override on work schedule
                val dayOverride = getDayOverride(schedule.dayOverridesJson, isoDayOfWeek)
                if (dayOverride != null) {
                    return@withContext AlarmDecision.FireWithOverride(
                        dayOverride.hour,
                        dayOverride.minute
                    )
                }

                return@withContext AlarmDecision.Fire
            }

            // 2. Time-of-month alarm
            if (alarm.alarmType == AlarmType.TIME_OF_MONTH && alarm.monthPeriodsJson != null) {
                if (!isInMonthPeriods(alarm.monthPeriodsJson, day)) {
                    return@withContext AlarmDecision.Skip(SkipReason.WRONG_TIME_OF_MONTH)
                }
            }

            // 3. Location alarm
            if (alarm.alarmType == AlarmType.LOCATION && alarm.locationRuleJson != null) {
                val locationDecision = evaluateLocationRules(alarm.locationRuleJson, isoDayOfWeek)
                if (locationDecision != null) return@withContext locationDecision
            }

            // 4. Charging alarm
            if (alarm.alarmType == AlarmType.CHARGING &&
                alarm.chargingRequirement != ChargingRequirement.NOT_REQUIRED
            ) {
                val isCharging = isPhoneCharging()
                val shouldFire = when (alarm.chargingRequirement) {
                    ChargingRequirement.CHARGING -> isCharging
                    ChargingRequirement.NOT_CHARGING -> !isCharging
                    ChargingRequirement.NOT_REQUIRED -> true
                }
                if (!shouldFire) {
                    return@withContext AlarmDecision.Skip(SkipReason.CHARGING_MISMATCH)
                }
            }

            // Also apply charging check to any alarm that has one
            if (alarm.chargingRequirement != ChargingRequirement.NOT_REQUIRED) {
                val isCharging = isPhoneCharging()
                val shouldFire = when (alarm.chargingRequirement) {
                    ChargingRequirement.CHARGING -> isCharging
                    ChargingRequirement.NOT_CHARGING -> !isCharging
                    ChargingRequirement.NOT_REQUIRED -> true
                }
                if (!shouldFire) {
                    return@withContext AlarmDecision.Skip(SkipReason.CHARGING_MISMATCH)
                }
            }

            AlarmDecision.Fire
        }

    // -------------------------------------------------------------------------
    // Next trigger time calculation
    // -------------------------------------------------------------------------

    private suspend fun computeNextTriggerTime(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        val today = now.clone() as Calendar
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        today.set(Calendar.HOUR_OF_DAY, alarm.hour)
        today.set(Calendar.MINUTE, alarm.minute)

        // If the time has already passed today, start search from tomorrow
        val startCal = if (today.timeInMillis <= now.timeInMillis) {
            (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        } else {
            today
        }

        return when (alarm.alarmType) {
            AlarmType.STANDARD -> computeStandardNextTime(alarm, startCal)
            AlarmType.WORK_SCHEDULE -> computeWorkScheduleNextTime(alarm, startCal)
            AlarmType.TIME_OF_MONTH -> computeTimeOfMonthNextTime(alarm, startCal)
            AlarmType.LOCATION -> startCal.timeInMillis // fires daily, evaluated at trigger
            AlarmType.CHARGING -> startCal.timeInMillis  // fires daily, evaluated at trigger
        }
    }

    private fun computeStandardNextTime(alarm: Alarm, startCal: Calendar): Long? {
        if (alarm.repeatDays.isEmpty()) {
            // One-time alarm — schedule for the next occurrence
            return startCal.timeInMillis
        }
        // Repeating: find next matching day-of-week
        val cal = startCal.clone() as Calendar
        for (i in 0..6) {
            val isoDow = toIsoDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
            if (isoDow in alarm.repeatDays) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    private suspend fun computeWorkScheduleNextTime(alarm: Alarm, startCal: Calendar): Long? {
        val scheduleId = alarm.workScheduleId ?: return null
        val schedule = workScheduleRepository.getScheduleById(scheduleId) ?: return null

        val cal = startCal.clone() as Calendar
        // For last-workday-of-month variants the next occurrence could be up to ~35 days away
        val searchDays = when (alarm.workScheduleRepeat) {
            WorkScheduleRepeat.LAST_WORKDAY_OF_MONTH,
            WorkScheduleRepeat.LAST_WORKDAY_OF_WEEK_AND_MONTH -> 35
            else -> 14
        }
        for (i in 0 until searchDays) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val holidays = holidayRepository.getHolidaysForDate(year, month, day)
            val isHoliday = holidays.isNotEmpty()

            if (!isHoliday && isWorkDay(schedule, cal)) {
                val matchesRepeat = when (alarm.workScheduleRepeat) {
                    WorkScheduleRepeat.EVERY_WORKDAY -> true
                    WorkScheduleRepeat.LAST_WORKDAY_OF_WEEK ->
                        isLastWorkdayOfWeek(schedule, cal)
                    WorkScheduleRepeat.LAST_WORKDAY_OF_MONTH ->
                        isLastWorkdayOfMonth(schedule, cal)
                    WorkScheduleRepeat.LAST_WORKDAY_OF_WEEK_AND_MONTH ->
                        isLastWorkdayOfWeek(schedule, cal) || isLastWorkdayOfMonth(schedule, cal)
                }

                if (matchesRepeat) {
                    // Apply time-of-month or day-of-week overrides
                    val monthOverride = getMonthlyOverride(schedule.monthlyOverridesJson, day)
                    val dayOverride = getDayOverride(
                        schedule.dayOverridesJson,
                        toIsoDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
                    )
                    val overrideHour = monthOverride?.hour ?: dayOverride?.hour
                    val overrideMinute = monthOverride?.minute ?: dayOverride?.minute
                    val triggerCal = cal.clone() as Calendar
                    triggerCal.set(Calendar.HOUR_OF_DAY, overrideHour ?: alarm.hour)
                    triggerCal.set(Calendar.MINUTE, overrideMinute ?: alarm.minute)
                    triggerCal.set(Calendar.SECOND, 0)
                    triggerCal.set(Calendar.MILLISECOND, 0)
                    return triggerCal.timeInMillis
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    private fun computeTimeOfMonthNextTime(alarm: Alarm, startCal: Calendar): Long? {
        if (alarm.monthPeriodsJson == null) return null
        val cal = startCal.clone() as Calendar
        for (i in 0..60) {
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (isInMonthPeriods(alarm.monthPeriodsJson, day)) {
                return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Work day evaluation
    // -------------------------------------------------------------------------

    private fun isWorkDay(schedule: WorkSchedule, cal: Calendar): Boolean {
        return when (schedule.scheduleType) {
            ScheduleType.FIXED_DAYS -> {
                val isoDow = toIsoDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
                isoDow in schedule.workDays
            }
            ScheduleType.ROTATING -> {
                val startDate = schedule.rotationStartDate ?: return false
                val cycles = parseShiftCycles(schedule.shiftCyclesJson) ?: return false
                val totalCycleDays = cycles.sumOf { it.durationDays }
                if (totalCycleDays == 0) return false

                val diffDays = daysBetween(startDate, cal.timeInMillis)
                val posInCycle = (diffDays % totalCycleDays).toInt()

                var cumulative = 0
                for (cycle in cycles) {
                    cumulative += cycle.durationDays
                    if (posInCycle < cumulative) return cycle.isWorkDay
                }
                false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Override helpers
    // -------------------------------------------------------------------------

    private fun getMonthlyOverride(json: String?, dayOfMonth: Int): MonthlyTimeOverride? {
        if (json == null) return null
        return try {
            val type = object : TypeToken<List<MonthlyTimeOverride>>() {}.type
            val overrides: List<MonthlyTimeOverride> = gson.fromJson(json, type) ?: return null
            overrides.firstOrNull { dayOfMonth >= it.startDay && dayOfMonth <= it.endDay }
        } catch (e: Exception) { null }
    }

    private fun getDayOverride(json: String?, isoDayOfWeek: Int): DayTimeOverride? {
        if (json == null) return null
        return try {
            val type = object : TypeToken<List<DayTimeOverride>>() {}.type
            val overrides: List<DayTimeOverride> = gson.fromJson(json, type) ?: return null
            overrides.firstOrNull { it.dayOfWeek == isoDayOfWeek }
        } catch (e: Exception) { null }
    }

    private fun isInMonthPeriods(json: String, dayOfMonth: Int): Boolean {
        return try {
            val type = object : TypeToken<List<MonthPeriod>>() {}.type
            val periods: List<MonthPeriod> = gson.fromJson(json, type) ?: return false
            periods.any { dayOfMonth >= it.startDay && dayOfMonth <= it.endDay }
        } catch (e: Exception) { false }
    }

    private suspend fun evaluateLocationRules(json: String, isoDayOfWeek: Int): AlarmDecision? {
        return try {
            val type = object : TypeToken<List<AlarmLocationRule>>() {}.type
            val rules: List<AlarmLocationRule> = gson.fromJson(json, type) ?: return null

            for (rule in rules) {
                // Skip rule if it doesn't apply to today's day of week
                if (rule.activeDays.isNotEmpty() && isoDayOfWeek !in rule.activeDays) continue

                val isInside = locationRepository.isInsideLocation(rule.savedLocationId)
                val matches = when (rule.triggerWhen) {
                    LocationTrigger.INSIDE -> isInside
                    LocationTrigger.OUTSIDE -> !isInside
                }

                if (!matches) return AlarmDecision.Skip(SkipReason.LOCATION_MISMATCH)

                // Return time override if defined
                if (rule.overrideHour != null && rule.overrideMinute != null) {
                    return AlarmDecision.FireWithOverride(rule.overrideHour, rule.overrideMinute)
                }
            }
            null // All rules passed, no override
        } catch (e: Exception) { null }
    }

    // -------------------------------------------------------------------------
    // Charging state
    // -------------------------------------------------------------------------

    fun isPhoneCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }

    // -------------------------------------------------------------------------
    // PendingIntent factory
    // -------------------------------------------------------------------------

    private fun buildAlarmPendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_FIRE_ALARM
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // -------------------------------------------------------------------------
    // Last-workday helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if [cal] is the last work day remaining in its ISO week
     * (i.e. no later work day exists from tomorrow through Sunday).
     */
    private suspend fun isLastWorkdayOfWeek(schedule: WorkSchedule, cal: Calendar): Boolean {
        val check = cal.clone() as Calendar
        check.add(Calendar.DAY_OF_YEAR, 1)
        // Walk forward until we reach the next Monday (start of the next ISO week)
        while (toIsoDayOfWeek(check.get(Calendar.DAY_OF_WEEK)) != 1) {
            val y = check.get(Calendar.YEAR)
            val m = check.get(Calendar.MONTH) + 1
            val d = check.get(Calendar.DAY_OF_MONTH)
            val isHoliday = holidayRepository.getHolidaysForDate(y, m, d).isNotEmpty()
            if (!isHoliday && isWorkDay(schedule, check)) return false
            check.add(Calendar.DAY_OF_YEAR, 1)
        }
        return true
    }

    /**
     * Returns true if [cal] is the last work day remaining in its calendar month
     * (i.e. no later work day exists from tomorrow through the end of the month).
     */
    private suspend fun isLastWorkdayOfMonth(schedule: WorkSchedule, cal: Calendar): Boolean {
        val check = cal.clone() as Calendar
        val currentMonth = cal.get(Calendar.MONTH)
        check.add(Calendar.DAY_OF_YEAR, 1)
        while (check.get(Calendar.MONTH) == currentMonth) {
            val y = check.get(Calendar.YEAR)
            val m = check.get(Calendar.MONTH) + 1
            val d = check.get(Calendar.DAY_OF_MONTH)
            val isHoliday = holidayRepository.getHolidaysForDate(y, m, d).isNotEmpty()
            if (!isHoliday && isWorkDay(schedule, check)) return false
            check.add(Calendar.DAY_OF_YEAR, 1)
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) → ISO (Mon=1..Sun=7) */
    private fun toIsoDayOfWeek(javaDayOfWeek: Int): Int =
        if (javaDayOfWeek == Calendar.SUNDAY) 7 else javaDayOfWeek - 1

    private fun daysBetween(startMillis: Long, endMillis: Long): Long {
        val startDay = startMillis / (24 * 60 * 60 * 1000L)
        val endDay = endMillis / (24 * 60 * 60 * 1000L)
        return endDay - startDay
    }

    private fun parseShiftCycles(json: String?): List<ShiftCycle>? {
        if (json == null) return null
        return try {
            val type = object : TypeToken<List<ShiftCycle>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { null }
    }
}

/** A simple month period (day range within a month) */
data class MonthPeriod(
    val startDay: Int,
    val endDay: Int,
    val label: String = ""
)

/** Result of smart alarm evaluation */
sealed class AlarmDecision {
    object Fire : AlarmDecision()
    data class FireWithOverride(val hour: Int, val minute: Int) : AlarmDecision()
    data class Skip(val reason: SkipReason) : AlarmDecision()
}
