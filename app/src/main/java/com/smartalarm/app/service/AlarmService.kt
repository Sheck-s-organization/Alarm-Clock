package com.smartalarm.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.smartalarm.app.R
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.ui.alarm.AlarmFiringActivity
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that plays the alarm sound and manages the alarm lifecycle.
 *
 * Features:
 *  - Plays ringtone at configurable volume with optional gradual volume ramp-up
 *  - Vibration with alarm pattern
 *  - Launches AlarmFiringActivity as a fullscreen notification
 *  - Auto-dismisses after [Constants.DEFAULT_DISMISS_TIMEOUT_SECONDS] seconds
 */
class AlarmService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarmId: Long = -1L

    companion object {
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 500, 500, 500, 500)
        private const val VOLUME_RAMP_STEP_MS = 1000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(Constants.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        currentAlarmId = alarmId

        val app = applicationContext as SmartAlarmApplication
        serviceScope.launch {
            val alarm = app.alarmRepository.getAlarmById(alarmId)
            if (alarm != null) {
                startAlarm(alarm)
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startAlarm(alarm: Alarm) {
        // Start as foreground service immediately
        startForeground(
            Constants.NOTIFICATION_ID_ALARM_SERVICE,
            buildAlarmNotification(alarm)
        )

        // Play ringtone
        playRingtone(alarm)

        // Vibrate
        if (alarm.vibrate) startVibration()

        // Schedule auto-dismiss
        serviceScope.launch {
            kotlinx.coroutines.delay(
                Constants.DEFAULT_DISMISS_TIMEOUT_SECONDS * 1000L
            )
            val app = applicationContext as SmartAlarmApplication
            app.alarmRepository.logEvent(
                alarmId = alarm.id,
                event = com.smartalarm.app.data.entities.AlarmEvent.MISSED
            )
            stopSelf()
        }
    }

    private fun playRingtone(alarm: Alarm) {
        val ringtoneUri: Uri = alarm.ringtoneUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(applicationContext, ringtoneUri)
            isLooping = true

            val targetVolume = alarm.volumePercent / 100f

            if (alarm.gradualVolumeSeconds > 0) {
                // Start at 0% volume
                setVolume(0f, 0f)
                prepare()
                start()
                // Ramp up volume gradually
                serviceScope.launch {
                    val steps = alarm.gradualVolumeSeconds
                    val stepVolume = targetVolume / steps
                    for (i in 1..steps) {
                        kotlinx.coroutines.delay(VOLUME_RAMP_STEP_MS)
                        val vol = (stepVolume * i).coerceAtMost(targetVolume)
                        setVolume(vol, vol)
                    }
                }
            } else {
                setVolume(targetVolume, targetVolume)
                prepare()
                start()
            }
        }
    }

    private fun startVibration() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.vibrate(
            VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // 0 = repeat
        )
    }

    private fun buildAlarmNotification(alarm: Alarm): Notification {
        // Full-screen intent to show AlarmFiringActivity
        val fullScreenIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarm.id.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, com.smartalarm.app.receiver.SnoozeReceiver::class.java).apply {
            action = Constants.ACTION_SNOOZE_ALARM
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, (alarm.id * 10 + 2).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, com.smartalarm.app.receiver.DismissReceiver::class.java).apply {
            action = Constants.ACTION_DISMISS_ALARM
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, (alarm.id * 10 + 3).toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label.ifBlank { getString(R.string.alarm_label_default) })
            .setContentText(getString(R.string.alarm_firing_tap_to_dismiss))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_snooze, getString(R.string.action_snooze), snoozePendingIntent)
            .addAction(R.drawable.ic_dismiss, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
