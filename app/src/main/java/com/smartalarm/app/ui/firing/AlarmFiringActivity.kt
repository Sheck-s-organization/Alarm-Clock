package com.smartalarm.app.ui.firing

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.smartalarm.app.databinding.ActivityAlarmFiringBinding
import com.smartalarm.app.service.AlarmService

class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFiringBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSnooze.isEnabled = false  // Snooze implemented in Phase 4
        binding.btnDismiss.setOnClickListener {
            AlarmService.stop(this)
            finish()
        }
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
