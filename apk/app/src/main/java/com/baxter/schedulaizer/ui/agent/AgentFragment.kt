package com.baxter.schedulaizer.ui.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.baxter.schedulaizer.databinding.FragmentAgentBinding
import com.baxter.schedulaizer.util.TtsSpeaker
import com.baxter.schedulaizer.voice.AgentDispatcher
import com.baxter.schedulaizer.voice.IntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentFragment : Fragment() {

    private var _binding: FragmentAgentBinding? = null
    private val binding get() = _binding!!

    private lateinit var dispatcher: AgentDispatcher
    private var speechRecognizer: SpeechRecognizer? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var micTimeout: Runnable? = null
    private var isListening = false

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceCapture()
        else Toast.makeText(requireContext(), "Microphone permission needed for voice", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dispatcher = AgentDispatcher(requireContext().applicationContext)

        binding.sendButton.setOnClickListener { submitFromInput() }
        binding.inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitFromInput()
                true
            } else false
        }

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            binding.micButton.visibility = View.GONE
        }
        binding.micButton.setOnClickListener {
            if (isListening) {
                stopVoiceCapture()
                return@setOnClickListener
            }
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startVoiceCapture() else requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun submitFromInput() {
        val text = binding.inputText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.inputText.setText("")
        submit(text)
    }

    private fun submit(utterance: String) {
        appendLine("You: $utterance")
        viewLifecycleOwner.lifecycleScope.launch {
            val intent = IntentParser.parse(utterance)
            val response = try {
                withContext(Dispatchers.IO) { dispatcher.dispatch(intent) }
            } catch (e: Exception) {
                "Sorry, that didn't work: ${e.message ?: "unknown error"}"
            }
            appendLine("Agent: $response")
            TtsSpeaker.speak(response)
        }
    }

    private fun appendLine(line: String) {
        val b = _binding ?: return
        val existing = b.transcriptText.text
        b.transcriptText.text = if (existing.isNullOrEmpty()) line else "$existing\n\n$line"
        b.transcriptScroll.post { _binding?.transcriptScroll?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startVoiceCapture() {
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer = recognizer
        isListening = true
        updateMicState()
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(requireContext(), "Listening… tap to stop", Toast.LENGTH_SHORT).show()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                clearRecognizer(recognizer)
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                clearRecognizer(recognizer)
                if (text.isNotEmpty()) submit(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Auto-stop after a stretch of trailing silence (recognizer hint; the
            // platform still applies its own end-of-speech detection as well).
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS)
        }
        recognizer.startListening(intent)

        // Safety net only — force the session closed if the recognizer never returns
        // so the mic can't get stuck open. NOT the normal stop path: speech ends on
        // trailing silence or when you tap the button again.
        micTimeout = Runnable {
            try { recognizer.cancel() } catch (_: Throwable) {}
            clearRecognizer(recognizer)
        }.also { mainHandler.postDelayed(it, MAX_SESSION_MS) }
    }

    /** Manually finish the current session (tap-to-stop); delivers what was heard so far. */
    private fun stopVoiceCapture() {
        try { speechRecognizer?.stopListening() } catch (_: Throwable) {}
    }

    private fun clearRecognizer(recognizer: SpeechRecognizer) {
        micTimeout?.let { mainHandler.removeCallbacks(it) }
        micTimeout = null
        recognizer.destroy()
        if (speechRecognizer === recognizer) speechRecognizer = null
        isListening = false
        updateMicState()
    }

    private fun updateMicState() {
        _binding?.micButton?.setImageResource(
            if (isListening) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        micTimeout?.let { mainHandler.removeCallbacks(it) }
        micTimeout = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        _binding = null
    }

    private companion object {
        const val SILENCE_MS = 8_000
        const val MAX_SESSION_MS = 60_000L
    }
}
