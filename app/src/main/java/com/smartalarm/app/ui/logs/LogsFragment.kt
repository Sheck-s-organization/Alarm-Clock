package com.smartalarm.app.ui.logs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartalarm.app.R
import com.smartalarm.app.databinding.FragmentLogsBinding
import com.smartalarm.app.util.LogBuffer
import kotlinx.coroutines.launch

class LogsFragment : Fragment(R.layout.fragment_logs) {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogsBinding.bind(view)

        val adapter = LogsAdapter()
        binding.recyclerLogs.adapter = adapter

        binding.toolbarLogs.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_clear_logs) {
                LogBuffer.clear()
                true
            } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            LogBuffer.entries.collect { entries ->
                adapter.submitList(entries)
                binding.emptyLogs.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerLogs.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
                if (entries.isNotEmpty()) {
                    binding.recyclerLogs.scrollToPosition(entries.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
