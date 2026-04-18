package com.zbrowser.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zbrowser.app.MainActivity
import com.zbrowser.app.R
import com.zbrowser.app.ZBrowserApp
import com.zbrowser.app.data.HistoryEntry
import com.zbrowser.app.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter
    private val historyDao by lazy { (requireActivity().application as ZBrowserApp).database.historyDao() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onHistoryClick = { entry ->
                (activity as? MainActivity)?.webViewManager?.createTab(entry.url)
                findNavController().navigate(R.id.browserFragment)
            },
            onHistoryLongClick = { entry ->
                showHistoryOptionsDialog(entry)
            }
        )
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_clear_history -> {
                    showClearHistoryDialog()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyDao.getAllHistory().collect { historyList ->
                    adapter.submitList(historyList)
                    binding.emptyState.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerHistory.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_history)
            .setMessage(R.string.dialog_confirm_clear_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                lifecycleScope.launch {
                    historyDao.deleteAll()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showHistoryOptionsDialog(entry: HistoryEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(entry.title)
            .setItems(arrayOf(getString(R.string.remove_bookmark))) { _, _ ->
                lifecycleScope.launch {
                    historyDao.delete(entry)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
