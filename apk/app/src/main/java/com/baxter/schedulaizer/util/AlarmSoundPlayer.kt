package com.baxter.schedulaizer.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays an alarm/alert tone from the broadcast receiver on the **alarm** audio
 * stream, so it is audible even when the ringer is silent. Playback is one-shot:
 * the tone plays once and [play]'s `onComplete` fires when it finishes, errors, or
 * cannot start — that callback is what lets [com.baxter.schedulaizer.alerts.AlertReceiver]
 * chain the spoken alert and then release its `goAsync` token.
 *
 * Only one tone plays at a time; a new [play] (or [stop]) tears down the previous
 * one. This deliberately does not loop-until-dismissed — that would require a
 * foreground service / full-screen alarm activity, which is out of scope here.
 */
object AlarmSoundPlayer {
    @Volatile private var player: MediaPlayer? = null

    /** Per-alert tone wins; otherwise the global tone; otherwise null (use default). */
    fun chooseToneUri(perAlert: String?, global: String?): String? =
        perAlert?.takeIf { it.isNotBlank() } ?: global?.takeIf { it.isNotBlank() }

    fun play(context: Context, toneUri: String?, onComplete: () -> Unit) {
        stop()

        val uri: Uri = toneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
            ?: run { onComplete(); return }

        val mp = MediaPlayer()
        val done = AtomicBoolean(false)
        val finish = {
            if (done.compareAndSet(false, true)) {
                runCatching { mp.release() }
                if (player === mp) player = null
                onComplete()
            }
        }

        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.isLooping = false
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ -> finish(); true }
            mp.setOnPreparedListener { it.start() }
            player = mp
            mp.prepareAsync()
        } catch (_: Throwable) {
            finish()
        }
    }

    fun stop() {
        player?.let { p ->
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        player = null
    }
}
