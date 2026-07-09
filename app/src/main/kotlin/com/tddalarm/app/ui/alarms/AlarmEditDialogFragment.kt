package com.tddalarm.app.ui.alarms

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.R
import com.tddalarm.app.data.daysFromStorageString
import com.tddalarm.app.data.toStorageString
import com.tddalarm.app.databinding.DialogAlarmEditBinding
import com.tddalarm.app.ui.AlarmListViewModel
import com.tddalarm.app.ui.ViewModelFactory
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import java.time.DayOfWeek
import kotlinx.coroutines.launch

class AlarmEditDialogFragment : DialogFragment() {

    private val viewModel: AlarmListViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAlarmEditBinding.inflate(layoutInflater)
        val args = requireArguments()
        val alarmId = args.getLong(ARG_ID)
        val isNew = alarmId == 0L

        binding.timePicker.setIs24HourView(true)
        binding.timePicker.hour = args.getInt(ARG_HOUR)
        binding.timePicker.minute = args.getInt(ARG_MINUTE)
        binding.labelInput.setText(args.getString(ARG_LABEL).orEmpty())
        binding.skipDaysOffSwitch.isChecked = args.getBoolean(ARG_SKIP_DAYS_OFF)

        val dayChips = mapOf(
            DayOfWeek.MONDAY to binding.chipMon,
            DayOfWeek.TUESDAY to binding.chipTue,
            DayOfWeek.WEDNESDAY to binding.chipWed,
            DayOfWeek.THURSDAY to binding.chipThu,
            DayOfWeek.FRIDAY to binding.chipFri,
            DayOfWeek.SATURDAY to binding.chipSat,
            DayOfWeek.SUNDAY to binding.chipSun,
        )
        val repeatDays = daysFromStorageString(args.getString(ARG_REPEAT_DAYS).orEmpty())
        dayChips.forEach { (day, chip) -> chip.isChecked = day in repeatDays }

        val hasLocation = args.getBoolean(ARG_HAS_LOCATION)
        binding.locationSwitch.isChecked = hasLocation
        binding.locationFields.isVisible = hasLocation
        if (hasLocation) {
            binding.latitudeInput.setText(args.getDouble(ARG_LATITUDE).toString())
            binding.longitudeInput.setText(args.getDouble(ARG_LONGITUDE).toString())
            binding.radiusInput.setText((args.getDouble(ARG_RADIUS_METERS) / 1000.0).toString())
        }
        binding.fireWhenUnknownSwitch.isChecked = args.getBoolean(ARG_FIRE_WHEN_UNKNOWN, true)
        binding.locationSwitch.setOnCheckedChangeListener { _, checked ->
            binding.locationFields.isVisible = checked
        }

        binding.useCurrentLocationButton.setOnClickListener {
            val provider = AlarmApplication.from(requireContext()).container.locationProvider
            lifecycleScope.launch {
                val point = provider.lastKnown()
                if (point == null) {
                    Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
                } else {
                    binding.latitudeInput.setText(point.latitude.toString())
                    binding.longitudeInput.setText(point.longitude.toString())
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isNew) R.string.add_alarm else R.string.edit_alarm)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.save(buildAlarm(binding, alarmId, dayChips, enabled = args.getBoolean(ARG_ENABLED, true)))
            }
            .setNegativeButton(R.string.cancel, null)
        if (!isNew) {
            builder.setNeutralButton(R.string.delete) { _, _ ->
                viewModel.delete(buildAlarm(binding, alarmId, dayChips, enabled = false))
            }
        }
        return builder.create()
    }

    private fun buildAlarm(
        binding: DialogAlarmEditBinding,
        alarmId: Long,
        dayChips: Map<DayOfWeek, com.google.android.material.chip.Chip>,
        enabled: Boolean,
    ): Alarm = Alarm(
        id = alarmId,
        label = binding.labelInput.text.toString().trim(),
        hour = binding.timePicker.hour,
        minute = binding.timePicker.minute,
        repeatDays = dayChips.filterValues { it.isChecked }.keys,
        enabled = enabled,
        skipOnDaysOff = binding.skipDaysOffSwitch.isChecked,
        locationRule = buildLocationRule(binding),
    )

    private fun buildLocationRule(binding: DialogAlarmEditBinding): LocationRule? {
        if (!binding.locationSwitch.isChecked) return null
        val lat = binding.latitudeInput.text.toString().toDoubleOrNull()
        val lng = binding.longitudeInput.text.toString().toDoubleOrNull()
        val radiusKm = binding.radiusInput.text.toString().toDoubleOrNull()
        if (lat == null || lng == null || radiusKm == null || radiusKm <= 0) {
            Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
            return null
        }
        return LocationRule(
            fence = GeoFence(GeoPoint(lat, lng), radiusKm * 1000.0),
            fireWhenLocationUnknown = binding.fireWhenUnknownSwitch.isChecked,
        )
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_LABEL = "label"
        private const val ARG_HOUR = "hour"
        private const val ARG_MINUTE = "minute"
        private const val ARG_REPEAT_DAYS = "repeat_days"
        private const val ARG_ENABLED = "enabled"
        private const val ARG_SKIP_DAYS_OFF = "skip_days_off"
        private const val ARG_HAS_LOCATION = "has_location"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_RADIUS_METERS = "radius_meters"
        private const val ARG_FIRE_WHEN_UNKNOWN = "fire_when_unknown"

        fun forNewAlarm(): AlarmEditDialogFragment = AlarmEditDialogFragment().apply {
            arguments = bundleOf(ARG_ID to 0L, ARG_HOUR to 7, ARG_MINUTE to 0)
        }

        fun forAlarm(alarm: Alarm): AlarmEditDialogFragment = AlarmEditDialogFragment().apply {
            arguments = bundleOf(
                ARG_ID to alarm.id,
                ARG_LABEL to alarm.label,
                ARG_HOUR to alarm.hour,
                ARG_MINUTE to alarm.minute,
                ARG_REPEAT_DAYS to alarm.repeatDays.toStorageString(),
                ARG_ENABLED to alarm.enabled,
                ARG_SKIP_DAYS_OFF to alarm.skipOnDaysOff,
                ARG_HAS_LOCATION to (alarm.locationRule != null),
                ARG_LATITUDE to (alarm.locationRule?.fence?.center?.latitude ?: 0.0),
                ARG_LONGITUDE to (alarm.locationRule?.fence?.center?.longitude ?: 0.0),
                ARG_RADIUS_METERS to (alarm.locationRule?.fence?.radiusMeters ?: 0.0),
                ARG_FIRE_WHEN_UNKNOWN to (alarm.locationRule?.fireWhenLocationUnknown ?: true),
            )
        }
    }
}
