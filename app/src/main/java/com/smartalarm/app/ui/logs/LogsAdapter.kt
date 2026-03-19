package com.smartalarm.app.ui.logs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.databinding.ItemLogBinding
import com.smartalarm.app.util.LogBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsAdapter : ListAdapter<LogBuffer.Entry, LogsAdapter.ViewHolder>(DIFF) {

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    class ViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        with(holder.binding) {
            tvLogTime.text = timeFmt.format(Date(entry.timestampMillis))
            tvLogTag.text = entry.tag
            tvLogMessage.text = entry.message
            tvLogMessage.setTextColor(
                if (entry.level == LogBuffer.Level.ERROR) Color.parseColor("#D32F2F")
                else tvLogTime.currentTextColor
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LogBuffer.Entry>() {
            override fun areItemsTheSame(a: LogBuffer.Entry, b: LogBuffer.Entry) =
                a.timestampMillis == b.timestampMillis && a.tag == b.tag && a.message == b.message
            override fun areContentsTheSame(a: LogBuffer.Entry, b: LogBuffer.Entry) = a == b
        }
    }
}
