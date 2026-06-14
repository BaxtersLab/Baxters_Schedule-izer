package com.baxter.schedulaizer.alerts

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.baxter.schedulaizer.util.AppConstants

object AlertNotificationBuilder {
    fun build(context: Context, title: String, body: String): Notification {
        return NotificationCompat.Builder(context, AppConstants.CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
    }
}
