package com.baxter.schedulaizer.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.baxter.schedulaizer.util.AppConstants
import com.baxter.schedulaizer.data.db.entity.AlertEntity

class AlertScheduler(private val context: Context) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlert(alert: AlertEntity) {
        val pi = buildPendingIntent(alert.id)
        val whenMs = alert.snoozeUntilMs.takeIf { alert.isSnoozed } ?: alert.fireAtMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }

    fun cancelAlert(alertId: Long) {
        val pi = buildPendingIntent(alertId)
        am.cancel(pi)
    }

    private fun buildPendingIntent(alertId: Long): PendingIntent {
        val intent = Intent(context, AlertReceiver::class.java).apply {
            action = AppConstants.ACTION_FIRE_ALERT
            putExtra(AppConstants.EXTRA_ALERT_ID, alertId)
        }
        val requestCode = (alertId and 0x7fffffff).toInt()
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or pendingImmutableFlag())
    }

    private fun pendingImmutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
