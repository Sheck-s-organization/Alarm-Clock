package com.smartalarm.app.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.databinding.ItemAlarmBinding

class AlarmsAdapter(
    private val onDelete: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmsAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = getItem(position)
        with(holder.binding) {
            tvAlarmTime.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
            tvAlarmLabel.text = alarm.label
            tvAlarmType.text = "STD"
            tvRepeatDays.text = ""
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = alarm.enabled
        }
        holder.itemView.setOnLongClickListener {
            onDelete(alarm)
            true
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Alarm>() {
            override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
        }
    }
}
