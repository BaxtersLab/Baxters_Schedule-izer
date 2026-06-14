package com.baxter.schedulaizer.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.transfer.RemoteDexterLoader
import com.baxter.schedulaizer.transfer.RemoteDexterNative
import com.baxter.schedulaizer.util.AppConstants

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val tvStatus = findViewById<TextView>(R.id.tv_native_status)
        val btn = findViewById<Button>(R.id.btn_enable_native)
        val etEndpoint = findViewById<EditText>(R.id.et_budget_blaster_endpoint)
        val btnSaveEndpoint = findViewById<Button>(R.id.btn_save_endpoint)

        fun updateUiFromPref() {
            val enabled = prefs.getBoolean("native_engine_enabled", false)
            tvStatus.text = if (enabled) "Native engine state: enabled" else "Native engine state: disabled"
        }

        updateUiFromPref()

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
    }
}
