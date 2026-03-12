package com.smartalarm.app.ui.holiday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentHolidaysBinding
import com.smartalarm.app.viewmodel.HolidayViewModel
import com.smartalarm.app.viewmodel.OperationResult
import java.util.Calendar

class HolidaysFragment : Fragment() {

    private var _binding: FragmentHolidaysBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HolidayViewModel by viewModels()
    private lateinit var adapter: HolidayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        adapter = HolidayAdapter(
            onDelete = { holiday ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_holiday_title)
                    .setMessage(getString(R.string.delete_holiday_message, holiday.name))
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.deleteHoliday(holiday)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        )

        binding.recyclerHolidays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HolidaysFragment.adapter
        }

        viewModel.allHolidays.observe(viewLifecycleOwner) { holidays ->
            adapter.submitList(holidays)
            binding.emptyState.visibility = if (holidays.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is OperationResult.Success ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                is OperationResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.fabAddHoliday.setOnClickListener {
            showAddHolidayDialog()
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_holidays, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_import_us_holidays -> {
                        viewModel.seedUsHolidays()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showAddHolidayDialog() {
        val options = arrayOf(
            getString(R.string.holiday_type_single_day),
            getString(R.string.holiday_type_annual),
            getString(R.string.holiday_type_vacation)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_holiday_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSingleDayPicker()
                    1 -> showAnnualHolidayDialog()
                    2 -> showVacationRangePicker()
                }
            }
            .show()
    }

    private fun showSingleDayPicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_day_off))
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            viewModel.addPersonalDayOff(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
        picker.show(parentFragmentManager, "day_picker")
    }

    private fun showAnnualHolidayDialog() {
        // In a production app this would be a custom dialog with month/day pickers and a name field
        // For brevity, we show an example using the US holidays import
        viewModel.seedUsHolidays()
    }

    private fun showVacationRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.pick_vacation_range))
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val startCal = Calendar.getInstance().apply { timeInMillis = range.first }
            val endCal = Calendar.getInstance().apply { timeInMillis = range.second }
            viewModel.addVacationRange(
                startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1,
                startCal.get(Calendar.DAY_OF_MONTH),
                endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1,
                endCal.get(Calendar.DAY_OF_MONTH)
            )
        }
        picker.show(parentFragmentManager, "vacation_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
