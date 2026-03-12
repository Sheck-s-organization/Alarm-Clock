package com.smartalarm.app.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.databinding.ItemAlarmBinding
import java.util.Locale

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onEdit: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(
        private val binding: ItemAlarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            binding.apply {
                // Time
                tvAlarmTime.text = String.format(
                    Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute
                )

                // Label
                tvAlarmLabel.text = alarm.label.ifBlank { "Alarm" }

                // Type badge
                tvAlarmType.text = when (alarm.alarmType) {
                    AlarmType.STANDARD -> ""
                    AlarmType.WORK_SCHEDULE -> "WORK"
                    AlarmType.TIME_OF_MONTH -> "MONTHLY"
                    AlarmType.LOCATION -> "LOCATION"
                    AlarmType.CHARGING -> "CHARGING"
                }

                // Repeat days summary
                tvRepeatDays.text = formatRepeatDays(alarm.repeatDays)

                // Enable toggle
                switchEnabled.isChecked = alarm.enabled
                switchEnabled.setOnCheckedChangeListener { _, checked ->
                    onToggle(alarm, checked)
                }

                // Click to edit
                root.setOnClickListener { onEdit(alarm) }

                // Long press to delete
                root.setOnLongClickListener {
                    onDelete(alarm)
                    true
                }
            }
        }

        private fun formatRepeatDays(days: Set<Int>): String {
            if (days.isEmpty()) return "Once"
            val dayNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            return when {
                days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
                days == setOf(6, 7) -> "Weekends"
                days.size == 7 -> "Every day"
                else -> days.sorted().joinToString(", ") { dayNames.getOrElse(it) { "" } }
            }
        }
    }

    private class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
    }
}
