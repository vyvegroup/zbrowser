package com.zbrowser.app.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitHubStorageManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("zbrowser_github_storage", Context.MODE_PRIVATE)
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean("github_storage_enabled", false)
        set(value) = prefs.edit().putBoolean("github_storage_enabled", value).apply()

    var personalAccessToken: String
        get() = prefs.getString("github_pat", "") ?: ""
        set(value) = prefs.edit().putString("github_pat", value).apply()

    var repositoryName: String
        get() = prefs.getString("github_repo", "zbrowser-storage") ?: "zbrowser-storage"
        set(value) = prefs.edit().putString("github_repo", value).apply()

    var githubUsername: String
        get() = prefs.getString("github_username", "") ?: ""
        set(value) = prefs.edit().putString("github_username", value).apply()

    var autoUpload: Boolean
        get() = prefs.getBoolean("github_auto_upload", true)
        set(value) = prefs.edit().putBoolean("github_auto_upload", value).apply()

    val isConfigured: Boolean
        get() = personalAccessToken.isNotEmpty() && githubUsername.isNotEmpty()

    suspend fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        subFolder: String = "downloads"
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        try {
            ensureRepositoryExists()
            val content = readStream(inputStream)
            val base64Content = Base64.encodeToString(content, Base64.NO_WRAP)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val datePath = dateFormat.format(Date())
            val filePath = "$subFolder/$datePath/$fileName"
            val existingSha = getFileSha(filePath)
            val commitUrl = "https://api.github.com/repos/${githubUsername}/${repositoryName}/contents/$filePath"
            val jsonBody = JSONObject().apply {
                put("message", "Upload $fileName via ZBrowser")
                put("content", base64Content)
                if (existingSha != null) put("sha", existingSha)
            }
            val response = makeGitHubRequest("PUT", commitUrl, jsonBody.toString())
            response?.optJSONObject("content")?.optString("download_url")
        } catch (e: Exception) { null }
    }

    suspend fun uploadFromUrl(
        fileUrl: String,
        fileName: String,
        userAgent: String = "",
        cookies: String = "",
        subFolder: String = "downloads"
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        try {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", userAgent.ifEmpty { "ZBrowser/1.0" })
                if (cookies.isNotEmpty()) setRequestProperty("Cookie", cookies)
                connectTimeout = 30000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            val inputStream = connection.inputStream
            val result = uploadFile(inputStream, fileName, subFolder)
            inputStream.close()
            connection.disconnect()
            result
        } catch (e: Exception) { null }
    }

    suspend fun verifyToken(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val response = makeGitHubRequest("GET", "https://api.github.com/user")
            if (response != null) {
                val username = response.optString("login")
                if (username.isNotEmpty()) {
                    githubUsername = username
                    Pair(true, username)
                } else Pair(false, "Invalid token response")
            } else Pair(false, "Failed to authenticate")
        } catch (e: Exception) { Pair(false, e.message) }
    }

    private suspend fun ensureRepositoryExists() = withContext(Dispatchers.IO) {
        try {
            val checkUrl = "https://api.github.com/repos/${githubUsername}/${repositoryName}"
            val response = makeGitHubRequest("GET", checkUrl)
            if (response == null) {
                val createUrl = "https://api.github.com/user/repos"
                val jsonBody = JSONObject().apply {
                    put("name", repositoryName)
                    put("private", true)
                    put("description", "ZBrowser cloud storage")
                    put("auto_init", true)
                }
                makeGitHubRequest("POST", createUrl, jsonBody.toString())
            }
        } catch (_: Exception) {}
    }

    private suspend fun getFileSha(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${githubUsername}/${repositoryName}/contents/$filePath"
            val response = makeGitHubRequest("GET", url)
            response?.optString("sha")
        } catch (_: Exception) { null }
    }

    private fun makeGitHubRequest(method: String, urlStr: String, body: String? = null): JSONObject? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = method
                setRequestProperty("Authorization", "token $personalAccessToken")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { os -> os.write(body.toByteArray(Charsets.UTF_8)) }
            }
            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                if (response.isNotEmpty() && response.startsWith("{")) JSONObject(response) else null
            } else null
        } catch (e: Exception) { null }
    }

    private fun readStream(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(16384)
        var n: Int
        while (inputStream.read(data, 0, data.size).also { n = it } != -1) buffer.write(data, 0, n)
        return buffer.toByteArray()
    }

    companion object {
        @Volatile
        private var INSTANCE: GitHubStorageManager? = null
        fun getInstance(context: Context): GitHubStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GitHubStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
