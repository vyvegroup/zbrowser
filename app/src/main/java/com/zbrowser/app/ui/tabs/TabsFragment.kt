package com.zbrowser.app.ui.tabs

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
import com.zbrowser.app.MainActivity
import com.zbrowser.app.R
import com.zbrowser.app.databinding.FragmentTabsBinding
import com.zbrowser.app.web.WebViewManager
import kotlinx.coroutines.launch

class TabsFragment : Fragment() {

    private var _binding: FragmentTabsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TabsAdapter

    private val webViewManager: WebViewManager?
        get() = (activity as? MainActivity)?.webViewManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeTabs()
    }

    private fun setupRecyclerView() {
        adapter = TabsAdapter(
            onTabClick = { tabId ->
                webViewManager?.switchToTab(tabId)
                findNavController().navigate(R.id.browserFragment)
            },
            onTabClose = { tabId ->
                webViewManager?.closeTab(tabId)
            }
        )
        binding.recyclerTabs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTabs.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabNewTab.setOnClickListener {
            webViewManager?.createTab("https://www.google.com")
            findNavController().navigate(R.id.browserFragment)
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_close_all -> {
                    webViewManager?.closeAllTabs()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeTabs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                webViewManager?.tabs?.collect { tabsMap ->
                    val tabInfoList = webViewManager?.getAllTabInfo() ?: emptyList()
                    adapter.submitList(tabInfoList)
                    binding.emptyState.visibility = if (tabInfoList.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerTabs.visibility = if (tabInfoList.isEmpty()) View.GONE else View.VISIBLE
                    binding.toolbar.subtitle = getString(R.string.tabs_count, tabInfoList.size)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
