package com.baxter.schedulaizer.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-wide text-to-speech. Initialized once (from [SchedulaizerApp]) and kept
 * alive for the life of the app process so both reminder alerts and the agent can
 * speak at any time.
 *
 * The engine's init is asynchronous, so utterances requested before it is ready are
 * queued and flushed on success — that way the very first spoken alert after a cold
 * start (e.g. an alarm waking the app) isn't silently dropped. Speaking is safe to
 * call from any thread.
 */
object TtsSpeaker {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    private val pending = ConcurrentLinkedQueue<Pair<String, (() -> Unit)?>>()
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private val counter = AtomicInteger(0)

    fun init(context: Context) {
        if (tts != null) return
        val appCtx = context.applicationContext
        val engine = TextToSpeech(appCtx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ready = true
                while (true) {
                    val (msg, done) = pending.poll() ?: break
                    speakNow(msg, done)
                }
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { fireCallback(utteranceId) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { fireCallback(utteranceId) }
            override fun onError(utteranceId: String?, errorCode: Int) { fireCallback(utteranceId) }
        })
        tts = engine
    }

    /**
     * Speak [text]. [onDone] (if provided) is invoked when the utterance finishes or
     * errors — used by [AlertReceiver] to know when it's safe to release the process.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val t = text.trim()
        if (t.isEmpty()) { onDone?.invoke(); return }
        if (ready) speakNow(t, onDone) else pending.add(t to onDone)
    }

    private fun speakNow(text: String, onDone: (() -> Unit)?) {
        val id = "utt_${counter.incrementAndGet()}"
        if (onDone != null) callbacks[id] = onDone
        val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        if (result != TextToSpeech.SUCCESS) {
            callbacks.remove(id)?.invoke()
        }
    }

    private fun fireCallback(utteranceId: String?) {
        utteranceId?.let { callbacks.remove(it)?.invoke() }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Throwable) {}
    }

    fun isReady(): Boolean = ready
}
