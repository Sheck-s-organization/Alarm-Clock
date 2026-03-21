package com.smartalarm.app.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.WorkSchedule
import com.smartalarm.app.databinding.ItemWorkScheduleBinding
import java.util.Calendar

class WorkSchedulesAdapter(
    private val onEdit: (WorkSchedule) -> Unit,
    private val onDelete: (WorkSchedule) -> Unit
) : ListAdapter<WorkSchedule, WorkSchedulesAdapter.ViewHolder>(DIFF) {

    var selectedId: Long = -1L
        set(value) {
            val old = field
            field = value
            if (old != value) notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: ItemWorkScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: WorkSchedule) {
            binding.tvScheduleName.text = schedule.name
            binding.tvWorkDays.text = formatDays(schedule.workDays)
            binding.tvScheduleType.text = "Fixed"
            binding.radioActive.isChecked = schedule.id == selectedId
            binding.radioActive.setOnClickListener { selectedId = schedule.id }
            binding.root.setOnClickListener { onEdit(schedule) }
            binding.root.setOnLongClickListener { onDelete(schedule); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkSchedule>() {
            override fun areItemsTheSame(a: WorkSchedule, b: WorkSchedule) = a.id == b.id
            override fun areContentsTheSame(a: WorkSchedule, b: WorkSchedule) = a == b
        }

        private val DAY_LABELS = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )

        private val DAY_ORDER = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        fun formatDays(days: Set<Int>): String {
            if (days.isEmpty()) return "No work days"
            val sorted = DAY_ORDER.filter { it in days }
            return sorted.mapNotNull { DAY_LABELS[it] }.joinToString(", ")
        }
    }
}
