package com.zbrowser.app.ui.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.zbrowser.app.MainActivity
import com.zbrowser.app.R
import com.zbrowser.app.databinding.FragmentBrowserBinding
import com.zbrowser.app.storage.GitHubStorageManager
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

    private var currentWebView: TabWebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupClickListeners()
        setupWebViewCallbacks()

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

    private fun setupWebViewCallbacks() {
        webViewManager?.callbacks = object : WebViewManager.TabCallbacks {
            override fun onPageStarted(tabId: String, url: String) {
                if (tabId == webViewManager?.activeTabId?.value) {
                    viewModel.updateUrl(url)
                    viewModel.updateProgress(0)
                }
            }

            override fun onPageFinished(tabId: String, url: String) {
                if (tabId == webViewManager?.activeTabId?.value) {
                    viewModel.updateUrl(url)
                    viewModel.updateProgress(100)
                }
            }

            override fun onProgressChanged(tabId: String, progress: Int) {
                if (tabId == webViewManager?.activeTabId?.value) {
                    viewModel.updateProgress(progress)
                }
            }

            override fun onTitleReceived(tabId: String, title: String) {
                if (tabId == webViewManager?.activeTabId?.value) {
                    viewModel.updateTitle(title)
                }
            }

            override fun onIconReceived(tabId: String, icon: Bitmap?) {
                // Could update favicon display
            }

            override fun onRequestNewTab(url: String): String {
                return webViewManager?.createTab(url) ?: ""
            }

            override fun onDownloadRequested(tabId: String, request: TabWebView.DownloadRequest) {
                mainActivity?.handleDownload(request)
            }

            override fun onLongPressHit(tabId: String, hitResult: WebViewManager.HitResult) {
                if (tabId == webViewManager?.activeTabId?.value) {
                    activity?.runOnUiThread { showHitResultModal(hitResult) }
                }
            }
        }
    }

    private fun showHitResultModal(hitResult: WebViewManager.HitResult) {
        val context = context ?: return
        val dialog = BottomSheetDialog(context)

        val contentView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 36)
        }

        // Title
        val titleText = when (hitResult.type) {
            WebViewManager.HitResult.TYPE_IMAGE -> "Image Options"
            WebViewManager.HitResult.TYPE_IMAGE_LINK -> "Image Link Options"
            else -> "Link Options"
        }
        contentView.addView(TextView(context).apply {
            text = titleText
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setPadding(0, 0, 0, 24)
        })

        // Preview image for image hits
        if (hitResult.src != null) {
            val previewImage = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400
                ).apply { setMargins(0, 0, 0, 24) }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(android.R.drawable.ic_menu_gallery)
            }
            contentView.addView(previewImage)

            // Load image with Coil
            val imageLoader = ImageLoader.Builder(context).build()
            val request = ImageRequest.Builder(context)
                .data(hitResult.src)
                .target(object : coil.target.Target {
                    override fun onStart(placeholder: Drawable?) {
                        previewImage.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                    override fun onSuccess(result: Drawable) {
                        previewImage.setImageDrawable(result)
                    }
                    override fun onError(error: Drawable?) {
                        previewImage.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                })
                .build()
            imageLoader.enqueue(request)
        }

        // URL info
        val urlToShow = when (hitResult.type) {
            WebViewManager.HitResult.TYPE_IMAGE -> hitResult.src
            WebViewManager.HitResult.TYPE_IMAGE_LINK -> hitResult.url ?: hitResult.src
            else -> hitResult.url
        }
        if (urlToShow != null) {
            val urlView = TextView(context).apply {
                text = urlToShow
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(0, 0, 0, 16)
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            }
            contentView.addView(urlView)
        }

        // Action buttons based on hit type
        fun addButton(text: String, iconRes: Int = 0, onClick: () -> Unit) {
            val btn = com.google.android.material.button.MaterialButton(context).apply {
                this.text = text
                icon = if (iconRes != 0) resources.getDrawable(iconRes, null) else null
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 16
                setPadding(0, 12, 0, 12)
                textSize = 14f
                elevation = 0f
                stateListAnimator = null
                cornerRadius = 16f
                setOnClickListener {
                    dialog.dismiss()
                    onClick()
                }
            }
            contentView.addView(btn)
        }

        when (hitResult.type) {
            WebViewManager.HitResult.TYPE_IMAGE -> {
                addButton("Open Image") { currentWebView?.loadUrl(hitResult.src ?: return@addButton) }
                addButton("Save Image") { downloadFile(hitResult.src ?: return@addButton, "image") }
                addButton("Share Image") { shareUrl(hitResult.src ?: return@addButton) }
                addButton("Copy Image URL") { copyToClipboard(hitResult.src ?: return@addButton) }
                if (githubStorage.isEnabled && githubStorage.isConfigured) {
                    addButton("Save to GitHub") { uploadToGithub(hitResult.src ?: return@addButton, "images") }
                }
            }
            WebViewManager.HitResult.TYPE_IMAGE_LINK -> {
                addButton("Open Link") { currentWebView?.loadUrl(hitResult.url ?: return@addButton) }
                addButton("Open in New Tab") { webViewManager?.createTab(hitResult.url) }
                addButton("Save Image") { downloadFile(hitResult.src ?: return@addButton, "image") }
                addButton("Copy Link URL") { copyToClipboard(hitResult.url ?: return@addButton) }
                addButton("Copy Image URL") { copyToClipboard(hitResult.src ?: return@addButton) }
                addButton("Share Link") { shareUrl(hitResult.url ?: return@addButton) }
                if (githubStorage.isEnabled && githubStorage.isConfigured) {
                    addButton("Save to GitHub") { uploadToGithub(hitResult.src ?: return@addButton, "images") }
                }
            }
            WebViewManager.HitResult.TYPE_LINK -> {
                addButton("Open Link") { currentWebView?.loadUrl(hitResult.url ?: return@addButton) }
                addButton("Open in New Tab") { webViewManager?.createTab(hitResult.url) }
                addButton("Copy Link URL") { copyToClipboard(hitResult.url ?: return@addButton) }
                addButton("Share Link") { shareUrl(hitResult.url ?: return@addButton) }
                addButton("Download Link") { downloadFile(hitResult.url ?: return@addButton) }
                if (githubStorage.isEnabled && githubStorage.isConfigured) {
                    addButton("Save to GitHub") { uploadToGithub(hitResult.url ?: return@addButton) }
                }
            }
        }

        val scrollView = ScrollView(context).apply { addView(contentView) }
        dialog.setContentView(scrollView)
        dialog.show()
    }

    private val githubStorage: GitHubStorageManager by lazy {
        GitHubStorageManager.getInstance(requireContext())
    }

    private fun downloadFile(url: String, typeHint: String = "") {
        val request = TabWebView.DownloadRequest(
            url = url,
            userAgent = currentWebView?.settings?.userAgentString ?: "",
            contentDisposition = "",
            mimetype = if (typeHint == "image") "image/*" else "",
            contentLength = 0
        )
        mainActivity?.handleDownload(request)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = android.content.ClipboardManager::class.java.cast(
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        )
        val clip = android.content.ClipData.newPlainText("URL", text)
        clipboard?.setPrimaryClip(clip)
        Snackbar.make(binding.root, "Copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun uploadToGithub(url: String, subFolder: String = "downloads") {
        val fileName = url.substringAfterLast('/').substringBefore('?').ifEmpty {
            "file_${System.currentTimeMillis()}"
        }
        lifecycleScope.launch {
            val result = githubStorage.uploadFromUrl(
                fileUrl = url,
                fileName = fileName,
                subFolder = subFolder
            )
            if (result != null) {
                Snackbar.make(binding.root, "Saved to GitHub!", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Failed to save to GitHub", Snackbar.LENGTH_SHORT).show()
            }
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
                R.id.action_share -> { viewModel.currentUrl.value.let { shareUrl(it) }; true }
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
        // Detach WebView from container to prevent leaks
        currentWebView?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
        currentWebView = null
        _binding = null
        super.onDestroyView()
    }
}
