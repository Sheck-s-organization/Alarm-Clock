package com.smartalarm.app.ui.location

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartalarm.app.data.entities.SavedLocation
import com.smartalarm.app.databinding.ItemLocationBinding

class LocationsAdapter(
    private val onDelete: (SavedLocation) -> Unit
) : ListAdapter<SavedLocation, LocationsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: SavedLocation) {
            binding.tvLocationName.text = location.name
            binding.tvCoordinates.text = "%.5f, %.5f".format(location.latitude, location.longitude)
            binding.tvRadius.text = "${location.radiusMeters.toInt()}m radius"
            binding.tvAddress.text = ""
            binding.btnDeleteLocation.setOnClickListener { onDelete(location) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SavedLocation>() {
            override fun areItemsTheSame(a: SavedLocation, b: SavedLocation) = a.id == b.id
            override fun areContentsTheSame(a: SavedLocation, b: SavedLocation) = a == b
        }
    }
}
