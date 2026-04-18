package com.zbrowser.app.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbrowser.app.ZBrowserApp
import com.zbrowser.app.data.Bookmark
import com.zbrowser.app.data.HistoryEntry
import com.zbrowser.app.web.WebViewManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZBrowserApp
    private val bookmarkDao = app.database.bookmarkDao()
    private val historyDao = app.database.historyDao()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    private val _tabCount = MutableStateFlow(0)
    val tabCount: StateFlow<Int> = _tabCount

    fun updateUrl(url: String) {
        _currentUrl.value = url
        checkBookmark(url)
    }

    fun updateTitle(title: String) {
        _currentTitle.value = title
    }

    fun updateProgress(progress: Int) {
        _loadingProgress.value = progress
        _isLoading.value = progress < 100
    }

    fun updateTabCount(count: Int) {
        _tabCount.value = count
    }

    private fun checkBookmark(url: String) {
        viewModelScope.launch {
            val bookmark = bookmarkDao.getBookmarkByUrl(url)
            _isBookmarked.value = bookmark != null
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            val existing = bookmarkDao.getBookmarkByUrl(url)
            if (existing == null) {
                bookmarkDao.insert(Bookmark(title = title, url = url))
                _isBookmarked.value = true
            }
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            val bookmark = bookmarkDao.getBookmarkByUrl(url)
            bookmark?.let {
                bookmarkDao.delete(it)
                _isBookmarked.value = false
            }
        }
    }

    fun toggleBookmark(title: String, url: String) {
        if (_isBookmarked.value) {
            removeBookmark(url)
        } else {
            addBookmark(title, url)
        }
    }

    fun addToHistory(title: String, url: String) {
        viewModelScope.launch {
            historyDao.insert(HistoryEntry(title = title, url = url))
        }
    }

    fun formatUrlForDisplay(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return url
            // Remove www. prefix for cleaner display
            var display = host.removePrefix("www.")
            val path = uri.path
            if (path != null && path != "/" && path.isNotEmpty()) {
                display += path
            }
            display
        } catch (e: Exception) {
            url
        }
    }
}
