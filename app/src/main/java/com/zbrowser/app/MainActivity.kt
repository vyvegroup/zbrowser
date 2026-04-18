package com.zbrowser.app

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zbrowser.app.web.TabWebView
import com.zbrowser.app.web.WebViewManager

class MainActivity : AppCompatActivity() {

    val webViewManager by lazy { WebViewManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val activeTab = webViewManager.getActiveTab()
                if (activeTab != null && activeTab.canGoBack()) {
                    activeTab.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString ?: return
                webViewManager.createTab(url)
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                        webViewManager.createTab(sharedText)
                    } else {
                        webViewManager.createTab(WebViewManager.createSearchUrl(sharedText))
                    }
                }
            }
        }
    }

    fun handleDownload(request: TabWebView.DownloadRequest) {
        try {
            val dmRequest = DownloadManager.Request(Uri.parse(request.url)).apply {
                val fileName = android.webkit.URLUtil.guessFileName(
                    request.url, request.contentDisposition, request.mimetype
                )
                setTitle(fileName)
                setDescription("Downloading $fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                addRequestHeader("User-Agent", request.userAgent)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(request.url))
            }

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(dmRequest)
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.menu_share)))
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewManager.destroy()
    }
}
