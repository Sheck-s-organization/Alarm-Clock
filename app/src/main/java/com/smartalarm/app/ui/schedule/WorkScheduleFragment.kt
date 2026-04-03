package com.smartalarm.app.ui.schedule

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentWorkScheduleBinding
import com.smartalarm.app.viewmodel.WorkScheduleViewModel
import com.smartalarm.app.viewmodel.WorkScheduleViewModelFactory
import kotlinx.coroutines.launch

class WorkScheduleFragment : Fragment(R.layout.fragment_work_schedule) {

    private var _binding: FragmentWorkScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkScheduleViewModel by viewModels {
        val app = requireActivity().application as AlarmApplication
        WorkScheduleViewModelFactory(app, app.workScheduleRepository)
    }

    private val adapter = WorkSchedulesAdapter(
        onEdit = { schedule ->
            val action = WorkScheduleFragmentDirections
                .actionScheduleToEditSchedule(schedule.id)
            findNavController().navigate(action)
        },
        onDelete = { schedule ->
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_schedule_title)
                .setMessage(R.string.delete_schedule_message)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteSchedule(schedule) }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWorkScheduleBinding.bind(view)

        binding.recyclerSchedules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSchedules.adapter = adapter

        binding.fabAddSchedule.setOnClickListener {
            val action = WorkScheduleFragmentDirections.actionScheduleToEditSchedule(-1L)
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allSchedules.collect { schedules ->
                    adapter.submitList(schedules)
                    binding.emptyState.visibility =
                        if (schedules.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
