package dev.noirload.app.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.noirload.app.MainActivity
import dev.noirload.app.R
import dev.noirload.app.tr
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {
    private data class TaskRuntime(
        val id: String,
        @Volatile var cancelRequested: Boolean = false,
        @Volatile var directConnection: HttpURLConnection? = null,
        @Volatile var adaptiveProcess: Process? = null,
        @Volatile var activeOutput: File? = null,
        @Volatile var completedOutput: File? = null,
        @Volatile var lastNotificationAt: Long = 0L,
        @Volatile var wakeLock: PowerManager.WakeLock? = null
    ) {
        val processId = "noir-download-$id"
        val outputHints: MutableSet<File> = ConcurrentHashMap.newKeySet()
    }

    private val tasks = ConcurrentHashMap<String, TaskRuntime>()

    override fun onCreate() {
        super.onCreate()
        deleteSharedPreferences("download_method_stats")
        createChannel()
        clearLegacyTaskNotifications()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> intent.getStringExtra(EXTRA_TASK_ID)?.let(::cancelDownload)
            ACTION_DOWNLOAD -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val fallbackUrls = intent.getStringArrayListExtra(EXTRA_FALLBACK_URLS).orEmpty()
                val cookie = intent.getStringExtra(EXTRA_COOKIE).orEmpty()
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()
                val preferDirect = intent.getBooleanExtra(EXTRA_PREFER_DIRECT, false)
                val refererUrl = intent.getStringExtra(EXTRA_REFERER_URL).orEmpty().ifBlank { url }
                if (url.isNotBlank() && taskId.isNotBlank()) {
                    val task = TaskRuntime(taskId)
                    if (tasks.putIfAbsent(taskId, task) == null) {
                        begin(task, url, title, fallbackUrls, cookie, userAgent, preferDirect, refererUrl)
                    }
                }
            }
        }
        // Completed download intents must never be delivered again when the idle
        // service is recreated. New downloads always arrive with a fresh task id.
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val message = tr("Androidのバックグラウンド実行時間上限に達しました", "Android's background execution time limit was reached")
        tasks.values.toList().forEach { task ->
            cancelRuntime(task)
            DownloadsStore.update(ActiveDownload("", tr("ダウンロードを停止しました", "Download stopped"), running = false, error = message, detail = message, id = task.id))
        }
        notificationManager().notify(NOTIFICATION_ID, errorNotification(message))
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    private fun begin(
        task: TaskRuntime,
        url: String,
        pageTitle: String,
        fallbackUrls: List<String>,
        cookie: String,
        userAgent: String,
        preferDirect: Boolean,
        refererUrl: String
    ) {
        task.wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoirLoad:Download:${task.id}")
            .apply { acquire(6 * 60 * 60 * 1000L) }
        startForeground(NOTIFICATION_ID, summaryNotification())
        thread(name = "noir-download-${task.id.take(8)}") {
            try {
                var downloaded = false
                if (preferDirect) {
                    downloaded = runCatching {
                        val clean = url.substringBefore('?').substringBefore('#').lowercase()
                        if (clean.endsWith(".m3u8") || clean.endsWith(".mpd")) {
                            downloadAdaptive(task, url, refererUrl, pageTitle, cookie, userAgent)
                        } else {
                            downloadDirectFile(task, url, refererUrl, pageTitle, cookie, userAgent)
                        }
                    }.getOrDefault(false)
                    ensureNotCancelled(task)
                    if (!downloaded) cleanupTaskOutputs(task)
                }
                if (!downloaded) {
                var ytDlpResult = runCatching { downloadWithYtDlp(task, url, cookie, userAgent, refererUrl) }
                if (ytDlpResult.isFailure && cookie.isNotBlank() && !task.cancelRequested) {
                    runCatching { YoutubeDL.getInstance().destroyProcessById(task.processId) }
                    ytDlpResult = runCatching { downloadWithYtDlp(task, url, "", userAgent, refererUrl) }
                }
                ensureNotCancelled(task)
                if (ytDlpResult.isFailure) {
                    DownloadsStore.update(ActiveDownload(url, pageTitle.ifBlank { tr("動画", "Video") }, detail = tr("別の方法を試しています", "Trying another method"), id = task.id))
                    if (!downloadFallback(task, refererUrl, pageTitle, fallbackUrls + url, cookie, userAgent)) {
                        throw ytDlpResult.exceptionOrNull() ?: IllegalStateException(tr("動画を取得できませんでした", "Could not retrieve the video"))
                    }
                }
                }
                ensureNotCancelled(task)
                val completedFile = resolveCompletedOutput(task)?.let { finalizeTaskOutput(task, it) }
                if (completedFile == null || !isLibraryMedia(completedFile)) {
                    cleanupTaskOutputs(task)
                    throw IllegalStateException(tr("保存された動画ファイルを確認できませんでした", "Could not verify the saved video file"))
                }
                DownloadsStore.scan(applicationContext)
                DownloadsStore.update(ActiveDownload(url, pageTitle.ifBlank { tr("保存しました", "Saved") }, 100f, detail = "Downloads/Veiload", running = false, id = task.id))
                notificationManager().cancel(notificationId(task.id))
                notificationManager().notify(NOTIFICATION_ID, summaryNotification())
                toast(tr("ダウンロードが完了しました", "Download complete"))
            } catch (_: DownloadCancelled) {
                cleanupTaskOutputs(task)
                DownloadsStore.remove(task.id)
            } catch (error: Throwable) {
                cleanupTaskOutputs(task)
                val message = error.message?.lineSequence()?.lastOrNull()?.take(140) ?: tr("動画を取得できませんでした", "Could not retrieve the video")
                DownloadsStore.update(ActiveDownload(url, pageTitle.ifBlank { tr("ダウンロード失敗", "Download failed") }, detail = message, running = false, error = message, id = task.id))
                notificationManager().cancel(notificationId(task.id))
                notificationManager().notify(NOTIFICATION_ID, summaryNotification())
                toast(tr("ダウンロードに失敗しました", "Download failed"))
            } finally {
                task.wakeLock?.takeIf { it.isHeld }?.release()
                task.wakeLock = null
                tasks.remove(task.id)
                notificationManager().cancel(notificationId(task.id))
                if (tasks.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationManager().cancel(NOTIFICATION_ID)
                    // Keep the lightweight service instance ready. Stopping it here
                    // caused the first request after the final completion to be lost
                    // during Android's service teardown/recreation window.
                } else notificationManager().notify(NOTIFICATION_ID, summaryNotification())
            }
        }
    }

    private fun downloadWithYtDlp(task: TaskRuntime, url: String, cookie: String, userAgent: String, referer: String) {
        prepareEngine(task, url)
        val output = DownloadsStore.directory().absolutePath + "/%(title).180B [%(id)s].__noir_${task.id.take(8)}.%(ext)s"
        val cookieFile = createCookieFile(url, cookie)
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--playlist-items", "1")
            addOption("--newline")
            addOption("--no-mtime")
            addOption("--embed-metadata")
            addOption("--write-info-json")
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--concurrent-fragments", "8")
            addOption("--extractor-retries", "5")
            addOption("--file-access-retries", "3")
            addOption("--geo-bypass")
            addOption("--merge-output-format", "mp4")
            if (userAgent.isNotBlank()) addOption("--user-agent", userAgent)
            if (referer.isNotBlank()) addOption("--referer", referer)
            cookieFile?.let { addOption("--cookies", it.absolutePath) }
            val quickJs = File(applicationInfo.nativeLibraryDir, "libqjs.so")
            if (quickJs.exists()) addOption("--js-runtimes", "quickjs:${quickJs.absolutePath}")
            addOption("-f", "bv*[vcodec^=avc1][ext=mp4]+ba[ext=m4a]/bv*[vcodec^=avc1]+ba/b[ext=mp4]/bv*[ext=mp4]+ba[ext=m4a]/bv*+ba/b")
            addOption("-o", output)
        }
        try {
            YoutubeDL.getInstance().execute(request, task.processId) { progress, eta, line ->
                extractOutputFile(line)?.let { file ->
                    task.outputHints += file
                    task.completedOutput = file
                }
                val title = extractTitle(line)
                    ?: DownloadsStore.active.value.firstOrNull { it.id == task.id }?.title
                ?: tr("ダウンロード中", "Downloading")
                DownloadsStore.update(ActiveDownload(url, title, progress, eta, line.takeLast(120), id = task.id))
                notifyProgress(task, progress.toInt(), title)
            }
        } finally {
            cookieFile?.delete()
        }
    }

    private fun createCookieFile(url: String, cookieHeader: String): File? {
        if (cookieHeader.isBlank()) return null
        val uri = Uri.parse(url)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val secure = (uri.scheme == "https").toString().uppercase()
        val lines = cookieHeader.split(';').mapNotNull { part ->
            val pair = part.trim().split('=', limit = 2)
            if (pair.size != 2 || pair[0].isBlank()) null
            else listOf(".$host", "TRUE", "/", secure, "2147483647", pair[0], pair[1]).joinToString("\t")
        }
        if (lines.isEmpty()) return null
        return File(cacheDir, "yt-cookies-${System.nanoTime()}.txt").apply {
            writeText("# Netscape HTTP Cookie File\n${lines.joinToString("\n")}\n")
        }
    }

    private fun downloadFallback(task: TaskRuntime, pageUrl: String, pageTitle: String, candidates: List<String>, cookie: String, userAgent: String): Boolean {
        DownloadsStore.update(ActiveDownload(pageUrl, pageTitle.ifBlank { tr("動画", "Video") }, detail = tr("ページ内のメディアを探索中", "Searching for media on the page"), id = task.id))
        val discovered = discoverMediaUrls(task, pageUrl, cookie, userAgent)
        val sources = (candidates + discovered + pageUrl).asSequence()
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .filterNot(::isLikelyVideoAdUrl)
            .distinct().take(24).toList()
        for (source in sources) {
            ensureNotCancelled(task)
            val clean = source.substringBefore('?').substringBefore('#').lowercase()
            val adaptive = clean.endsWith(".m3u8") || clean.endsWith(".mpd")
            val success = runCatching {
                if (adaptive) downloadAdaptive(task, source, pageUrl, pageTitle, cookie, userAgent)
                else downloadDirectFile(task, source, pageUrl, pageTitle, cookie, userAgent)
            }.getOrDefault(false)
            ensureNotCancelled(task)
            if (success) return true
        }
        return false
    }

    private fun discoverMediaUrls(task: TaskRuntime, pageUrl: String, cookie: String, userAgent: String): List<String> {
        val connection = runCatching { URL(pageUrl).openConnection() as HttpURLConnection }.getOrNull()
            ?: return emptyList()
        task.directConnection = connection
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Referer", pageUrl)
            if (cookie.isNotBlank()) connection.setRequestProperty("Cookie", cookie)
            if (userAgent.isNotBlank()) connection.setRequestProperty("User-Agent", userAgent)
            if (connection.responseCode !in 200..299) return emptyList()
            val type = connection.contentType.orEmpty().lowercase()
            if (type.isNotBlank() && !type.contains("html") && !type.contains("json") && !type.contains("text")) {
                return emptyList()
            }

            val html = buildString {
                connection.inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(16 * 1024)
                    while (length < MAX_PAGE_CHARS) {
                        ensureNotCancelled(task)
                        val count = reader.read(buffer, 0, minOf(buffer.size, MAX_PAGE_CHARS - length))
                        if (count < 0) break
                        append(buffer, 0, count)
                    }
                }
            }
            val attributeUrls = MEDIA_ATTRIBUTE_REGEX.findAll(html).map { it.groupValues[1] }
            val embeddedUrls = MEDIA_URL_REGEX.findAll(html).map { it.value }
            (attributeUrls + embeddedUrls)
                .mapNotNull { normalizeDiscoveredUrl(pageUrl, it) }
                .distinct()
                .take(20)
                .toList()
        } catch (_: DownloadCancelled) {
            throw DownloadCancelled()
        } catch (_: Throwable) {
            emptyList()
        } finally {
            connection.disconnect()
            if (task.directConnection === connection) task.directConnection = null
        }
    }

    private fun normalizeDiscoveredUrl(pageUrl: String, raw: String): String? = runCatching {
        val decoded = raw.trim()
            .replace("\\/", "/")
            .replace("\\u0026", "&", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
        URL(URL(pageUrl), decoded).toString()
    }.getOrNull()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

    private fun downloadDirectFile(task: TaskRuntime, source: String, pageUrl: String, pageTitle: String, cookie: String, userAgent: String): Boolean {
        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Referer", pageUrl)
            if (cookie.isNotBlank()) setRequestProperty("Cookie", cookie)
            if (userAgent.isNotBlank()) setRequestProperty("User-Agent", userAgent)
        }
        task.directConnection = connection
        try {
            if (connection.responseCode !in 200..299) return false
            val contentType = connection.contentType.orEmpty().substringBefore(';').lowercase()
            if (contentType.contains("text/html") || contentType.contains("mpegurl") || contentType.contains("dash+xml")) return false
            val extension = source.substringBefore('?').substringBefore('#').substringAfterLast('.', "")
                .lowercase().takeIf { it in DIRECT_EXTENSIONS }
                ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.takeIf { it.isNotBlank() }
                ?: "mp4"
            val output = uniqueOutput(pageTitle, extension)
            val partial = File(output.parentFile, "${output.name}.part")
            task.activeOutput = partial
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { outputStream ->
                    val buffer = ByteArray(128 * 1024)
                    var copied = 0L
                    while (true) {
                        ensureNotCancelled(task)
                        val count = input.read(buffer)
                        if (count < 0) break
                        outputStream.write(buffer, 0, count)
                        copied += count
                        val progress = if (total > 0) copied * 100f / total else 0f
                        DownloadsStore.update(ActiveDownload(pageUrl, pageTitle, progress, detail = tr("直接動画を取得中", "Downloading video directly"), id = task.id))
                        notifyProgress(task, progress.toInt(), pageTitle.ifBlank { tr("動画", "Video") })
                    }
                }
            }
            if (!partial.renameTo(output)) {
                partial.copyTo(output, overwrite = true)
                partial.delete()
            }
            if (output.exists() && output.length() > 0) writeSourceInfo(output, pageUrl, pageTitle)
            if (output.exists() && output.length() > 0) {
                task.completedOutput = output
                task.outputHints += output
            }
            task.activeOutput = null
            return output.exists() && output.length() > 0
        } finally {
            connection.disconnect()
            if (task.directConnection === connection) task.directConnection = null
            if (task.cancelRequested) task.activeOutput?.delete()
        }
    }

    private fun downloadAdaptive(task: TaskRuntime, source: String, pageUrl: String, pageTitle: String, cookie: String, userAgent: String): Boolean {
        val binary = File(applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (!binary.exists()) return false
        val output = uniqueOutput(pageTitle, "mp4")
        val partial = File(output.parentFile, ".${output.name}.${task.id}.part")
        task.activeOutput = partial
        val headers = buildString {
            append("Referer: $pageUrl\r\n")
            if (cookie.isNotBlank()) append("Cookie: $cookie\r\n")
        }
        val command = mutableListOf(binary.absolutePath, "-y")
        if (userAgent.isNotBlank()) command += listOf("-user_agent", userAgent)
        command += listOf("-headers", headers, "-i", source, "-c", "copy", "-f", "mp4", partial.absolutePath)
        DownloadsStore.update(ActiveDownload(pageUrl, pageTitle, detail = tr("ストリームを保存中", "Saving stream"), id = task.id))
        notifyProgress(task, 0, pageTitle.ifBlank { tr("動画", "Video") }, force = true)
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        task.adaptiveProcess = process
        var durationSeconds = 0.0
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                Regex("""Duration:\s*(\d{2}:\d{2}:\d{2}(?:\.\d+)?)""").find(line)?.groupValues?.getOrNull(1)
                    ?.let(::parseFfmpegTime)?.takeIf { it > 0 }?.let { durationSeconds = it }
                val currentSeconds = Regex("""time=(\d{2}:\d{2}:\d{2}(?:\.\d+)?)""").find(line)
                    ?.groupValues?.getOrNull(1)?.let(::parseFfmpegTime) ?: 0.0
                if (durationSeconds > 0 && currentSeconds > 0) {
                    val progress = (currentSeconds * 100.0 / durationSeconds).toFloat().coerceIn(0f, 99f)
                    DownloadsStore.update(ActiveDownload(pageUrl, pageTitle, progress, detail = tr("ストリームを保存中", "Saving stream"), id = task.id))
                    notifyProgress(task, progress.toInt(), pageTitle.ifBlank { tr("動画", "Video") })
                }
            }
        }
        var result = process.waitFor() == 0 && partial.exists() && partial.length() > 0
        if (result) {
            output.delete()
            if (!partial.renameTo(output)) {
                partial.copyTo(output, overwrite = true)
                partial.delete()
            }
            result = output.exists() && output.length() > 0
            if (result) {
                writeSourceInfo(output, pageUrl, pageTitle)
                task.completedOutput = output
                task.outputHints += output
            }
        }
        if (task.adaptiveProcess === process) task.adaptiveProcess = null
        if (!result || task.cancelRequested) {
            partial.delete()
            output.delete()
        }
        task.activeOutput = null
        ensureNotCancelled(task)
        return result
    }

    private fun writeSourceInfo(output: File, pageUrl: String, title: String) {
        runCatching {
            val sidecar = File(output.parentFile, "${output.nameWithoutExtension}.info.json")
            val domain = Uri.parse(pageUrl).host.orEmpty().lowercase().removePrefix("www.")
            sidecar.writeText(
                JSONObject()
                    .put("title", title)
                    .put("webpage_url", pageUrl)
                    .put("source_url", pageUrl)
                    .put("source_domain", domain)
                    .toString()
            )
        }
    }

    private fun uniqueOutput(title: String, extension: String): File = synchronized(OUTPUT_LOCK) {
        val safe = title.ifBlank { "video" }.replace(Regex("[\\/:*?\"<>|]"), "_").trim().take(90).ifBlank { "video" }
        var output = File(DownloadsStore.directory(), "$safe.$extension")
        var suffix = 2
        while (output.exists()) {
            output = File(DownloadsStore.directory(), "$safe ($suffix).$extension")
            suffix++
        }
        output.createNewFile()
        output
    }

    private fun prepareEngine(task: TaskRuntime, url: String) = synchronized(ENGINE_LOCK) {
        YoutubeDL.getInstance().init(applicationContext)
        FFmpeg.getInstance().init(applicationContext)
        val preferences = getSharedPreferences("download_engine", MODE_PRIVATE)
        val lastUpdate = preferences.getLong("last_update", 0L)
        if (System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL_MS) {
            DownloadsStore.update(ActiveDownload(url, tr("ダウンロードエンジンを更新中", "Updating download engine"), detail = tr("最新の抽出器を取得しています", "Getting the latest extractor"), id = task.id))
            runCatching { YoutubeDL.getInstance().updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.NIGHTLY) }
                .onSuccess { preferences.edit().putLong("last_update", System.currentTimeMillis()).apply() }
        }
    }

    private fun extractTitle(line: String): String? {
        val destination = Regex("""(?:Destination:|Merging formats into) "?(.+?)(?:"|$)""")
            .find(line)?.groupValues?.getOrNull(1)
        return destination?.substringAfterLast('/')?.substringAfterLast('\\')?.substringBeforeLast('.')
    }

    private fun extractOutputFile(line: String): File? {
        val raw = Regex("""(?:Destination:|Merging formats into) "?(.+?)(?:"|$)""")
            .find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return File(raw).let { if (it.isAbsolute) it else File(DownloadsStore.directory(), raw) }
    }

    private fun parseFfmpegTime(value: String): Double {
        val parts = value.split(':')
        if (parts.size != 3) return 0.0
        return (parts[0].toDoubleOrNull() ?: 0.0) * 3600.0 +
            (parts[1].toDoubleOrNull() ?: 0.0) * 60.0 +
            (parts[2].toDoubleOrNull() ?: 0.0)
    }

    private fun resolveCompletedOutput(task: TaskRuntime): File? =
        (listOfNotNull(task.completedOutput) + task.outputHints)
            .filter(File::exists)
            .maxByOrNull(File::lastModified)

    private fun finalizeTaskOutput(task: TaskRuntime, source: File): File = synchronized(OUTPUT_LOCK) {
        val marker = ".__noir_${task.id.take(8)}"
        if (!source.nameWithoutExtension.endsWith(marker)) return@synchronized source
        val base = source.nameWithoutExtension.removeSuffix(marker)
        var destination = File(source.parentFile, "$base.${source.extension}")
        var suffix = 2
        while (destination.exists()) {
            destination = File(source.parentFile, "$base ($suffix).${source.extension}")
            suffix++
        }
        val oldInfo = File(source.parentFile, "${source.nameWithoutExtension}.info.json")
        val newInfo = File(source.parentFile, "${destination.nameWithoutExtension}.info.json")
        val moved = source.renameTo(destination) || runCatching {
            source.copyTo(destination, overwrite = false)
            source.delete()
            true
        }.getOrDefault(false)
        if (!moved) return@synchronized source
        if (oldInfo.exists()) runCatching {
            if (!oldInfo.renameTo(newInfo)) {
                oldInfo.copyTo(newInfo, overwrite = false)
                oldInfo.delete()
            }
        }
        task.completedOutput = destination
        task.outputHints += destination
        destination
    }

    private fun isLibraryMedia(file: File): Boolean {
        val directory = DownloadsStore.directory().canonicalFile
        val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return false
        val insideDirectory = candidate.parentFile == directory
        return insideDirectory && candidate.isFile && candidate.length() > 0 &&
            candidate.extension.lowercase() in LIBRARY_EXTENSIONS
    }

    private fun cleanupTaskOutputs(task: TaskRuntime) {
        val directory = DownloadsStore.directory().canonicalFile
        (task.outputHints + listOfNotNull(task.activeOutput, task.completedOutput)).forEach { file ->
            val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return@forEach
            if (candidate.parentFile == directory) {
                runCatching { candidate.delete() }
                runCatching { File(directory, "${candidate.nameWithoutExtension}.info.json").delete() }
            }
        }
    }

    private fun cancelDownload(taskId: String) {
        val task = tasks[taskId] ?: return
        cancelRuntime(task)
        DownloadsStore.remove(taskId)
        notificationManager().cancel(notificationId(taskId))
        toast(tr("ダウンロードを中止しました", "Download canceled"))
    }

    private fun cancelRuntime(task: TaskRuntime) {
        task.cancelRequested = true
        runCatching { YoutubeDL.getInstance().destroyProcessById(task.processId) }
        runCatching { task.directConnection?.disconnect() }
        runCatching { task.adaptiveProcess?.destroy() }
        runCatching { task.activeOutput?.delete() }
        cleanupTaskOutputs(task)
    }

    private fun ensureNotCancelled(task: TaskRuntime) {
        if (task.cancelRequested) throw DownloadCancelled()
    }

    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    private fun notification(progress: Int, title: String, ongoing: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(if (ongoing) tr("$progress% · ダウンロード中", "$progress% · Downloading") else tr("Downloads/Veiload に保存しました", "Saved to Downloads/Veiload"))
            .setProgress(100, progress.coerceIn(0, 100), progress == 0 && ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun notifyProgress(task: TaskRuntime, progress: Int, title: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && progress < 100 && now - task.lastNotificationAt < 600L) return
        task.lastNotificationAt = now
        // A single foreground notification represents all active downloads.
        // Per-task progress notifications made the same progress appear twice.
        notificationManager().cancel(notificationId(task.id))
        notificationManager().notify(NOTIFICATION_ID, summaryNotification())
    }

    private fun summaryNotification(): android.app.Notification {
        val active = DownloadsStore.active.value.filter { it.running }
        val known = active.filter { it.progress > 0f }
        val average = known.map { it.progress }.average().takeIf { !it.isNaN() }?.toInt()?.coerceIn(0, 100) ?: 0
        val detail = when {
            active.isEmpty() -> tr("ダウンロード処理を終了しています", "Finishing download tasks")
            active.size == 1 && average > 0 -> "${average}% · ${active.first().detail}"
            active.size == 1 -> active.first().detail
            average > 0 -> tr("平均 ${average}% · ${active.size}件を処理中", "Average ${average}% · Processing ${active.size}")
            else -> tr("${active.size}件を処理中", "Processing ${active.size}")
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (active.size <= 1) active.firstOrNull()?.title ?: tr("ダウンロード", "Download") else tr("${active.size}件をダウンロード中", "Downloading ${active.size} items"))
            .setContentText(detail)
            .setProgress(100, average, active.isNotEmpty() && known.isEmpty())
            .setOngoing(active.isNotEmpty())
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .build()
    }

    private fun notificationId(taskId: String): Int = 1_000 + (taskId.hashCode() and 0x3FFF)

    private fun errorNotification(message: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle(tr("ダウンロードできませんでした", "Download failed"))
        .setContentText(message)
        .setAutoCancel(true)
        .setContentIntent(openAppIntent())
        .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager().createNotificationChannel(
                NotificationChannel(CHANNEL_ID, tr("ダウンロード", "Downloads"), NotificationManager.IMPORTANCE_LOW).apply {
                    description = tr("動画のダウンロード進捗", "Video download progress")
                }
            )
        }
    }

    private fun clearLegacyTaskNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        notificationManager().activeNotifications
            .filter { it.id in LEGACY_TASK_NOTIFICATION_RANGE }
            .forEach { notificationManager().cancel(it.tag, it.id) }
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DOWNLOAD = "dev.noirload.DOWNLOAD"
        const val ACTION_CANCEL = "dev.noirload.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_FALLBACK_URLS = "fallback_urls"
        const val EXTRA_COOKIE = "cookie"
        const val EXTRA_USER_AGENT = "user_agent"
        const val EXTRA_PREFER_DIRECT = "prefer_direct"
        const val EXTRA_REFERER_URL = "referer_url"
        private const val CHANNEL_ID = "noir_downloads"
        private const val NOTIFICATION_ID = 701
        private val LEGACY_TASK_NOTIFICATION_RANGE = 1_000..17_383
        private const val UPDATE_INTERVAL_MS = 12 * 60 * 60 * 1000L
        private const val MAX_PAGE_CHARS = 5_000_000
        private val DIRECT_EXTENSIONS = setOf("mp4", "m4v", "webm", "mov", "m4a", "mp3", "opus")
        private val LIBRARY_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "m4v", "avi", "mp3", "m4a", "opus")
        private val MEDIA_ATTRIBUTE_REGEX = Regex(
            """(?:src|content|file|url)\s*[:=]\s*["']([^"']+(?:\.mp4|\.m4v|\.webm|\.mov|\.m3u8|\.mpd)[^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        private val MEDIA_URL_REGEX = Regex(
            """https?:\\?/\\?/[^"'\s<>]+?(?:\.mp4|\.m4v|\.webm|\.mov|\.m3u8|\.mpd)[^"'\s<>]*""",
            RegexOption.IGNORE_CASE
        )
        private val ENGINE_LOCK = Any()
        private val OUTPUT_LOCK = Any()
    }

    private class DownloadCancelled : RuntimeException()
}
