package com.tddalarm.app.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tddalarm.app.databinding.ItemDeletableRowBinding

data class DeletableRow(val id: Long, val text: String)

class DeletableRowAdapter(
    private val onDelete: (Long) -> Unit,
) : ListAdapter<DeletableRow, DeletableRowAdapter.Holder>(Diff) {

    object Diff : DiffUtil.ItemCallback<DeletableRow>() {
        override fun areItemsTheSame(a: DeletableRow, b: DeletableRow) = a.id == b.id
        override fun areContentsTheSame(a: DeletableRow, b: DeletableRow) = a == b
    }

    inner class Holder(val binding: ItemDeletableRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemDeletableRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        holder.binding.rowText.text = row.text
        holder.binding.deleteButton.setOnClickListener { onDelete(row.id) }
    }
}
