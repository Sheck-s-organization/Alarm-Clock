package com.smartalarm.app.ui.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentAlarmsBinding
import com.smartalarm.app.viewmodel.AlarmViewModel
import com.smartalarm.app.viewmodel.OperationResult

class AlarmsFragment : Fragment() {

    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlarmViewModel by viewModels()
    private lateinit var adapter: AlarmAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm.id, enabled) },
            onEdit = { alarm ->
                val action = AlarmsFragmentDirections.actionAlarmsToEditAlarm(alarm.id)
                findNavController().navigate(action)
            },
            onDelete = { alarm ->
                viewModel.deleteAlarm(alarm)
            }
        )

        binding.recyclerAlarms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AlarmsFragment.adapter
        }

        viewModel.allAlarms.observe(viewLifecycleOwner) { alarms ->
            adapter.submitList(alarms)
            binding.emptyState.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is OperationResult.Success ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
                is OperationResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.fabAddAlarm.setOnClickListener {
            findNavController().navigate(R.id.action_alarms_to_editAlarm)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
