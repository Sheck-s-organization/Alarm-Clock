package com.tddalarm.app.ui.alarms

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.R
import com.tddalarm.app.data.Place
import com.tddalarm.app.data.daysFromStorageString
import com.tddalarm.app.data.toStorageString
import com.tddalarm.app.databinding.DialogAlarmEditBinding
import com.tddalarm.app.ui.AlarmListViewModel
import com.tddalarm.app.ui.EditorResult
import com.tddalarm.app.ui.LocationsViewModel
import com.tddalarm.app.ui.ViewModelFactory
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import java.time.DayOfWeek
import kotlinx.coroutines.launch

class AlarmEditDialogFragment : DialogFragment() {

    private val viewModel: AlarmListViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    private val locationsViewModel: LocationsViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    private var availablePlaces: List<Place> = emptyList()

    private var binding: DialogAlarmEditBinding? = null
    private var dayChips: Map<DayOfWeek, Chip> = emptyMap()

    /** Set when a button already decided the outcome, so onDismiss doesn't auto-save. */
    private var closeHandled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAlarmEditBinding.inflate(layoutInflater).also { this.binding = it }
        val args = requireArguments()
        val alarmId = args.getLong(ARG_ID)
        val isNew = alarmId == 0L

        binding.timePicker.setIs24HourView(true)
        binding.timePicker.hour = args.getInt(ARG_HOUR)
        binding.timePicker.minute = args.getInt(ARG_MINUTE)
        binding.labelInput.setText(args.getString(ARG_LABEL).orEmpty())
        binding.skipDaysOffSwitch.isChecked = args.getBoolean(ARG_SKIP_DAYS_OFF)

        dayChips = mapOf(
            DayOfWeek.MONDAY to binding.chipMon,
            DayOfWeek.TUESDAY to binding.chipTue,
            DayOfWeek.WEDNESDAY to binding.chipWed,
            DayOfWeek.THURSDAY to binding.chipThu,
            DayOfWeek.FRIDAY to binding.chipFri,
            DayOfWeek.SATURDAY to binding.chipSat,
            DayOfWeek.SUNDAY to binding.chipSun,
        )
        val repeatDays = daysFromStorageString(args.getString(ARG_REPEAT_DAYS).orEmpty())
        dayChips.forEach { (day, chip) -> chip.isChecked = day in repeatDays }

        val initialPlaceId = args.getLong(ARG_PLACE_ID, NO_PLACE)
        binding.locationSwitch.isChecked = initialPlaceId != NO_PLACE
        binding.locationFields.isVisible = initialPlaceId != NO_PLACE
        binding.fireWhenUnknownSwitch.isChecked = args.getBoolean(ARG_FIRE_WHEN_UNKNOWN, true)
        binding.locationSwitch.setOnCheckedChangeListener { _, checked ->
            binding.locationFields.isVisible = checked
        }

        lifecycleScope.launch {
            locationsViewModel.items.collect { places ->
                availablePlaces = places
                binding.noPlacesHint.isVisible = places.isEmpty()
                binding.placeSpinner.isVisible = places.isNotEmpty()
                val selected = binding.placeSpinner.selectedItemPosition
                    .takeIf { it in places.indices }
                binding.placeSpinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    places.map { it.name },
                )
                val restoreIndex = selected
                    ?: places.indexOfFirst { it.id == initialPlaceId }.takeIf { it >= 0 }
                if (restoreIndex != null) binding.placeSpinner.setSelection(restoreIndex)
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isNew) R.string.add_alarm else R.string.edit_alarm)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ -> close(EditorResult.SAVE) }
            .setNegativeButton(R.string.cancel) { _, _ -> close(EditorResult.CANCEL) }
        if (!isNew) {
            builder.setNeutralButton(R.string.delete) { _, _ -> close(EditorResult.DELETE) }
        }
        return builder.create()
    }

    /**
     * Clicking off the dialog (tap outside / Back) saves the draft instead of
     * discarding it — an alarm the user set but forgot to save must still ring.
     * Only the explicit Cancel button discards.
     */
    override fun onDismiss(dialog: DialogInterface) {
        if (!closeHandled && activity?.isChangingConfigurations != true) {
            close(EditorResult.DISMISS)
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        binding = null
        dayChips = emptyMap()
        super.onDestroyView()
    }

    private fun close(result: EditorResult) {
        if (closeHandled) return
        closeHandled = true
        val binding = this.binding ?: return
        val args = requireArguments()
        val draft = Alarm(
            id = args.getLong(ARG_ID),
            label = binding.labelInput.text.toString().trim(),
            hour = binding.timePicker.hour,
            minute = binding.timePicker.minute,
            repeatDays = dayChips.filterValues { it.isChecked }.keys,
            enabled = args.getBoolean(ARG_ENABLED, true),
            skipOnDaysOff = binding.skipDaysOffSwitch.isChecked,
            locationRule = buildLocationRule(binding),
        )
        viewModel.onEditorClosed(draft, result)
    }

    private fun buildLocationRule(binding: DialogAlarmEditBinding): LocationRule? {
        if (!binding.locationSwitch.isChecked) return null
        val place = availablePlaces.getOrNull(binding.placeSpinner.selectedItemPosition) ?: return null
        return LocationRule(
            fence = place.fence,
            fireWhenLocationUnknown = binding.fireWhenUnknownSwitch.isChecked,
            placeId = place.id,
        )
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_LABEL = "label"
        private const val ARG_HOUR = "hour"
        private const val ARG_MINUTE = "minute"
        private const val ARG_REPEAT_DAYS = "repeat_days"
        private const val ARG_ENABLED = "enabled"
        private const val ARG_SKIP_DAYS_OFF = "skip_days_off"
        private const val ARG_PLACE_ID = "place_id"
        private const val ARG_FIRE_WHEN_UNKNOWN = "fire_when_unknown"
        private const val NO_PLACE = -1L

        fun forNewAlarm(): AlarmEditDialogFragment = AlarmEditDialogFragment().apply {
            arguments = bundleOf(ARG_ID to 0L, ARG_HOUR to 7, ARG_MINUTE to 0)
        }

        fun forAlarm(alarm: Alarm): AlarmEditDialogFragment = AlarmEditDialogFragment().apply {
            arguments = bundleOf(
                ARG_ID to alarm.id,
                ARG_LABEL to alarm.label,
                ARG_HOUR to alarm.hour,
                ARG_MINUTE to alarm.minute,
                ARG_REPEAT_DAYS to alarm.repeatDays.toStorageString(),
                ARG_ENABLED to alarm.enabled,
                ARG_SKIP_DAYS_OFF to alarm.skipOnDaysOff,
                ARG_PLACE_ID to (alarm.locationRule?.placeId ?: NO_PLACE),
                ARG_FIRE_WHEN_UNKNOWN to (alarm.locationRule?.fireWhenLocationUnknown ?: true),
            )
        }
    }
}
