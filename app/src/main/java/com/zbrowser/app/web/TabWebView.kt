package com.zbrowser.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.*
import android.widget.FrameLayout

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
    private val onDownloadRequested: (DownloadRequest) -> Unit = {},
    private val onLongPressHit: (WebViewManager.HitResult) -> Unit = {}
) : WebView(context.applicationContext) {

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

    // Anti-drag: track long press state
    private var isLongPressActive = false
    private var longPressStartTime = 0L

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
                val url = request.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // Block non-web schemes from auto-navigating
                return try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                url?.let {
                    currentUrl = it
                    onPageStarted(it)
                }
                // Inject anti-drag CSS for images and links
                injectAntiDragScript()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                pageProgress = 100
                url?.let {
                    currentUrl = it
                    onPageFinished(it)
                }
                onProgressChanged(100)
                // Re-inject anti-drag after page loads
                injectAntiDragScript()
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                if (detail != null && detail.didCrash()) {
                    // WebView crashed, try to recover
                    loadUrl("about:blank")
                }
                return true
            }
        }
    }

    private fun injectAntiDragScript() {
        evaluateJavascript("""
            (function() {
                if (window._zbrowserAntiDragInjected) return;
                window._zbrowserAntiDragInjected = true;

                function preventDrag(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }

                // Prevent drag on all images and links
                document.addEventListener('dragstart', function(e) {
                    if (e.target.tagName === 'IMG' || e.target.tagName === 'A' || e.target.closest('a') || e.target.closest('img')) {
                        e.preventDefault();
                        e.stopPropagation();
                        return false;
                    }
                }, true);

                // Prevent native image long-press drag on mobile
                document.addEventListener('touchmove', function(e) {
                    // Allow normal scroll, but prevent drag on images/links
                    var target = e.target;
                    if (target.tagName === 'IMG' || target.tagName === 'A' || target.closest('a') || target.closest('img')) {
                        // Only prevent if it looks like a drag (not a scroll)
                        // We allow scroll to work normally
                    }
                }, {passive: true});

                // Set draggable false on images and links
                var style = document.createElement('style');
                style.textContent = 'img { -webkit-user-drag: none; user-drag: none; } a { -webkit-user-drag: none; user-drag: none; } a img { -webkit-user-drag: none; user-drag: none; }';
                document.head.appendChild(style);

                // Set draggable attribute on existing elements
                document.querySelectorAll('img, a').forEach(function(el) {
                    el.setAttribute('draggable', 'false');
                });

                // Observe DOM changes to apply to new elements
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                if (node.tagName === 'IMG' || node.tagName === 'A') {
                                    node.setAttribute('draggable', 'false');
                                }
                                node.querySelectorAll && node.querySelectorAll('img, a').forEach(function(el) {
                                    el.setAttribute('draggable', 'false');
                                });
                            }
                        });
                    });
                });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            })();
        """.trimIndent(), null)
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
                // Only grant safe permissions
                val safeResources = request?.resources?.filter {
                    it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
                    it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                }?.toTypedArray()
                if (safeResources != null && safeResources.isNotEmpty()) {
                    request.grant(safeResources)
                } else {
                    request?.grant(request.resources)
                }
            }
        }
    }

    private fun setupDownloadListener() {
        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadRequested(DownloadRequest(url, userAgent, contentDisposition, mimetype, contentLength))
        }
    }

    // Override onInterceptTouchEvent to handle long press for our custom modal
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.action == MotionEvent.ACTION_DOWN) {
            isLongPressActive = false
            longPressStartTime = System.currentTimeMillis()
        }
        return super.onInterceptTouchEvent(ev)
    }

    // Handle long click to show our custom modal instead of default behavior
    override fun performLongClick(): Boolean {
        val result = hitTestResult
        val hitType = result.type
        val extra = result.extra

        when (hitType) {
            HitTestResult.IMAGE_TYPE -> {
                // Pure image (no link)
                onLongPressHit(WebViewManager.HitResult(
                    type = WebViewManager.HitResult.TYPE_IMAGE,
                    url = null,
                    src = extra,
                    title = null,
                    alt = null
                ))
                return true
            }
            HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                // Image inside a link - get both image src and link href
                val imageUrl = extra
                // We need JavaScript to get the parent link href
                evaluateJavascript("""
                    (function() {
                        var el = document.elementFromPoint(${lastTouchEvent?.x ?: 0}, ${lastTouchEvent?.y ?: 0});
                        if (el && el.tagName === 'IMG') {
                            var parent = el.closest('a');
                            if (parent) {
                                return JSON.stringify({link: parent.href, img: el.src, alt: el.alt || '', title: parent.title || el.title || ''});
                            }
                            return JSON.stringify({link: null, img: el.src, alt: el.alt || '', title: el.title || ''});
                        }
                        return JSON.stringify({link: null, img: null, alt: '', title: ''});
                    })();
                """.trimIndent()) { json ->
                    try {
                        val obj = org.json.JSONObject(json)
                        val linkUrl = obj.optString("link")
                        val imgSrc = obj.optString("img") ?: imageUrl
                        val alt = obj.optString("alt")
                        val title = obj.optString("title")
                        onLongPressHit(WebViewManager.HitResult(
                            type = if (linkUrl.isNotEmpty() && linkUrl != "null")
                                WebViewManager.HitResult.TYPE_IMAGE_LINK
                            else WebViewManager.HitResult.TYPE_IMAGE,
                            url = if (linkUrl.isNotEmpty() && linkUrl != "null") linkUrl else null,
                            src = imgSrc,
                            title = title,
                            alt = alt
                        ))
                    } catch (e: Exception) {
                        onLongPressHit(WebViewManager.HitResult(
                            type = WebViewManager.HitResult.TYPE_IMAGE,
                            url = null,
                            src = imageUrl,
                            title = null,
                            alt = null
                        ))
                    }
                }
                return true
            }
            HitTestResult.SRC_ANCHOR_TYPE -> {
                // Pure link (not an image)
                onLongPressHit(WebViewManager.HitResult(
                    type = WebViewManager.HitResult.TYPE_LINK,
                    url = extra,
                    src = null,
                    title = null,
                    alt = null
                ))
                return true
            }
            HitTestResult.ANCHOR_TYPE -> {
                onLongPressHit(WebViewManager.HitResult(
                    type = WebViewManager.HitResult.TYPE_LINK,
                    url = extra,
                    src = null,
                    title = null,
                    alt = null
                ))
                return true
            }
        }

        return super.performLongClick()
    }

    private var lastTouchEvent: MotionEvent? = null

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        lastTouchEvent = ev
        return super.onTouchEvent(ev)
    }

    override fun loadUrl(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("about:blank") -> url
            url.startsWith("javascript:") -> url
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
