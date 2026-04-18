package com.zbrowser.app.ui.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.zbrowser.app.MainActivity
import com.zbrowser.app.R
import com.zbrowser.app.databinding.FragmentBrowserBinding
import com.zbrowser.app.web.TabWebView
import com.zbrowser.app.web.WebViewManager
import kotlinx.coroutines.launch

class BrowserFragment : Fragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowserViewModel by viewModels()

    private val mainActivity: MainActivity?
        get() = activity as? MainActivity

    private val webViewManager: WebViewManager?
        get() = mainActivity?.webViewManager

    private var currentWebView: com.zbrowser.app.web.TabWebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()

        if (webViewManager?.getTabCount() == 0) {
            webViewManager?.createTab("https://www.google.com")
        }
        attachActiveTab()
    }

    private fun setupViews() {
        binding.searchView.setupWithSearchBar(binding.searchBar)
        binding.swipeRefresh.setColorSchemeResources(
            com.google.android.material.R.color.material_dynamic_primary60
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachActiveTab() {
        val webView = webViewManager?.getActiveTab() ?: return
        currentWebView = webView

        (webView.parent as? ViewGroup)?.removeView(webView)

        binding.webviewContainer.removeAllViews()
        binding.webviewContainer.addView(webView)

        viewModel.updateUrl(webView.currentUrl)
        viewModel.updateTitle(webView.currentTitle)
        viewModel.updateProgress(webView.pageProgress)
        viewModel.updateTabCount(webViewManager?.getTabCount() ?: 0)

        val displayUrl = viewModel.formatUrlForDisplay(webView.currentUrl)
        if (displayUrl.isNotEmpty() && displayUrl != "about:blank") {
            binding.searchBar.setText(displayUrl)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadingProgress.collect { progress ->
                        if (progress in 1..99) {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.progressBar.progress = progress
                        } else {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }
                launch {
                    viewModel.currentUrl.collect { url ->
                        val displayUrl = viewModel.formatUrlForDisplay(url)
                        if (displayUrl.isNotEmpty() && displayUrl != "about:blank") {
                            binding.searchBar.setText(displayUrl)
                        }
                    }
                }
                launch {
                    viewModel.isBookmarked.collect { isBookmarked ->
                        binding.btnBookmark.setImageResource(
                            if (isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            currentWebView?.let { if (it.canGoBack()) it.goBack() }
        }

        binding.btnForward.setOnClickListener {
            currentWebView?.let { if (it.canGoForward()) it.goForward() }
        }

        binding.btnBookmark.setOnClickListener {
            val url = viewModel.currentUrl.value
            val title = viewModel.currentTitle.value.ifBlank { url }
            viewModel.toggleBookmark(title, url)
            val msg = if (viewModel.isBookmarked.value) getString(R.string.bookmark_removed) else getString(R.string.bookmark_added)
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnMenu.setOnClickListener {
            showBrowserMenu()
        }

        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchView.text.toString().trim()
                if (query.isNotEmpty()) {
                    navigateTo(query)
                    binding.searchView.hide()
                }
                true
            } else false
        }

        binding.chipGoogle.setOnClickListener { navigateTo("https://www.google.com") }
        binding.chipYoutube.setOnClickListener { navigateTo("https://www.youtube.com") }
        binding.chipGithub.setOnClickListener { navigateTo("https://github.com") }
        binding.chipWikipedia.setOnClickListener { navigateTo("https://www.wikipedia.org") }

        binding.swipeRefresh.setOnRefreshListener {
            currentWebView?.reload()
        }

        binding.btnBottomHome.setOnClickListener { navigateTo("https://www.google.com") }
        binding.btnBottomTabs.setOnClickListener { findNavController().navigate(R.id.tabsFragment) }
        binding.btnBottomBookmarks.setOnClickListener { findNavController().navigate(R.id.bookmarksFragment) }
        binding.btnBottomHistory.setOnClickListener { findNavController().navigate(R.id.historyFragment) }
        binding.btnBottomSettings.setOnClickListener { findNavController().navigate(R.id.settingsFragment) }

        binding.btnFindClose.setOnClickListener { hideFindBar() }
        binding.btnFindNext.setOnClickListener { currentWebView?.findNext(true) }
        binding.btnFindPrev.setOnClickListener { currentWebView?.findNext(false) }

        binding.findInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentWebView?.findAllAsync(binding.findInput.text.toString())
                true
            } else false
        }
    }

    private fun navigateTo(input: String) {
        val url = if (WebViewManager.isUrl(input)) {
            if (input.startsWith("http")) input else "https://$input"
        } else {
            WebViewManager.createSearchUrl(input)
        }
        currentWebView?.loadUrl(url)
        viewModel.addToHistory(input, url)
    }

    private fun showBrowserMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMenu)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.menu.findItem(R.id.action_desktop)?.isChecked = currentWebView?.isDesktopMode == true
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new_tab -> {
                    webViewManager?.createTab("https://www.google.com")
                    attachActiveTab()
                    true
                }
                R.id.action_incognito -> {
                    webViewManager?.createTab("https://www.google.com", isIncognito = true)
                    attachActiveTab()
                    true
                }
                R.id.action_refresh -> { currentWebView?.reload(); true }
                R.id.action_find -> { showFindBar(); true }
                R.id.action_desktop -> { currentWebView?.toggleDesktopMode(); item.isChecked = !item.isChecked; true }
                R.id.action_share -> { viewModel.currentUrl.value.let { mainActivity?.shareUrl(it) }; true }
                R.id.action_downloads -> {
                    try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                    catch (e: Exception) { Toast.makeText(requireContext(), "Cannot open downloads", Toast.LENGTH_SHORT).show() }
                    true
                }
                R.id.action_settings -> { findNavController().navigate(R.id.settingsFragment); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showFindBar() {
        binding.findBar.visibility = View.VISIBLE
        binding.findInput.requestFocus()
    }

    private fun hideFindBar() {
        binding.findBar.visibility = View.GONE
        currentWebView?.clearMatches()
    }

    override fun onResume() {
        super.onResume()
        currentWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        currentWebView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
