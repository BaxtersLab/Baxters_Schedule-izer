package com.baxter.schedulaizer.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baxter.schedulaizer.util.AppConstants
import com.baxter.schedulaizer.util.AlarmSoundPlayer
import com.baxter.schedulaizer.util.AlarmTimeUtils
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

        // Keep the receiver (and process) alive across the async DB read, the tone,
        // and the spoken alert. Without this the process could be reclaimed mid-sound.
        val pendingResult = goAsync()
        val finished = AtomicBoolean(false)
        val release = {
            if (finished.compareAndSet(false, true)) {
                AlarmSoundPlayer.stop()
                pendingResult.finish()
            }
        }
        // Watchdog: always release within 60s even if the tone/TTS never report done.
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ release() }, 60_000L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = SchedulaizerApp.get(context)
                val dao = app.database.alertDao()
                val alert = dao.getAlertById(alertId)
                if (alert == null) { release(); return@launch }

                // Show the (silent) heads-up notification.
                val notif = NotificationCompat.Builder(context, AppConstants.CHANNEL_ALERTS)
                    .setContentTitle(alert.title)
                    .setContentText(alert.body)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(alertId.toInt(), notif)

                // Re-arm a repeating alarm for its next daily firing; otherwise retire it.
                if (alert.parentType == "alarm" && alert.repeatDaily) {
                    val next = AlarmTimeUtils.nextDailyMs(alert.fireAtMs)
                    val updated = alert.copy(
                        fireAtMs = next, isActive = true, isSnoozed = false, snoozeUntilMs = -1L
                    )
                    dao.updateAlert(updated)
                    runCatching { AlertScheduler(context).scheduleAlert(updated) }
                } else {
                    dao.deactivateAlert(alertId)
                }

                // Resolve the tone: per-alert override, else the global setting, else
                // the system default alarm sound (handled inside AlarmSoundPlayer).
                val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                val globalTone = prefs.getString(AppConstants.PREF_ALARM_TONE_URI, null)
                val tone = AlarmSoundPlayer.chooseToneUri(alert.soundUri, globalTone)

                val spoken = buildString {
                    append(alert.title)
                    if (alert.body.isNotBlank()) {
                        append(". ")
                        append(alert.body)
                    }
                }

                // Play the tone, then speak, then release. Posted to the main looper so
                // the MediaPlayer's prepared/completion callbacks have a live Looper.
                handler.post {
                    AlarmSoundPlayer.play(context, tone) {
                        TtsSpeaker.speak(spoken) { release() }
                    }
                }
            } catch (_: Throwable) {
                release()
            }
        }
    }
}
