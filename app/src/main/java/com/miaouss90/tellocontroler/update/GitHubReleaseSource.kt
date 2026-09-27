package com.miaouss90.tellocontroler.update

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Blocking GitHub Releases client. Call from Dispatchers.IO. The repository is public: no token. */
class GitHubReleaseSource(private val repo: String = REPO) {
    companion object {
        const val REPO = "Miaouss90/Tello-Controler"
        const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
        private const val TIMEOUT_MS = 15_000
    }

    fun latest(): ReleaseInfo? {
        val connection = open("https://api.github.com/repos/$repo/releases/latest").apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            check(code == HttpURLConnection.HTTP_OK) { "GitHub API HTTP $code" }
            return ReleaseInfo.fromGitHubJson(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** Opens the APK download (GitHub redirects to its CDN; HttpURLConnection follows https→https). */
    fun <T> download(url: String, block: (InputStream) -> T): T {
        val connection = open(url)
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Download HTTP ${connection.responseCode}" }
            return connection.inputStream.use(block)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Tello-Controler-Android")
    }
}
