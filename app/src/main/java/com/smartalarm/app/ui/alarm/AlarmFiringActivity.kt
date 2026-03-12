package com.smartalarm.app.ui.alarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartalarm.app.R
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.databinding.ActivityAlarmFiringBinding
import com.smartalarm.app.receiver.DismissReceiver
import com.smartalarm.app.receiver.SnoozeReceiver
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen activity shown when an alarm fires.
 * Displayed over the lock screen so the user can snooze or dismiss without unlocking.
 */
class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFiringBinding
    private var alarmId: Long = -1L
    private var snoozeCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        snoozeCount = intent.getIntExtra(Constants.EXTRA_SNOOZE_COUNT, 0)

        if (alarmId == -1L) {
            finish()
            return
        }

        // Show current time
        binding.tvFiringTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date())

        // Load alarm label
        val app = applicationContext as SmartAlarmApplication
        lifecycleScope.launch {
            val alarm = app.alarmRepository.getAlarmById(alarmId)
            binding.tvAlarmLabel.text = alarm?.label?.ifBlank {
                getString(R.string.alarm_label_default)
            } ?: getString(R.string.alarm_label_default)

            // Hide snooze button if max snooze count reached
            alarm?.let {
                if (snoozeCount >= it.snoozeMaxCount) {
                    binding.btnSnooze.isEnabled = false
                    binding.btnSnooze.alpha = 0.4f
                }
                binding.tvSnoozeInfo.text = getString(
                    R.string.snooze_info_format,
                    it.snoozeDurationMinutes,
                    snoozeCount,
                    it.snoozeMaxCount
                )
            }
        }

        binding.btnDismiss.setOnClickListener {
            sendBroadcast(
                Intent(this, DismissReceiver::class.java).apply {
                    action = Constants.ACTION_DISMISS_ALARM
                    putExtra(Constants.EXTRA_ALARM_ID, alarmId)
                    putExtra(Constants.EXTRA_SNOOZE_COUNT, snoozeCount)
                }
            )
            finish()
        }

        binding.btnSnooze.setOnClickListener {
            sendBroadcast(
                Intent(this, SnoozeReceiver::class.java).apply {
                    action = Constants.ACTION_SNOOZE_ALARM
                    putExtra(Constants.EXTRA_ALARM_ID, alarmId)
                    putExtra(Constants.EXTRA_SNOOZE_COUNT, snoozeCount)
                }
            )
            finish()
        }
    }
}
