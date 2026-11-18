package dev.noirload.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.noirload.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object UpdateInstaller {
    data class Progress(val downloadedBytes: Long, val totalBytes: Long?) {
        val fraction get() = totalBytes?.takeIf { it > 0L }
            ?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
    }

    suspend fun download(context: Context, update: UpdateChecker.UpdateInfo, onProgress: (Progress) -> Unit): File = withContext(Dispatchers.IO) {
        val url = update.downloadUrl ?: error("No APK is attached to this release.")
        val parsed = URL(url)
        require(parsed.protocol.equals("https", true)) { "The update URL is not secure." }
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val output = File(directory, safeName(update.assetName ?: "Veiload-${update.latestVersion}.apk"))
        val temporary = File(directory, "${output.name}.part").apply { delete() }
        val connection = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.android.package-archive,application/octet-stream")
            setRequestProperty("User-Agent", "Veiload-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode !in 200..299) error("APK download failed (HTTP ${connection.responseCode}).")
            val total = update.assetSize ?: connection.contentLengthLong.takeIf { it > 0L }
            var downloaded = 0L
            var lastReport = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { file ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        file.write(buffer, 0, count)
                        downloaded += count
                        if (System.currentTimeMillis() - lastReport >= 100L) {
                            withContext(Dispatchers.Main.immediate) { onProgress(Progress(downloaded, total)) }
                            lastReport = System.currentTimeMillis()
                        }
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) { onProgress(Progress(downloaded, total)) }
            update.sha256?.let { expected -> check(sha256(temporary).equals(expected, true)) { "APK integrity check failed." } }
            output.delete()
            check(temporary.renameTo(output)) { "Could not save the APK." }
            output
        } catch (error: Exception) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun canInstall(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    fun permissionIntent(context: Context) = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
        .let { if (it.endsWith(".apk", true)) it else "$it.apk" }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
