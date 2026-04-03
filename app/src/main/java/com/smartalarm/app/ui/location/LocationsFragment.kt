package com.smartalarm.app.ui.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentLocationsBinding
import com.smartalarm.app.viewmodel.SavedLocationViewModel
import com.smartalarm.app.viewmodel.SavedLocationViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationsFragment : Fragment(R.layout.fragment_locations) {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedLocationViewModel by viewModels {
        val app = requireActivity().application as AlarmApplication
        SavedLocationViewModelFactory(app, app.savedLocationRepository)
    }

    private val adapter = LocationsAdapter(
        onDelete = { location ->
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_location_title)
                .setMessage(getString(R.string.delete_location_message, location.name))
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteLocation(location) }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchAndSaveCurrentLocation()
        else showPermissionDenied()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLocationsBinding.bind(view)

        binding.recyclerLocations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLocations.adapter = adapter

        binding.fabAddLocation.setOnClickListener { showAddLocationDialog() }
        binding.btnUseCurrentLocation.setOnClickListener { requestLocationAndSave() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allLocations.collect { locations ->
                    adapter.submitList(locations)
                    binding.emptyState.visibility =
                        if (locations.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        refreshCurrentLocationDisplay()
    }

    private fun refreshCurrentLocationDisplay() {
        if (!hasLocationPermission()) {
            binding.tvCurrentLocation.setText(R.string.location_unknown)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loc = LocationServices.getFusedLocationProviderClient(requireContext())
                    .lastLocation.await()
                binding.tvCurrentLocation.text = if (loc != null) {
                    getString(R.string.current_location_format, loc.latitude, loc.longitude)
                } else {
                    getString(R.string.location_unknown)
                }
            } catch (_: SecurityException) {
                binding.tvCurrentLocation.setText(R.string.location_unknown)
            }
        }
    }

    private fun showAddLocationDialog() {
        val nameInput = EditText(requireContext()).apply {
            hint = getString(R.string.location_name_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.name_this_location)
            .setView(nameInput)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = nameInput.text.toString().trim().ifEmpty { "Location" }
                saveCurrentLocationWithName(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun requestLocationAndSave() {
        if (hasLocationPermission()) {
            showAddLocationDialog()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchAndSaveCurrentLocation() {
        val nameInput = EditText(requireContext()).apply {
            hint = getString(R.string.location_name_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.name_this_location)
            .setView(nameInput)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = nameInput.text.toString().trim().ifEmpty { "Location" }
                saveCurrentLocationWithName(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun saveCurrentLocationWithName(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loc = LocationServices.getFusedLocationProviderClient(requireContext())
                    .lastLocation.await()
                if (loc != null) {
                    viewModel.addLocation(name, loc.latitude, loc.longitude)
                    binding.tvCurrentLocation.text =
                        getString(R.string.current_location_format, loc.latitude, loc.longitude)
                } else {
                    binding.tvCurrentLocation.setText(R.string.location_unknown)
                }
            } catch (_: SecurityException) {
                showPermissionDenied()
            }
        }
    }

    private fun showPermissionDenied() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.location_permission_denied)
            .setPositiveButton(R.string.action_cancel, null)
            .show()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
