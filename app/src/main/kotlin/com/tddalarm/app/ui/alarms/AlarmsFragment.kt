package com.tddalarm.app.ui.alarms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.databinding.FragmentAlarmsBinding
import com.tddalarm.app.ui.AlarmListViewModel
import com.tddalarm.app.ui.ViewModelFactory
import kotlinx.coroutines.launch

class AlarmsFragment : Fragment() {

    private val viewModel: AlarmListViewModel by activityViewModels {
        ViewModelFactory(AlarmApplication.from(requireContext()).container)
    }

    private var binding: FragmentAlarmsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentAlarmsBinding.inflate(inflater, container, false)
        binding = b

        val adapter = AlarmAdapter(
            onToggle = { item, enabled -> viewModel.setEnabled(item.alarm, enabled) },
            onClick = { item ->
                AlarmEditDialogFragment.forAlarm(item.alarm)
                    .show(parentFragmentManager, "edit_alarm")
            },
        )
        b.alarmList.layoutManager = LinearLayoutManager(requireContext())
        b.alarmList.adapter = adapter

        b.addAlarmFab.setOnClickListener {
            AlarmEditDialogFragment.forNewAlarm().show(parentFragmentManager, "add_alarm")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                    b.emptyText.isVisible = items.isEmpty()
                }
            }
        }
        return b.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
