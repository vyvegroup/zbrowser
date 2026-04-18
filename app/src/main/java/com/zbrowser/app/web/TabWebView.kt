package com.zbrowser.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.webkit.*
import android.widget.FrameLayout
// BuildConfig accessed via reflection to avoid import issues

/**
 * Custom WebView that represents a single browser tab.
 */
@SuppressLint("SetJavaScriptEnabled")
class TabWebView(
    context: Context,
    val tabId: String,
    private val onPageStarted: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onProgressChanged: (Int) -> Unit = {},
    private val onTitleReceived: (String) -> Unit = {},
    private val onIconReceived: (Bitmap?) -> Unit = {},
    private val onRequestNewTab: (String) -> Unit = {},
    private val onDownloadRequested: (DownloadRequest) -> Unit = {}
) : WebView(context) {

    var currentUrl: String = "about:blank"
        private set
    var currentTitle: String = ""
        private set
    var currentFavicon: Bitmap? = null
        private set
    var isLoading: Boolean = false
        private set
    var pageProgress: Int = 0
        private set
    var isDesktopMode: Boolean = false

    private val defaultUserAgent = settings.userAgentString

    init {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setupSettings()
        setupWebViewClient()
        setupWebChromeClient()
        setupDownloadListener()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }

        // Enable WebView debugging in debug builds
        try {
            val buildConfig = Class.forName("com.zbrowser.app.BuildConfig")
            val debugField = buildConfig.getDeclaredField("DEBUG")
            if (debugField.getBoolean(null)) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        } catch (e: Exception) {
            // Not debug build, skip
        }
    }

    private fun setupWebViewClient() {
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                url?.let {
                    currentUrl = it
                    onPageStarted(it)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                url?.let {
                    currentUrl = it
                    onPageFinished(it)
                }
            }
        }
    }

    private fun setupWebChromeClient() {
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                pageProgress = newProgress
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let {
                    currentTitle = it
                    onTitleReceived(it)
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                currentFavicon = icon
                onIconReceived(icon)
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val href = view?.hitTestResult?.extra
                if (href != null) {
                    onRequestNewTab(href)
                }
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    private fun setupDownloadListener() {
        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadRequested(DownloadRequest(url, userAgent, contentDisposition, mimetype, contentLength))
        }
    }

    override fun loadUrl(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("about:blank") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(url, "UTF-8")}"
        }
        super.loadUrl(finalUrl)
    }

    fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        settings.apply {
            if (isDesktopMode) {
                userAgentString = defaultUserAgent.replace("Mobile", "eliboM").replace("Android", "diordnA")
                useWideViewPort = true
                loadWithOverviewMode = false
            } else {
                userAgentString = defaultUserAgent
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }
        reload()
    }

    fun cleanup() {
        stopLoading()
        settings.javaScriptEnabled = false
        clearHistory()
        clearCache(true)
        loadUrl("about:blank")
    }

    data class DownloadRequest(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimetype: String,
        val contentLength: Long
    )
}
