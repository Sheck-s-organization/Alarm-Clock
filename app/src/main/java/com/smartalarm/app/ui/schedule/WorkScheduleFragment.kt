package com.smartalarm.app.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentWorkScheduleBinding
import com.smartalarm.app.viewmodel.OperationResult
import com.smartalarm.app.viewmodel.WorkScheduleViewModel

class WorkScheduleFragment : Fragment() {

    private var _binding: FragmentWorkScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkScheduleViewModel by viewModels()
    private lateinit var adapter: WorkScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WorkScheduleAdapter(
            onSetActive = { schedule -> viewModel.setActiveSchedule(schedule.id) },
            onEdit = { schedule ->
                val action = WorkScheduleFragmentDirections
                    .actionScheduleToEditSchedule(schedule.id)
                findNavController().navigate(action)
            },
            onDelete = { schedule ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_schedule_title)
                    .setMessage(R.string.delete_schedule_message)
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.deleteSchedule(schedule)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        )

        binding.recyclerSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WorkScheduleFragment.adapter
        }

        viewModel.allSchedules.observe(viewLifecycleOwner) { schedules ->
            adapter.submitList(schedules)
            binding.emptyState.visibility = if (schedules.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.activeSchedule.observe(viewLifecycleOwner) { active ->
            adapter.setActiveScheduleId(active?.id)
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is OperationResult.Success ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                is OperationResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.fabAddSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_schedule_to_editSchedule)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
