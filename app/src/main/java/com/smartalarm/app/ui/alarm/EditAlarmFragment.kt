package com.smartalarm.app.ui.alarm

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
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.data.entities.ChargingRequirement
import com.smartalarm.app.databinding.FragmentEditAlarmBinding
import com.smartalarm.app.viewmodel.AlarmViewModel
import com.smartalarm.app.viewmodel.WorkScheduleViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class EditAlarmFragment : Fragment() {

    private var _binding: FragmentEditAlarmBinding? = null
    private val binding get() = _binding!!

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val scheduleViewModel: WorkScheduleViewModel by viewModels()

    private var existingAlarm: Alarm? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupAlarmTypePicker()
        setupChargingSpinner()
        binding.btnSave.setOnClickListener { saveAlarm() }

        // Load existing alarm if editing
        val alarmId = arguments?.getLong("alarmId", -1L) ?: -1L
        if (alarmId != -1L) {
            lifecycleScope.launch {
                val alarm = (requireActivity().application as com.smartalarm.app.SmartAlarmApplication)
                    .alarmRepository.getAlarmById(alarmId)
                alarm?.let { populateFields(it) }
                existingAlarm = alarm
            }
        } else {
            // Defaults for new alarm
            val now = Calendar.getInstance()
            binding.timePicker.hour = now.get(Calendar.HOUR_OF_DAY)
            binding.timePicker.minute = now.get(Calendar.MINUTE)
        }

        // Show / hide smart settings based on alarm type selection
        binding.radioGroupAlarmType.setOnCheckedChangeListener { _, checkedId ->
            updateSmartSectionsVisibility(checkedId)
        }

        // Day toggles
        setupDayToggles()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit_alarm, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        saveAlarm()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupAlarmTypePicker() {
        binding.radioGroupAlarmType.check(R.id.radio_standard)
        updateSmartSectionsVisibility(R.id.radio_standard)
    }

    private fun setupChargingSpinner() {
        val options = listOf("Not required", "Must be charging", "Must NOT be charging")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCharging.adapter = adapter
    }

    private fun setupDayToggles() {
        // Day toggle buttons Mon-Sun are wired up through layout click listeners
        // binding.toggleMonday etc.
    }

    private fun updateSmartSectionsVisibility(checkedId: Int) {
        binding.sectionWorkSchedule.visibility =
            if (checkedId == R.id.radio_work_schedule) View.VISIBLE else View.GONE
        binding.sectionTimeOfMonth.visibility =
            if (checkedId == R.id.radio_time_of_month) View.VISIBLE else View.GONE
        binding.sectionLocation.visibility =
            if (checkedId == R.id.radio_location) View.VISIBLE else View.GONE
        binding.sectionCharging.visibility =
            if (checkedId == R.id.radio_charging || checkedId == R.id.radio_work_schedule)
                View.VISIBLE else View.GONE
    }

    private fun populateFields(alarm: Alarm) {
        binding.timePicker.hour = alarm.hour
        binding.timePicker.minute = alarm.minute
        binding.etLabel.setText(alarm.label)
        binding.switchVibrate.isChecked = alarm.vibrate
        binding.seekVolume.progress = alarm.volumePercent

        val radioId = when (alarm.alarmType) {
            AlarmType.STANDARD -> R.id.radio_standard
            AlarmType.WORK_SCHEDULE -> R.id.radio_work_schedule
            AlarmType.TIME_OF_MONTH -> R.id.radio_time_of_month
            AlarmType.LOCATION -> R.id.radio_location
            AlarmType.CHARGING -> R.id.radio_charging
        }
        binding.radioGroupAlarmType.check(radioId)

        val chargingIndex = when (alarm.chargingRequirement) {
            ChargingRequirement.NOT_REQUIRED -> 0
            ChargingRequirement.CHARGING -> 1
            ChargingRequirement.NOT_CHARGING -> 2
        }
        binding.spinnerCharging.setSelection(chargingIndex)
    }

    private fun saveAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val label = binding.etLabel.text.toString().trim()
        val vibrate = binding.switchVibrate.isChecked
        val volume = binding.seekVolume.progress

        val alarmType = when (binding.radioGroupAlarmType.checkedRadioButtonId) {
            R.id.radio_work_schedule -> AlarmType.WORK_SCHEDULE
            R.id.radio_time_of_month -> AlarmType.TIME_OF_MONTH
            R.id.radio_location -> AlarmType.LOCATION
            R.id.radio_charging -> AlarmType.CHARGING
            else -> AlarmType.STANDARD
        }

        val chargingReq = when (binding.spinnerCharging.selectedItemPosition) {
            1 -> ChargingRequirement.CHARGING
            2 -> ChargingRequirement.NOT_CHARGING
            else -> ChargingRequirement.NOT_REQUIRED
        }

        val alarm = (existingAlarm ?: Alarm(hour = hour, minute = minute)).copy(
            hour = hour,
            minute = minute,
            label = label,
            vibrate = vibrate,
            volumePercent = volume,
            alarmType = alarmType,
            chargingRequirement = chargingReq,
            repeatDays = getSelectedDays()
        )

        lifecycleScope.launch {
            val repo = (requireActivity().application as com.smartalarm.app.SmartAlarmApplication).alarmRepository
            if (alarm.id == 0L) repo.insertAlarm(alarm) else repo.updateAlarm(alarm)
            findNavController().popBackStack()
        }
    }

    private fun getSelectedDays(): Set<Int> {
        val days = mutableSetOf<Int>()
        if (binding.toggleMonday.isChecked) days.add(1)
        if (binding.toggleTuesday.isChecked) days.add(2)
        if (binding.toggleWednesday.isChecked) days.add(3)
        if (binding.toggleThursday.isChecked) days.add(4)
        if (binding.toggleFriday.isChecked) days.add(5)
        if (binding.toggleSaturday.isChecked) days.add(6)
        if (binding.toggleSunday.isChecked) days.add(7)
        return days
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
