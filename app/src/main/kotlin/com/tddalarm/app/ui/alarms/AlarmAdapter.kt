package com.tddalarm.app.ui.alarms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tddalarm.app.R
import com.tddalarm.app.databinding.ItemAlarmBinding
import com.tddalarm.app.ui.AlarmListItem
import com.tddalarm.app.ui.formatClockTime
import com.tddalarm.app.ui.toNextFiringText
import com.tddalarm.app.ui.toShortNames

class AlarmAdapter(
    private val onToggle: (AlarmListItem, Boolean) -> Unit,
    private val onClick: (AlarmListItem) -> Unit,
) : ListAdapter<AlarmListItem, AlarmAdapter.Holder>(Diff) {

    object Diff : DiffUtil.ItemCallback<AlarmListItem>() {
        override fun areItemsTheSame(a: AlarmListItem, b: AlarmListItem) = a.alarm.id == b.alarm.id
        override fun areContentsTheSame(a: AlarmListItem, b: AlarmListItem) = a == b
    }

    inner class Holder(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val alarm = item.alarm
        val context = holder.binding.root.context

        with(holder.binding) {
            timeText.text = formatClockTime(alarm.hour, alarm.minute)
            labelText.text = alarm.label
            labelText.isVisible = alarm.label.isNotEmpty()

            val ruleParts = buildList {
                if (alarm.repeatDays.isNotEmpty()) add(alarm.repeatDays.toShortNames())
                if (alarm.skipOnDaysOff) add(context.getString(R.string.skip_on_days_off_short))
                if (alarm.locationRule != null) {
                    add(
                        item.placeName
                            ?.let { context.getString(R.string.near_place, it) }
                            ?: context.getString(R.string.only_at_location_short)
                    )
                }
            }
            rulesText.text = ruleParts.joinToString(" · ")
            rulesText.isVisible = ruleParts.isNotEmpty()

            nextText.text = item.nextFiring
                ?.let { context.getString(R.string.next_firing_prefix, it.toNextFiringText()) }
                ?: context.getString(R.string.next_firing_none)
            nextText.isVisible = alarm.enabled

            enabledSwitch.setOnCheckedChangeListener(null)
            enabledSwitch.isChecked = alarm.enabled
            enabledSwitch.setOnCheckedChangeListener { _, checked ->
                if (checked != alarm.enabled) onToggle(item, checked)
            }

            root.setOnClickListener { onClick(item) }
        }
    }
}
