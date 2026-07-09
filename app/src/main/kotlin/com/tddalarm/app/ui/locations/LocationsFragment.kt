package com.tddalarm.app.ui.locations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.R
import com.tddalarm.app.databinding.DialogAddPlaceBinding
import com.tddalarm.app.databinding.FragmentLocationsBinding
import com.tddalarm.app.ui.LocationsViewModel
import com.tddalarm.app.ui.ViewModelFactory
import com.tddalarm.app.ui.schedule.DeletableRow
import com.tddalarm.app.ui.schedule.DeletableRowAdapter
import java.util.Locale
import kotlinx.coroutines.launch

class LocationsFragment : Fragment() {

    private val viewModel: LocationsViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentLocationsBinding.inflate(inflater, container, false)

        val adapter = DeletableRowAdapter { viewModel.remove(it) }
        b.placeList.layoutManager = LinearLayoutManager(requireContext())
        b.placeList.adapter = adapter

        b.addPlaceButton.setOnClickListener { showAddPlaceDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.items.collect { places ->
                        adapter.submitList(
                            places.map { place ->
                                DeletableRow(
                                    id = place.id,
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%s — %.4f, %.4f · %.0f km",
                                        place.name,
                                        place.fence.center.latitude,
                                        place.fence.center.longitude,
                                        place.fence.radiusMeters / 1000.0,
                                    ),
                                )
                            }
                        )
                        b.emptyText.isVisible = places.isEmpty()
                    }
                }
                launch {
                    viewModel.message.collect { resId ->
                        if (resId != null) {
                            Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
        return b.root
    }

    private fun showAddPlaceDialog() {
        val dialogBinding = DialogAddPlaceBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_place)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.addPlace(
                    name = dialogBinding.placeNameInput.text.toString(),
                    address = dialogBinding.addressInput.text.toString(),
                    radiusKm = dialogBinding.radiusInput.text.toString().toDoubleOrNull(),
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
