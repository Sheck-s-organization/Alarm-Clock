package com.tddalarm.app.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.R
import com.tddalarm.app.databinding.DialogAddHolidayBinding
import com.tddalarm.app.databinding.FragmentWorkScheduleBinding
import com.tddalarm.app.ui.ViewModelFactory
import com.tddalarm.app.ui.WorkScheduleViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

class WorkScheduleFragment : Fragment() {

    private val viewModel: WorkScheduleViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentWorkScheduleBinding.inflate(inflater, container, false)

        val dayChips: Map<DayOfWeek, Chip> = mapOf(
            DayOfWeek.MONDAY to b.chipMon,
            DayOfWeek.TUESDAY to b.chipTue,
            DayOfWeek.WEDNESDAY to b.chipWed,
            DayOfWeek.THURSDAY to b.chipThu,
            DayOfWeek.FRIDAY to b.chipFri,
            DayOfWeek.SATURDAY to b.chipSat,
            DayOfWeek.SUNDAY to b.chipSun,
        )
        dayChips.forEach { (day, chip) ->
            chip.setOnClickListener { viewModel.toggleDay(day) }
        }

        val ptoAdapter = DeletableRowAdapter { viewModel.removePto(it) }
        b.ptoList.layoutManager = LinearLayoutManager(requireContext())
        b.ptoList.adapter = ptoAdapter

        val holidayAdapter = DeletableRowAdapter { viewModel.removeHoliday(it) }
        b.holidayList.layoutManager = LinearLayoutManager(requireContext())
        b.holidayList.adapter = holidayAdapter

        b.addPtoButton.setOnClickListener { pickPtoRange() }
        b.addHolidayButton.setOnClickListener { addHoliday() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.workingDays.collect { days ->
                        dayChips.forEach { (day, chip) ->
                            if (chip.isChecked != day in days) chip.isChecked = day in days
                        }
                    }
                }
                launch {
                    viewModel.ptoEntries.collect { entries ->
                        ptoAdapter.submitList(entries.map { DeletableRow(it.id, "${it.range.start} → ${it.range.endInclusive}") })
                    }
                }
                launch {
                    viewModel.holidayEntries.collect { entries ->
                        holidayAdapter.submitList(entries.map { DeletableRow(it.id, describeHoliday(it)) })
                    }
                }
            }
        }
        return b.root
    }

    private fun describeHoliday(entry: com.tddalarm.app.data.HolidayEntry): String =
        when (val holiday = entry.holiday) {
            is com.tddalarm.core.schedule.Holiday.OneTime -> "${holiday.name} — ${holiday.date}"
            is com.tddalarm.core.schedule.Holiday.Annual ->
                "${holiday.name} — ${holiday.month.value}/${holiday.dayOfMonth} ${getString(R.string.repeats_annually_suffix)}"
        }

    private fun pickPtoRange() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.add_pto)
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first.toUtcLocalDate()
            val end = range.second.toUtcLocalDate()
            viewModel.addPto(start, end)
        }
        picker.show(parentFragmentManager, "pto_picker")
    }

    private fun addHoliday() {
        val dialogBinding = DialogAddHolidayBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_holiday)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.pick_date) { _, _ ->
                val name = dialogBinding.holidayNameInput.text.toString()
                val annual = dialogBinding.annualCheckbox.isChecked
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.pick_date)
                    .build()
                datePicker.addOnPositiveButtonClickListener { millis ->
                    val date = millis.toUtcLocalDate()
                    if (annual) {
                        viewModel.addAnnualHoliday(name, date)
                    } else {
                        viewModel.addOneTimeHoliday(name, date)
                    }
                }
                datePicker.show(parentFragmentManager, "holiday_date_picker")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun Long.toUtcLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
