package com.smartalarm.app.ui.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentLocationsBinding
import com.smartalarm.app.viewmodel.LocationViewModel
import com.smartalarm.app.viewmodel.OperationResult

class LocationsFragment : Fragment() {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()
    private lateinit var adapter: LocationAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            viewModel.refreshCurrentLocation()
        } else {
            Snackbar.make(binding.root, R.string.location_permission_denied, Snackbar.LENGTH_LONG)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LocationAdapter(
            onDelete = { location ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_location_title)
                    .setMessage(getString(R.string.delete_location_message, location.name))
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.deleteLocation(location)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        )

        binding.recyclerLocations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LocationsFragment.adapter
        }

        viewModel.allLocations.observe(viewLifecycleOwner) { locations ->
            adapter.submitList(locations)
            binding.emptyState.visibility = if (locations.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is OperationResult.Success ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                is OperationResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) { loc ->
            if (loc != null) {
                binding.tvCurrentLocation.text = getString(
                    R.string.current_location_format,
                    loc.latitude,
                    loc.longitude
                )
            }
        }

        binding.fabAddLocation.setOnClickListener {
            showAddLocationDialog()
        }

        binding.btnUseCurrentLocation.setOnClickListener {
            requestLocationAndSave()
        }

        // Request location to display current position
        checkAndRefreshLocation()
    }

    private fun checkAndRefreshLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine) viewModel.refreshCurrentLocation()
    }

    private fun requestLocationAndSave() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        // Show name dialog, then save current location
        showNameInputDialog { name ->
            viewModel.saveCurrentAsLocation(name)
        }
    }

    private fun showAddLocationDialog() {
        showNameInputDialog { name ->
            viewModel.refreshCurrentLocation()
            viewModel.currentLocation.value?.let { loc ->
                viewModel.saveManualLocation(name, loc.latitude, loc.longitude)
            } ?: run {
                requestLocationAndSave()
            }
        }
    }

    private fun showNameInputDialog(onConfirm: (String) -> Unit) {
        val editText = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.location_name_hint)
            setPadding(48, 24, 48, 8)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.name_this_location)
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = editText.text.toString().trim().ifBlank { "Location" }
                onConfirm(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
