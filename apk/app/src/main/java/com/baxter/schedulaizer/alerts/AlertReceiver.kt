package com.baxter.schedulaizer.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baxter.schedulaizer.util.AppConstants
import com.baxter.schedulaizer.util.TtsSpeaker
import com.baxter.schedulaizer.SchedulaizerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppConstants.ACTION_FIRE_ALERT) return
        val alertId = intent.getLongExtra(AppConstants.EXTRA_ALERT_ID, -1L)
        if (alertId == -1L) return

        // Keep the receiver (and process) alive across the async DB read + spoken
        // alert. Without this the process could be reclaimed before TTS finishes.
        val pendingResult = goAsync()
        val finished = AtomicBoolean(false)
        val release = { if (finished.compareAndSet(false, true)) pendingResult.finish() }
        // Watchdog: always release within 10s even if TTS never reports completion.
        Handler(Looper.getMainLooper()).postDelayed({ release() }, 10_000L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = SchedulaizerApp.get(context)
                val alert = app.database.alertDao().getAlertById(alertId)
                if (alert == null) { release(); return@launch }

                // Build and show notification
                val notif = NotificationCompat.Builder(context, AppConstants.CHANNEL_ALERTS)
                    .setContentTitle(alert.title)
                    .setContentText(alert.body)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(alertId.toInt(), notif)

                // Deactivate the alert
                app.database.alertDao().deactivateAlert(alertId)

                // Speak the reminder aloud, then release the receiver when done.
                val spoken = buildString {
                    append(alert.title)
                    if (alert.body.isNotBlank()) {
                        append(". ")
                        append(alert.body)
                    }
                }
                TtsSpeaker.speak(spoken) { release() }
            } catch (_: Throwable) {
                release()
            }
        }
    }
}
