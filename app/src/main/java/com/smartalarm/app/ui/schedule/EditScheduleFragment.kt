package com.smartalarm.app.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartalarm.app.R
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.ScheduleType
import com.smartalarm.app.data.entities.WorkSchedule
import com.smartalarm.app.databinding.FragmentEditScheduleBinding
import com.smartalarm.app.viewmodel.WorkScheduleViewModel
import kotlinx.coroutines.launch

class EditScheduleFragment : Fragment() {

    private var _binding: FragmentEditScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkScheduleViewModel by viewModels()
    private var existingSchedule: WorkSchedule? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupScheduleTypeTabs()
        binding.btnSaveSchedule.setOnClickListener { saveSchedule() }

        val scheduleId = arguments?.getLong("scheduleId", -1L) ?: -1L
        if (scheduleId != -1L) {
            lifecycleScope.launch {
                val schedule = (requireActivity().application as SmartAlarmApplication)
                    .workScheduleRepository.getScheduleById(scheduleId)
                schedule?.let { populateFields(it) }
                existingSchedule = schedule
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit_alarm, menu) // Reuses save icon
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> { saveSchedule(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupScheduleTypeTabs() {
        binding.tabLayoutScheduleType.addOnTabSelectedListener(
            object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                    val isRotating = tab.position == 1
                    binding.sectionFixedDays.visibility = if (!isRotating) View.VISIBLE else View.GONE
                    binding.sectionRotating.visibility = if (isRotating) View.VISIBLE else View.GONE
                }
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            }
        )
    }

    private fun populateFields(schedule: WorkSchedule) {
        binding.etScheduleName.setText(schedule.name)

        if (schedule.scheduleType == ScheduleType.ROTATING) {
            binding.tabLayoutScheduleType.getTabAt(1)?.select()
        }

        // Work day toggles
        binding.toggleMon.isChecked = 1 in schedule.workDays
        binding.toggleTue.isChecked = 2 in schedule.workDays
        binding.toggleWed.isChecked = 3 in schedule.workDays
        binding.toggleThu.isChecked = 4 in schedule.workDays
        binding.toggleFri.isChecked = 5 in schedule.workDays
        binding.toggleSat.isChecked = 6 in schedule.workDays
        binding.toggleSun.isChecked = 7 in schedule.workDays
    }

    private fun saveSchedule() {
        val name = binding.etScheduleName.text.toString().trim().ifBlank { "Work Schedule" }
        val isRotating = binding.tabLayoutScheduleType.selectedTabPosition == 1

        val workDays = mutableSetOf<Int>().apply {
            if (binding.toggleMon.isChecked) add(1)
            if (binding.toggleTue.isChecked) add(2)
            if (binding.toggleWed.isChecked) add(3)
            if (binding.toggleThu.isChecked) add(4)
            if (binding.toggleFri.isChecked) add(5)
            if (binding.toggleSat.isChecked) add(6)
            if (binding.toggleSun.isChecked) add(7)
        }

        val schedule = (existingSchedule ?: WorkSchedule(name = name)).copy(
            name = name,
            scheduleType = if (isRotating) ScheduleType.ROTATING else ScheduleType.FIXED_DAYS,
            workDays = workDays
        )

        lifecycleScope.launch {
            val repo = (requireActivity().application as SmartAlarmApplication).workScheduleRepository
            if (schedule.id == 0L) repo.insertSchedule(schedule) else repo.updateSchedule(schedule)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
