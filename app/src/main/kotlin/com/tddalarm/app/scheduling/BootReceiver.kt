package com.tddalarm.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tddalarm.app.AlarmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** AlarmManager registrations do not survive a reboot; re-register everything. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container = AlarmApplication.from(context).container
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.alarmStore.enabledAlarms().forEach(container.scheduler::scheduleNext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
