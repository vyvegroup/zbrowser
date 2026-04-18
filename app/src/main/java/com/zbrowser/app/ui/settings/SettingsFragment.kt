package com.zbrowser.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zbrowser.app.R
import com.zbrowser.app.ZBrowserApp
import com.zbrowser.app.web.WebViewManager
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // Search engine preference
        findPreference<Preference>("search_engine")?.setOnPreferenceClickListener {
            showSearchEngineDialog()
            true
        }

        // Clear browsing data
        findPreference<Preference>("clear_browsing_data")?.setOnPreferenceClickListener {
            showClearDataDialog()
            true
        }

        // About
        findPreference<Preference>("about_version")?.summary = getVersionName()
    }

    private fun showSearchEngineDialog() {
        val engines = WebViewManager.SearchEngine.values()
        val names = engines.map { it.displayName }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_search_engine)
            .setItems(names) { _, which ->
                // Save selected search engine to preferences
                val prefs = requireActivity().getSharedPreferences("zbrowser_prefs", 0)
                prefs.edit().putString("search_engine", engines[which].name).apply()
            }
            .show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_clear_browsing_data)
            .setMessage(R.string.dialog_confirm_clear_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                clearBrowsingData()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun clearBrowsingData() {
        lifecycleScope.launch {
            val app = requireActivity().application as ZBrowserApp
            app.database.historyDao().deleteAll()
            app.database.bookmarkDao().deleteAll()

            // Clear WebView data
            android.webkit.WebView(requireContext()).apply {
                clearCache(true)
                clearHistory()
                clearFormData()
            }
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
    }

    private fun getVersionName(): String {
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
