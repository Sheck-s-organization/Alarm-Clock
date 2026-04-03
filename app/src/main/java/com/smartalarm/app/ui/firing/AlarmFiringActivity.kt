package com.smartalarm.app.ui.firing

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.databinding.ActivityAlarmFiringBinding
import com.smartalarm.app.receiver.AlarmReceiver
import com.smartalarm.app.receiver.DismissReceiver
import com.smartalarm.app.receiver.SnoozeReceiver
import com.smartalarm.app.service.AlarmService
import kotlinx.coroutines.launch

class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFiringBinding
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)

        if (alarmId != -1L) {
            val repository = (applicationContext as AlarmApplication).repository
            lifecycleScope.launch {
                val alarm = repository.getById(alarmId)
                if (alarm != null) updateSnoozeUi(alarm)
            }
        }

        binding.btnSnooze.setOnClickListener {
            sendBroadcast(
                Intent(this, SnoozeReceiver::class.java)
                    .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            )
            finish()
        }

        binding.btnDismiss.setOnClickListener {
            sendBroadcast(
                Intent(this, DismissReceiver::class.java)
                    .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            )
            finish()
        }
    }

    private fun updateSnoozeUi(alarm: Alarm) {
        val canSnooze = alarm.snoozeCount < alarm.snoozeMaxCount
        binding.btnSnooze.isEnabled = canSnooze
        binding.tvSnoozeInfo.text = getString(
            R.string.snooze_info_format,
            alarm.snoozeDurationMinutes,
            alarm.snoozeCount,
            alarm.snoozeMaxCount
        )
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
