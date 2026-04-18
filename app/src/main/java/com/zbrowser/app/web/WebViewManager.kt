package com.zbrowser.app.web

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Manages multiple browser tabs, each with its own WebView instance.
 * Handles tab creation, switching, closing, and state persistence.
 */
class WebViewManager(private val context: Context) {

    data class TabInfo(
        val id: String,
        val title: String,
        val url: String,
        val isIncognito: Boolean = false,
        val favicon: android.graphics.Bitmap? = null
    )

    private val _tabs = MutableStateFlow<Map<String, TabWebView>>(emptyMap())
    val tabs: StateFlow<Map<String, TabWebView>> = _tabs

    private val _tabOrder = MutableStateFlow<List<String>>(emptyList())
    val tabOrder: StateFlow<List<String>> = _tabOrder

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId

    private val tabStateBundles = mutableMapOf<String, Bundle>()

    fun createTab(url: String? = null, isIncognito: Boolean = false): String {
        val tabId = UUID.randomUUID().toString()
        val webView = TabWebView(
            context = context,
            tabId = tabId,
            onPageStarted = { _ ->
                // Handled by observer
            },
            onPageFinished = { _ ->
                // Handled by observer
            },
            onProgressChanged = { _ ->
                // Handled by observer
            },
            onTitleReceived = { _ ->
                // Handled by observer
            },
            onIconReceived = { _ ->
                // Handled by observer
            },
            onRequestNewTab = { url ->
                createTab(url)
            },
            onDownloadRequested = { _ ->
                // Handled by activity
            }
        )

        _tabs.value = _tabs.value + (tabId to webView)
        _tabOrder.value = _tabOrder.value + tabId

        url?.let { webView.loadUrl(it) }

        switchToTab(tabId)
        return tabId
    }

    fun switchToTab(tabId: String) {
        val currentActiveId = _activeTabId.value
        currentActiveId?.let { id ->
            _tabs.value[id]?.let { webView ->
                val bundle = Bundle()
                webView.saveState(bundle)
                tabStateBundles[id] = bundle
            }
        }

        _activeTabId.value = tabId

        _tabs.value[tabId]?.let { webView ->
            tabStateBundles[tabId]?.let { bundle ->
                webView.restoreState(bundle)
            }
        }
    }

    fun closeTab(tabId: String) {
        _tabs.value[tabId]?.cleanup()
        _tabs.value = _tabs.value - tabId
        _tabOrder.value = _tabOrder.value - tabId
        tabStateBundles.remove(tabId)

        if (_activeTabId.value == tabId) {
            val remaining = _tabOrder.value
            if (remaining.isNotEmpty()) {
                switchToTab(remaining.last())
            } else {
                _activeTabId.value = null
            }
        }
    }

    fun closeAllTabs() {
        _tabs.value.values.forEach { it.cleanup() }
        _tabs.value = emptyMap()
        _tabOrder.value = emptyList()
        _activeTabId.value = null
        tabStateBundles.clear()
    }

    fun getActiveTab(): TabWebView? {
        return _activeTabId.value?.let { _tabs.value[it] }
    }

    fun getTabInfo(tabId: String): TabInfo? {
        val webView = _tabs.value[tabId] ?: return null
        return TabInfo(
            id = webView.tabId,
            title = webView.currentTitle.ifBlank { webView.currentUrl },
            url = webView.currentUrl,
            favicon = webView.currentFavicon
        )
    }

    fun getAllTabInfo(): List<TabInfo> {
        return _tabOrder.value.mapNotNull { tabId -> getTabInfo(tabId) }
    }

    fun getTabCount(): Int = _tabs.value.size

    fun saveAllState(): Map<String, Bundle> {
        val states = mutableMapOf<String, Bundle>()
        _tabs.value.forEach { (tabId, webView) ->
            val bundle = Bundle()
            webView.saveState(bundle)
            states[tabId] = bundle
        }
        return states
    }

    fun restoreAllState(states: Map<String, Bundle>) {
        tabStateBundles.clear()
        tabStateBundles.putAll(states)
    }

    fun destroy() {
        _tabs.value.values.forEach { it.destroy() }
        _tabs.value = emptyMap()
        _tabOrder.value = emptyList()
        _activeTabId.value = null
        tabStateBundles.clear()
    }

    companion object {
        private const val MAX_TABS = 50

        fun isUrl(searchQuery: String): Boolean {
            return searchQuery.startsWith("http://") ||
                    searchQuery.startsWith("https://") ||
                    (searchQuery.contains(".") && !searchQuery.contains(" "))
        }

        fun createSearchUrl(query: String, engine: SearchEngine = SearchEngine.GOOGLE): String {
            return when (engine) {
                SearchEngine.GOOGLE -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                SearchEngine.BING -> "https://www.bing.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                SearchEngine.YAHOO -> "https://search.yahoo.com/search?p=${java.net.URLEncoder.encode(query, "UTF-8")}"
            }
        }
    }

    enum class SearchEngine(val displayName: String) {
        GOOGLE("Google"),
        BING("Bing"),
        DUCKDUCKGO("DuckDuckGo"),
        YAHOO("Yahoo")
    }
}
