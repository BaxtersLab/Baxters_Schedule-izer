package com.baxter.schedulaizer.ui.alarms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.alerts.AlertScheduler
import com.baxter.schedulaizer.data.db.entity.AlertEntity
import com.baxter.schedulaizer.util.AlarmTimeUtils
import com.baxter.schedulaizer.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmEditActivity : AppCompatActivity() {

    private var alarmId: Long = 0L
    private var existing: AlertEntity? = null
    private var perAlarmTone: String? = null

    private lateinit var timePicker: TimePicker
    private lateinit var etLabel: EditText
    private lateinit var cbRepeat: CheckBox
    private lateinit var tvTone: TextView

    private val pickTone = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Throwable) { /* best effort */ }
        perAlarmTone = uri.toString()
        updateToneLabel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        timePicker = findViewById(R.id.time_picker)
        etLabel = findViewById(R.id.et_label)
        cbRepeat = findViewById(R.id.cb_repeat)
        tvTone = findViewById(R.id.tv_alarm_tone)
        timePicker.setIs24HourView(DateFormat.is24HourFormat(this))

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val btnDelete = findViewById<Button>(R.id.btn_delete)

        if (alarmId != 0L) {
            title = "Edit alarm"
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val a = withContext(Dispatchers.IO) {
                    SchedulaizerApp.get(this@AlarmEditActivity).database.alertDao().getAlertById(alarmId)
                }
                if (a != null) {
                    existing = a
                    timePicker.hour = AlarmTimeUtils.hourOf(a.fireAtMs)
                    timePicker.minute = AlarmTimeUtils.minuteOf(a.fireAtMs)
                    etLabel.setText(a.title)
                    cbRepeat.isChecked = a.repeatDaily
                    perAlarmTone = a.soundUri
                    updateToneLabel()
                }
            }
        } else {
            title = "New alarm"
            btnDelete.visibility = View.GONE
        }
        updateToneLabel()

        findViewById<Button>(R.id.btn_pick_tone).setOnClickListener {
            try {
                pickTone.launch(arrayOf("audio/*"))
            } catch (_: Throwable) {
                Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btn_clear_tone).setOnClickListener {
            perAlarmTone = null
            updateToneLabel()
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }
        btnDelete.setOnClickListener { delete() }
    }

    private fun save() {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val repeat = cbRepeat.isChecked
        val label = etLabel.text.toString().trim().ifBlank { "Alarm" }
        val fireAt = AlarmTimeUtils.nextOccurrenceMs(hour, minute)

        val base = existing
        val entity = base?.copy(
            title = label,
            fireAtMs = fireAt,
            isActive = true,
            isSnoozed = false,
            snoozeUntilMs = -1L,
            soundUri = perAlarmTone,
            repeatDaily = repeat
        ) ?: AlertEntity(
            parentId = 0L,
            parentType = "alarm",
            title = label,
            body = "",
            fireAtMs = fireAt,
            isActive = true,
            soundUri = perAlarmTone,
            repeatDaily = repeat,
            createdMs = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = SchedulaizerApp.get(this@AlarmEditActivity).database.alertDao()
                // REPLACE strategy: a 0 id inserts, an existing id overwrites in place.
                val rowId = dao.insertAlert(entity)
                val saved = entity.copy(id = if (entity.id == 0L) rowId else entity.id)
                AlertScheduler(this@AlarmEditActivity).scheduleAlert(saved)
            }
            Toast.makeText(
                this@AlarmEditActivity,
                "Alarm set for ${DateUtils.formatTime(fireAt)}",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun delete() {
        val id = alarmId
        if (id == 0L) { finish(); return }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = SchedulaizerApp.get(this@AlarmEditActivity).database.alertDao()
                AlertScheduler(this@AlarmEditActivity).cancelAlert(id)
                dao.deleteById(id)
            }
            finish()
        }
    }

    private fun updateToneLabel() {
        val t = perAlarmTone
        tvTone.text = if (t.isNullOrBlank()) {
            "Tone: Use global / default"
        } else {
            "Tone: ${displayNameFor(Uri.parse(t))}"
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

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
