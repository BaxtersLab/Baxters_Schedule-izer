package com.baxter.schedulaizer.ui.alarms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.alerts.AlertScheduler
import com.baxter.schedulaizer.data.db.entity.AlertEntity
import com.baxter.schedulaizer.util.AlarmTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmsActivity : AppCompatActivity() {

    private lateinit var adapter: AlarmsAdapter
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarms)
        title = "Alarms"

        empty = findViewById(R.id.tv_empty)
        val rv = findViewById<RecyclerView>(R.id.rv_alarms)
        adapter = AlarmsAdapter(
            onToggle = { alarm, enabled -> setEnabled(alarm, enabled) },
            onClick = { alarm -> editAlarm(alarm.id) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_add_alarm).setOnClickListener { editAlarm(0L) }

        val dao = SchedulaizerApp.get(this).database.alertDao()
        lifecycleScope.launch {
            dao.getAlarms().collectLatest { list ->
                adapter.submit(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun editAlarm(id: Long) {
        startActivity(
            Intent(this, AlarmEditActivity::class.java)
                .putExtra(AlarmEditActivity.EXTRA_ALARM_ID, id)
        )
    }

    private fun setEnabled(alarm: AlertEntity, enabled: Boolean) {
        val dao = SchedulaizerApp.get(this).database.alertDao()
        val scheduler = AlertScheduler(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (enabled) {
                    // Re-enabling a past-due time advances it to the next occurrence.
                    val fireAt = if (alarm.fireAtMs <= System.currentTimeMillis())
                        AlarmTimeUtils.nextDailyMs(alarm.fireAtMs) else alarm.fireAtMs
                    val updated = alarm.copy(isActive = true, fireAtMs = fireAt)
                    dao.updateAlert(updated)
                    scheduler.scheduleAlert(updated)
                } else {
                    dao.setActive(alarm.id, false)
                    scheduler.cancelAlert(alarm.id)
                }
            }
        }
    }

    private fun deleteAlarm(alarm: AlertEntity) {
        val dao = SchedulaizerApp.get(this).database.alertDao()
        val scheduler = AlertScheduler(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                scheduler.cancelAlert(alarm.id)
                dao.deleteById(alarm.id)
            }
        }
    }
}
