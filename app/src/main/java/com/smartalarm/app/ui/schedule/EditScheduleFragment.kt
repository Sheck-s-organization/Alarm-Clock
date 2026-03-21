package com.smartalarm.app.ui.schedule

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentEditScheduleBinding
import com.smartalarm.app.viewmodel.WorkScheduleViewModel
import com.smartalarm.app.viewmodel.WorkScheduleViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class EditScheduleFragment : Fragment(R.layout.fragment_edit_schedule) {

    private var _binding: FragmentEditScheduleBinding? = null
    private val binding get() = _binding!!

    private val args: EditScheduleFragmentArgs by navArgs()

    private val viewModel: WorkScheduleViewModel by viewModels {
        val app = requireActivity().application as AlarmApplication
        WorkScheduleViewModelFactory(app, app.workScheduleRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEditScheduleBinding.bind(view)

        val scheduleId = args.scheduleId
        if (scheduleId != -1L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val schedule = (requireActivity().application as AlarmApplication)
                    .workScheduleRepository.getById(scheduleId) ?: return@launch
                binding.etScheduleName.setText(schedule.name)
                binding.toggleMon.isChecked = Calendar.MONDAY in schedule.workDays
                binding.toggleTue.isChecked = Calendar.TUESDAY in schedule.workDays
                binding.toggleWed.isChecked = Calendar.WEDNESDAY in schedule.workDays
                binding.toggleThu.isChecked = Calendar.THURSDAY in schedule.workDays
                binding.toggleFri.isChecked = Calendar.FRIDAY in schedule.workDays
                binding.toggleSat.isChecked = Calendar.SATURDAY in schedule.workDays
                binding.toggleSun.isChecked = Calendar.SUNDAY in schedule.workDays
            }
        }

        binding.btnSaveSchedule.setOnClickListener { save(scheduleId) }
    }

    private fun save(scheduleId: Long) {
        val name = binding.etScheduleName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.schedule_name_hint, Toast.LENGTH_SHORT).show()
            return
        }
        val workDays = buildSet {
            if (binding.toggleMon.isChecked) add(Calendar.MONDAY)
            if (binding.toggleTue.isChecked) add(Calendar.TUESDAY)
            if (binding.toggleWed.isChecked) add(Calendar.WEDNESDAY)
            if (binding.toggleThu.isChecked) add(Calendar.THURSDAY)
            if (binding.toggleFri.isChecked) add(Calendar.FRIDAY)
            if (binding.toggleSat.isChecked) add(Calendar.SATURDAY)
            if (binding.toggleSun.isChecked) add(Calendar.SUNDAY)
        }
        val schedule = com.smartalarm.app.data.entities.WorkSchedule(
            id = if (scheduleId == -1L) 0L else scheduleId,
            name = name,
            workDays = workDays
        )
        viewModel.updateSchedule(schedule)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
