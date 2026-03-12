package com.smartalarm.app.ui.holiday

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.databinding.ItemHolidayBinding

class HolidayAdapter(
    private val onDelete: (Holiday) -> Unit
) : ListAdapter<Holiday, HolidayAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHolidayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHolidayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(holiday: Holiday) {
            binding.apply {
                tvHolidayName.text = if (holiday.emoji.isNotBlank()) {
                    "${holiday.emoji} ${holiday.name}"
                } else {
                    holiday.name
                }

                tvHolidayDate.text = when (holiday.holidayType) {
                    HolidayType.ONE_TIME -> {
                        val year = if (holiday.year != null) "/${holiday.year}" else ""
                        "${holiday.month}/${holiday.day}$year"
                    }
                    HolidayType.ANNUAL -> "Every ${monthName(holiday.month)} ${holiday.day}"
                    HolidayType.RANGE -> {
                        "${holiday.month}/${holiday.day} – ${holiday.endMonth}/${holiday.endDay}"
                    }
                }

                tvHolidayType.text = when (holiday.holidayType) {
                    HolidayType.ONE_TIME -> "One-time"
                    HolidayType.ANNUAL -> "Annual"
                    HolidayType.RANGE -> "Vacation"
                }

                btnDeleteHoliday.setOnClickListener { onDelete(holiday) }
            }
        }

        private fun monthName(month: Int) = listOf(
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        ).getOrElse(month) { "" }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Holiday>() {
        override fun areItemsTheSame(a: Holiday, b: Holiday) = a.id == b.id
        override fun areContentsTheSame(a: Holiday, b: Holiday) = a == b
    }
}
