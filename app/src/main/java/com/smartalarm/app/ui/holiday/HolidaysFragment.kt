package com.smartalarm.app.ui.holiday

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.R
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.databinding.FragmentHolidaysBinding
import com.smartalarm.app.viewmodel.HolidayViewModel
import com.smartalarm.app.viewmodel.HolidayViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class HolidaysFragment : Fragment(R.layout.fragment_holidays) {

    private var _binding: FragmentHolidaysBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HolidayViewModel by viewModels {
        val app = requireActivity().application as AlarmApplication
        HolidayViewModelFactory(app, app.holidayRepository)
    }

    private val adapter = HolidaysAdapter(
        onDelete = { holiday ->
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_holiday_title)
                .setMessage(getString(R.string.delete_holiday_message, holiday.name))
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteHoliday(holiday) }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHolidaysBinding.bind(view)

        binding.recyclerHolidays.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHolidays.adapter = adapter

        binding.fabAddHoliday.setOnClickListener { showAddHolidayDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allHolidays.collect { holidays ->
                    adapter.submitList(holidays)
                    binding.emptyState.visibility =
                        if (holidays.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showAddHolidayDialog() {
        val options = arrayOf(
            getString(R.string.holiday_type_annual),
            getString(R.string.holiday_type_single_day),
            getString(R.string.action_import_us_holidays)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_holiday_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickDate(annual = true)
                    1 -> pickDate(annual = false)
                    2 -> viewModel.importUsHolidays()
                }
            }
            .show()
    }

    private fun pickDate(annual: Boolean) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                showNameDialog(
                    month = month + 1,
                    day = day,
                    year = if (annual) null else year,
                    type = if (annual) HolidayType.ANNUAL else HolidayType.ONE_TIME
                )
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showNameDialog(month: Int, day: Int, year: Int?, type: HolidayType) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.add_holiday_title)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_holiday_title)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "Holiday" }
                viewModel.addHoliday(name, month, day, year, type)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
