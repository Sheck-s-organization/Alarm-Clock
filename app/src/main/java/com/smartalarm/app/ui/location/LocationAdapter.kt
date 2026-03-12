package com.smartalarm.app.ui.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.SavedLocation
import com.smartalarm.app.databinding.ItemLocationBinding
import java.util.Locale

class LocationAdapter(
    private val onDelete: (SavedLocation) -> Unit
) : ListAdapter<SavedLocation, LocationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: SavedLocation) {
            binding.apply {
                tvLocationName.text = location.name
                tvCoordinates.text = String.format(
                    Locale.getDefault(), "%.5f, %.5f", location.latitude, location.longitude
                )
                tvRadius.text = String.format(
                    Locale.getDefault(), "%.0fm radius", location.radiusMeters
                )
                tvAddress.text = location.address ?: ""
                btnDeleteLocation.setOnClickListener { onDelete(location) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SavedLocation>() {
        override fun areItemsTheSame(a: SavedLocation, b: SavedLocation) = a.id == b.id
        override fun areContentsTheSame(a: SavedLocation, b: SavedLocation) = a == b
    }
}
