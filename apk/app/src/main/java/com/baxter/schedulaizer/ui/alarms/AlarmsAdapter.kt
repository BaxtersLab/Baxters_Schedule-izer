package com.baxter.schedulaizer.ui.alarms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.baxter.schedulaizer.R
import com.baxter.schedulaizer.data.db.entity.AlertEntity
import com.baxter.schedulaizer.util.DateUtils

class AlarmsAdapter(
    private val onToggle: (AlertEntity, Boolean) -> Unit,
    private val onClick: (AlertEntity) -> Unit,
    private val onDelete: (AlertEntity) -> Unit
) : RecyclerView.Adapter<AlarmsAdapter.VH>() {

    private val items = mutableListOf<AlertEntity>()

    fun submit(list: List<AlertEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val time: TextView = view.findViewById(R.id.tv_alarm_time)
        private val label: TextView = view.findViewById(R.id.tv_alarm_label)
        private val sub: TextView = view.findViewById(R.id.tv_alarm_sub)
        private val sw: SwitchCompat = view.findViewById(R.id.sw_alarm_enabled)
        private val del: ImageButton = view.findViewById(R.id.btn_alarm_delete)

        fun bind(a: AlertEntity) {
            time.text = DateUtils.formatTime(a.fireAtMs)
            label.text = a.title.ifBlank { "Alarm" }
            val bits = mutableListOf<String>()
            bits.add(if (a.repeatDaily) "Daily" else DateUtils.formatShortDate(a.fireAtMs))
            if (!a.soundUri.isNullOrBlank()) bits.add("Custom tone")
            sub.text = bits.joinToString(" · ")

            // Detach the listener before setting state so restoring the checked value
            // doesn't fire a spurious toggle callback while recycling.
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = a.isActive
            sw.setOnCheckedChangeListener { _, checked -> onToggle(a, checked) }

            itemView.setOnClickListener { onClick(a) }
            del.setOnClickListener { onDelete(a) }
        }
    }
}
