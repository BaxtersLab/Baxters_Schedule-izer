package com.baxter.schedulaizer.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baxter.schedulaizer.SchedulaizerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val app = SchedulaizerApp.get(context)
            val alerts = app.database.alertDao().getActiveAlerts().first()
            val scheduler = AlertScheduler(context)
            alerts.forEach { scheduler.scheduleAlert(it) }
        }
    }
}
