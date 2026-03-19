package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val scheduler = AlarmScheduler(context)
        val repository = (context.applicationContext as AlarmApplication).repository
        CoroutineScope(Dispatchers.IO).launch {
            val alarms = repository.allAlarms.first()
            alarms.filter { it.enabled }.forEach { scheduler.schedule(it) }
        }
    }
}
