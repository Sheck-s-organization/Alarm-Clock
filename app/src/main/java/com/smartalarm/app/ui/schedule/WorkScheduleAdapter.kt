package com.smartalarm.app.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.ScheduleType
import com.smartalarm.app.data.entities.WorkSchedule
import com.smartalarm.app.databinding.ItemWorkScheduleBinding
import java.util.Locale

class WorkScheduleAdapter(
    private val onSetActive: (WorkSchedule) -> Unit,
    private val onEdit: (WorkSchedule) -> Unit,
    private val onDelete: (WorkSchedule) -> Unit
) : ListAdapter<WorkSchedule, WorkScheduleAdapter.ViewHolder>(DiffCallback()) {

    private var activeScheduleId: Long? = null

    fun setActiveScheduleId(id: Long?) {
        activeScheduleId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == activeScheduleId)
    }

    inner class ViewHolder(private val binding: ItemWorkScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: WorkSchedule, isActive: Boolean) {
            binding.apply {
                tvScheduleName.text = schedule.name
                tvScheduleType.text = when (schedule.scheduleType) {
                    ScheduleType.FIXED_DAYS -> "Fixed"
                    ScheduleType.ROTATING -> "Rotating"
                }
                tvAlarmTime.text = String.format(
                    Locale.getDefault(), "%02d:%02d",
                    schedule.alarmHour, schedule.alarmMinute
                )
                tvWorkDays.text = formatWorkDays(schedule)
                radioActive.isChecked = isActive

                radioActive.setOnClickListener { onSetActive(schedule) }
                root.setOnClickListener { onEdit(schedule) }
                root.setOnLongClickListener { onDelete(schedule); true }
            }
        }

        private fun formatWorkDays(schedule: WorkSchedule): String {
            return when (schedule.scheduleType) {
                ScheduleType.FIXED_DAYS -> {
                    val days = schedule.workDays
                    when {
                        days == setOf(1, 2, 3, 4, 5) -> "Mon–Fri"
                        days == setOf(6, 7) -> "Sat–Sun"
                        days.size == 7 -> "Every day"
                        else -> {
                            val names = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            days.sorted().joinToString(", ") { names.getOrElse(it) { "" } }
                        }
                    }
                }
                ScheduleType.ROTATING -> "Rotating shift"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<WorkSchedule>() {
        override fun areItemsTheSame(a: WorkSchedule, b: WorkSchedule) = a.id == b.id
        override fun areContentsTheSame(a: WorkSchedule, b: WorkSchedule) = a == b
    }
}
