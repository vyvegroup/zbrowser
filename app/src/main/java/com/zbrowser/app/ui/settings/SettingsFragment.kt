package com.zbrowser.app.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.zbrowser.app.R
import com.zbrowser.app.storage.GitHubStorageManager
import com.zbrowser.app.web.WebViewManager
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    private val githubStorage by lazy { GitHubStorageManager.getInstance(requireContext()) }

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

        // GitHub Storage section
        setupGitHubStoragePreferences()

        // About
        findPreference<Preference>("about_version")?.summary = getVersionName()
    }

    private fun setupGitHubStoragePreferences() {
        // GitHub PAT input
        findPreference<Preference>("github_pat")?.let { pref ->
            pref.summary = if (githubStorage.personalAccessToken.isNotEmpty()) {
                "Token: ${githubStorage.personalAccessToken.take(4)}${"*".repeat(12)}"
            } else {
                "Not configured"
            }
            pref.setOnPreferenceClickListener {
                showGitHubPATDialog()
                true
            }
        }

        // GitHub Storage toggle
        findPreference<SwitchPreferenceCompat>("github_storage_enabled")?.let { pref ->
            pref.isChecked = githubStorage.isEnabled
            pref.setOnPreferenceChangeListener { _, newValue ->
                githubStorage.isEnabled = newValue as Boolean
                true
            }
        }

        // Auto-upload toggle
        findPreference<SwitchPreferenceCompat>("github_auto_upload")?.let { pref ->
            pref.isChecked = githubStorage.autoUpload
            pref.setOnPreferenceChangeListener { _, newValue ->
                githubStorage.autoUpload = newValue as Boolean
                true
            }
        }

        // Repository name
        findPreference<Preference>("github_repo_name")?.let { pref ->
            pref.summary = githubStorage.repositoryName
            pref.setOnPreferenceClickListener {
                showRepoNameDialog()
                true
            }
        }

        // Test connection
        findPreference<Preference>("github_test_connection")?.setOnPreferenceClickListener {
            testGitHubConnection()
            true
        }
    }

    private fun showGitHubPATDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val inputLayout = TextInputLayout(context).apply {
            hint = "GitHub Personal Access Token"
            isPasswordVisibleToggleEnabled = false
        }

        val editText = TextInputEditText(inputLayout.context).apply {
            hint = "ghp_xxxxxxxxxxxx"
            setText(githubStorage.personalAccessToken)
            setSingleLine()
        }

        inputLayout.addView(editText)
        layout.addView(inputLayout)

        val helpText = android.widget.TextView(context).apply {
            text = "Create a token at: Settings > Developer settings > Personal access tokens\nRequired scopes: repo (full control)"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setPadding(0, 16, 0, 0)
        }
        layout.addView(helpText)

        MaterialAlertDialogBuilder(context)
            .setTitle("GitHub Personal Access Token")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val token = editText.text.toString().trim()
                githubStorage.personalAccessToken = token
                findPreference<Preference>("github_pat")?.summary =
                    if (token.isNotEmpty()) "Token: ${token.take(4)}${"*".repeat(12)}"
                    else "Not configured"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRepoNameDialog() {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = "Repository name"
            setText(githubStorage.repositoryName)
            setSingleLine()
            setPadding(48, 24, 48, 24)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("GitHub Repository Name")
            .setMessage("A private repository will be created (if it doesn't exist) to store your files.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    githubStorage.repositoryName = name
                    findPreference<Preference>("github_repo_name")?.summary = name
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testGitHubConnection() {
        if (githubStorage.personalAccessToken.isEmpty()) {
            Snackbar.make(requireView(), "Please enter a GitHub PAT first", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val (success, message) = githubStorage.verifyToken()
            if (success) {
                Snackbar.make(
                    requireView(),
                    "Connected as: $message (repo: ${githubStorage.repositoryName})",
                    Snackbar.LENGTH_LONG
                ).show()
                findPreference<Preference>("github_pat")?.summary =
                    "Token: ${githubStorage.personalAccessToken.take(4)}${"*".repeat(12)}"
            } else {
                Snackbar.make(
                    requireView(),
                    "Connection failed: ${message ?: "Unknown error"}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showSearchEngineDialog() {
        val engines = WebViewManager.SearchEngine.values()
        val names = engines.map { it.displayName }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_search_engine)
            .setItems(names) { _, which ->
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
            val app = requireActivity().application as com.zbrowser.app.ZBrowserApp
            app.database.historyDao().deleteAll()
            app.database.bookmarkDao().deleteAll()

            // Clear WebView data safely
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebStorage.getInstance().deleteAllData()
            } catch (e: Exception) {
                // Ignore
            }
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
