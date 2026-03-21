package com.smartalarm.app.ui.holiday

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.databinding.ItemHolidayBinding

class HolidaysAdapter(
    private val onDelete: (Holiday) -> Unit
) : ListAdapter<Holiday, HolidaysAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemHolidayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(holiday: Holiday) {
            binding.tvHolidayName.text = holiday.name
            binding.tvHolidayDate.text = formatDate(holiday)
            binding.tvHolidayType.text = if (holiday.type == HolidayType.ANNUAL) "Annual" else "One-time"
            binding.btnDeleteHoliday.setOnClickListener { onDelete(holiday) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHolidayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Holiday>() {
            override fun areItemsTheSame(a: Holiday, b: Holiday) = a.id == b.id
            override fun areContentsTheSame(a: Holiday, b: Holiday) = a == b
        }

        private val MONTH_NAMES = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        fun formatDate(holiday: Holiday): String {
            val monthName = MONTH_NAMES.getOrElse(holiday.month - 1) { "?" }
            return if (holiday.type == HolidayType.ONE_TIME && holiday.year != null) {
                "$monthName ${holiday.day}, ${holiday.year}"
            } else {
                "$monthName ${holiday.day}"
            }
        }
    }
}
