package com.smartalarm.app.util

object Constants {

    // Intent actions
    const val ACTION_FIRE_ALARM = "com.smartalarm.app.ACTION_FIRE_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.smartalarm.app.ACTION_SNOOZE_ALARM"
    const val ACTION_DISMISS_ALARM = "com.smartalarm.app.ACTION_DISMISS_ALARM"
    const val ACTION_SHOW_ALARM = "com.smartalarm.app.ACTION_SHOW_ALARM"

    // Intent extras
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE_COUNT = "snooze_count"
    const val EXTRA_IS_SNOOZE = "is_snooze"

    // Notification channels
    const val CHANNEL_ID_ALARM = "channel_alarm"
    const val CHANNEL_ID_UPCOMING = "channel_upcoming"
    const val CHANNEL_ID_GENERAL = "channel_general"

    // Notification IDs
    const val NOTIFICATION_ID_ALARM_SERVICE = 1001
    const val NOTIFICATION_ID_UPCOMING_BASE = 2000

    // Preferences
    const val PREF_COUNTRY_CODE = "pref_country_code"
    const val PREF_DEFAULT_SNOOZE = "pref_default_snooze"
    const val PREF_VIBRATE_DEFAULT = "pref_vibrate_default"
    const val PREF_VOLUME_DEFAULT = "pref_volume_default"
    const val PREF_GRADUAL_VOLUME = "pref_gradual_volume"
    const val PREF_LOCATION_ENABLED = "pref_location_enabled"
    const val PREF_DISMISS_TIMEOUT = "pref_dismiss_timeout"

    // Default values
    const val DEFAULT_SNOOZE_MINUTES = 9
    const val DEFAULT_SNOOZE_MAX_COUNT = 3
    const val DEFAULT_DISMISS_TIMEOUT_SECONDS = 120
    const val DEFAULT_GEOFENCE_RADIUS_METERS = 200f

    // Days of week (ISO 8601)
    const val MONDAY = 1
    const val TUESDAY = 2
    const val WEDNESDAY = 3
    const val THURSDAY = 4
    const val FRIDAY = 5
    const val SATURDAY = 6
    const val SUNDAY = 7

    val WEEKDAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    val WEEKEND = setOf(SATURDAY, SUNDAY)
    val ALL_DAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
}
