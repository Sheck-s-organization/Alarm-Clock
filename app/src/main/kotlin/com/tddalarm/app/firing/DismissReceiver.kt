package com.tddalarm.app.firing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AlarmRingService.stop(context)
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, DismissReceiver::class.java)
    }
}
