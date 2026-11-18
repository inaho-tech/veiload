package dev.noirload.app.update

import android.util.Log
import dev.noirload.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val UPDATE_API_URL = "https://api.github.com/repos/inaho-tech/veiload/releases/latest"
    const val RELEASES_URL = "https://github.com/inaho-tech/veiload/releases"

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String?,
        val releasePageUrl: String,
        val assetName: String?,
        val assetSize: Long?,
        val sha256: String?
    )

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(UPDATE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "Veiload-Android/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latest = semanticVersion(release.optString("tag_name"))
                ?: semanticVersion(release.optString("name"))
                ?: return@withContext null
            if (compareVersions(latest, BuildConfig.VERSION_NAME) <= 0) return@withContext null
            val assets = release.optJSONArray("assets")
            val apkAssets = buildList {
                if (assets != null) repeat(assets.length()) { index ->
                    assets.optJSONObject(index)?.takeIf {
                        it.optString("name").endsWith(".apk", ignoreCase = true) &&
                            it.optString("state", "uploaded") == "uploaded"
                    }?.let(::add)
                }
            }
            val apk = apkAssets.maxByOrNull { asset ->
                val name = asset.optString("name").lowercase()
                (if ("release" in name) 4 else 0) + (if ("debug" !in name) 2 else 0) + (if ("universal" in name) 1 else 0)
            }
            val digest = apk?.optString("digest")?.takeIf { it.startsWith("sha256:", true) }
                ?.substringAfter(':')?.lowercase()
            UpdateInfo(
                latestVersion = latest,
                downloadUrl = apk?.optString("browser_download_url")?.takeIf(String::isNotBlank),
                releasePageUrl = release.optString("html_url").ifBlank { RELEASES_URL },
                assetName = apk?.optString("name")?.takeIf(String::isNotBlank),
                assetSize = apk?.optLong("size")?.takeIf { it > 0L },
                sha256 = digest
            )
        } catch (error: Exception) {
            Log.w(TAG, "GitHub release check failed", error)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun semanticVersion(value: String): String? = Regex("""\d+(?:\.\d+){1,3}""").find(value)?.value

    private fun compareVersions(first: String, second: String): Int {
        val a = first.split('.').map { it.toIntOrNull() ?: 0 }
        val b = second.split('.').map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(a.size, b.size)) { index ->
            val difference = a.getOrElse(index) { 0 } - b.getOrElse(index) { 0 }
            if (difference != 0) return difference
        }
        return 0
    }
}
