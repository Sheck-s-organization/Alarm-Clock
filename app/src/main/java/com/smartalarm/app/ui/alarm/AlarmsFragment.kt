package com.smartalarm.app.ui.alarm

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.databinding.FragmentAlarmsBinding
import com.smartalarm.app.viewmodel.AlarmViewModel
import com.smartalarm.app.viewmodel.AlarmViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmsFragment : Fragment(R.layout.fragment_alarms) {

    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlarmViewModel by viewModels {
        val app = requireActivity().application as AlarmApplication
        AlarmViewModelFactory(app.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlarmsBinding.bind(view)

        val adapter = AlarmsAdapter { alarm -> showDeleteConfirmDialog(alarm) }
        binding.recyclerAlarms.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allAlarms.collect { alarms ->
                adapter.submitList(alarms)
                binding.emptyState.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAddAlarm.setOnClickListener { showTimePickerDialog() }
    }

    private fun showDeleteConfirmDialog(alarm: Alarm) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_alarm_title)
            .setMessage(R.string.delete_alarm_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteAlarm(alarm) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showTimePickerDialog() {
        val now = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute -> viewModel.addAlarm(hour, minute) },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
