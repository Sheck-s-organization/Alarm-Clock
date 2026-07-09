package com.tddalarm.app.firing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.tddalarm.app.databinding.ActivityAlarmFiringBinding

/** Full-screen dismiss/snooze screen, shown over the lock screen. */
class AlarmFiringActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        binding.timeText.text = intent.getStringExtra(EXTRA_TIME_TEXT).orEmpty()
        binding.labelText.text = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        binding.dismissButton.setOnClickListener {
            sendBroadcast(DismissReceiver.intent(this))
            finish()
        }
        binding.snoozeButton.setOnClickListener {
            sendBroadcast(SnoozeReceiver.intent(this, alarmId))
            finish()
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "label"
        const val EXTRA_TIME_TEXT = "time_text"

        fun intent(context: Context, alarmId: Long, label: String, timeText: String): Intent =
            Intent(context, AlarmFiringActivity::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_TIME_TEXT, timeText)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
