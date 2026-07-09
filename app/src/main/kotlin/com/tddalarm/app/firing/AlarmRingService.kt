package com.tddalarm.app.firing

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.R
import com.tddalarm.core.alarm.Alarm
import java.util.Locale

class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(EXTRA_LABEL).orEmpty()
        val timeText = intent?.getStringExtra(EXTRA_TIME_TEXT).orEmpty()

        val notification = buildNotification(alarmId, label, timeText)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        startRinging()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }

    private fun buildNotification(alarmId: Long, label: String, timeText: String): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            0,
            AlarmFiringActivity.intent(this, alarmId, label, timeText),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getBroadcast(
            this,
            1,
            DismissReceiver.intent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            this,
            2,
            SnoozeReceiver.intent(this, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, AlarmApplication.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label.ifEmpty { getString(R.string.alarm_firing_title) })
            .setContentText(timeText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, getString(R.string.dismiss), dismissIntent)
            .addAction(0, getString(R.string.snooze), snoozeIntent)
            .build()
    }

    private fun startRinging() {
        if (mediaPlayer != null) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (alarmUri != null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }

        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "label"
        const val EXTRA_TIME_TEXT = "time_text"

        fun start(context: Context, alarm: Alarm) {
            val timeText = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
            val intent = Intent(context, AlarmRingService::class.java)
                .putExtra(EXTRA_ALARM_ID, alarm.id)
                .putExtra(EXTRA_LABEL, alarm.label)
                .putExtra(EXTRA_TIME_TEXT, timeText)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }
    }
}
