package com.baxter.schedulaizer.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.transfer.RemoteDexterLoader
import com.baxter.schedulaizer.transfer.RemoteDexterNative
import com.baxter.schedulaizer.util.AlarmSoundPlayer
import com.baxter.schedulaizer.util.AppConstants

class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE) }
    private lateinit var tvToneStatus: TextView

    // SAF picker for an audio file (e.g. a .wav). We persist read access so the
    // BroadcastReceiver can still read the Uri after a reboot.
    private val pickTone = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Throwable) { /* some providers don't grant persistable access; best effort */ }
        prefs.edit().putString(AppConstants.PREF_ALARM_TONE_URI, uri.toString()).apply()
        updateToneStatus()
        Toast.makeText(this, "Alarm tone set", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val tvStatus = findViewById<TextView>(R.id.tv_native_status)
        val btn = findViewById<Button>(R.id.btn_enable_native)
        val etEndpoint = findViewById<EditText>(R.id.et_budget_blaster_endpoint)
        val btnSaveEndpoint = findViewById<Button>(R.id.btn_save_endpoint)
        tvToneStatus = findViewById(R.id.tv_tone_status)
        val btnPickTone = findViewById<Button>(R.id.btn_pick_tone)
        val btnPreviewTone = findViewById<Button>(R.id.btn_preview_tone)
        val btnResetTone = findViewById<Button>(R.id.btn_reset_tone)

        fun updateUiFromPref() {
            val enabled = prefs.getBoolean("native_engine_enabled", false)
            tvStatus.text = if (enabled) "Native engine state: enabled" else "Native engine state: disabled"
        }

        updateUiFromPref()
        updateToneStatus()

        etEndpoint.setText(
            prefs.getString(
                AppConstants.PREF_BUDGET_BLASTER_ENDPOINT,
                AppConstants.DEFAULT_BUDGET_BLASTER_ENDPOINT
            )
        )

        btn.setOnClickListener {
            RemoteDexterLoader.loadNativeLibrary()
            val success = RemoteDexterNative.libraryLoaded
            prefs.edit().putBoolean("native_engine_enabled", success).apply()
            if (success) {
                Toast.makeText(this, "Native engine enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Native engine unavailable on this device", Toast.LENGTH_SHORT).show()
            }
            updateUiFromPref()
        }

        btnSaveEndpoint.setOnClickListener {
            val url = etEndpoint.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Endpoint cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "Endpoint must start with http:// or https://", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString(AppConstants.PREF_BUDGET_BLASTER_ENDPOINT, url).apply()
            Toast.makeText(this, "Budget Blaster endpoint saved", Toast.LENGTH_SHORT).show()
        }

        btnPickTone.setOnClickListener {
            try {
                pickTone.launch(arrayOf("audio/*"))
            } catch (_: Throwable) {
                Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show()
            }
        }

        btnPreviewTone.setOnClickListener {
            val tone = prefs.getString(AppConstants.PREF_ALARM_TONE_URI, null)
            AlarmSoundPlayer.play(applicationContext, tone) { /* one-shot preview */ }
        }

        btnResetTone.setOnClickListener {
            prefs.edit().remove(AppConstants.PREF_ALARM_TONE_URI).apply()
            updateToneStatus()
            Toast.makeText(this, "Using default alarm sound", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Don't let a preview keep playing after leaving Settings.
        AlarmSoundPlayer.stop()
    }

    private fun updateToneStatus() {
        val uriStr = prefs.getString(AppConstants.PREF_ALARM_TONE_URI, null)
        tvToneStatus.text = if (uriStr.isNullOrBlank()) {
            "Tone: System default alarm"
        } else {
            "Tone: ${displayNameFor(Uri.parse(uriStr))}"
        }
    }

    private fun displayNameFor(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment ?: "Custom"
        } catch (_: Throwable) {
            uri.lastPathSegment ?: "Custom"
        }
    }
}
