package dev.noirload.app.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.noirload.app.BrowserIdentity
import dev.noirload.app.tr
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import org.json.JSONObject
import kotlin.concurrent.thread

data class MediaFile(
    val file: File,
    val title: String,
    val size: Long,
    val modified: Long,
    val site: String,
    val durationMs: Long
)

data class ActiveDownload(
    val url: String,
    val title: String = tr("リンクを解析中", "Analyzing link"),
    val progress: Float = 0f,
    val etaSeconds: Long = 0,
    val detail: String = tr("yt-dlpを起動しています", "Starting yt-dlp"),
    val running: Boolean = true,
    val error: String? = null,
    val id: String = url
)

data class AnalyzedMedia(
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Double = 0.0,
    val sizeBytes: Long = 0L
)

data class UrlAnalysisState(
    val url: String,
    val running: Boolean = true,
    val items: List<AnalyzedMedia> = emptyList(),
    val error: String? = null,
    val browserProbeFinished: Boolean = false
)

object DownloadsStore {
    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files = _files.asStateFlow()

    private val _active = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val active = _active.asStateFlow()

    private val _urlAnalysis = MutableStateFlow<UrlAnalysisState?>(null)
    val urlAnalysis = _urlAnalysis.asStateFlow()
    private val browserProbeResults = java.util.concurrent.ConcurrentHashMap<String, List<AnalyzedMedia>>()

    @Synchronized
    fun directory(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val target = File(downloads, "Veiload")
        val legacy = File(downloads, "NoirLoad")
        if (!target.exists() && legacy.isDirectory) {
            if (legacy.renameTo(target)) return target
            return legacy
        }
        return target.apply { mkdirs() }
    }

    fun scan(context: Context) {
        thread(name = "noir-library-scan") {
            val extensions = setOf("mp4", "mkv", "webm", "mov", "m4v", "avi", "mp3", "m4a", "opus")
            val allFiles = directory().listFiles()?.filter(File::isFile).orEmpty()
            val validMedia = allFiles.filter { it.length() > 0 && it.extension.lowercase() in extensions }
            val validSidecars = validMedia.mapTo(HashSet()) { "${it.nameWithoutExtension}.info.json".lowercase() }
            val downloadRunning = _active.value.any { it.running }
            allFiles.forEach { file ->
                val isSidecar = file.name.endsWith(".info.json", ignoreCase = true)
                val keepSidecar = isSidecar && file.name.lowercase() in validSidecars
                val inProgressSidecar = downloadRunning && isSidecar && ".__noir_" in file.name
                val keep = file in validMedia || keepSidecar || inProgressSidecar || (downloadRunning && !isSidecar)
                if (!keep) runCatching { file.delete() }
            }
            _files.value = validMedia
                ?.asSequence()
                ?.map { file ->
                    val info = readInfo(file)
                    MediaFile(
                        file = file,
                        title = file.nameWithoutExtension.replace(Regex("\\s*\\[[^]]+]$"), ""),
                        size = file.length(),
                        modified = file.lastModified(),
                        site = sourceDomain(info),
                        durationMs = info?.optDouble("duration", 0.0)?.times(1000)?.toLong()?.takeIf { it > 0 } ?: readDuration(file)
                    )
                }
                ?.sortedByDescending { it.modified }
                ?.toList()
                .orEmpty()
        }
    }

    fun delete(context: Context, media: MediaFile) {
        thread(name = "noir-library-delete") {
            val removed = runCatching { media.file.delete() }.getOrDefault(false)
            if (removed) {
                File(media.file.parentFile, "${media.file.nameWithoutExtension}.info.json").delete()
                _files.value = _files.value.filterNot { it.file.absolutePath == media.file.absolutePath }
                MediaScannerConnection.scanFile(context, arrayOf(media.file.absolutePath), null, null)
            } else {
                scan(context)
            }
        }
    }

    private fun readInfo(file: File): JSONObject? = runCatching {
        val sidecar = File(file.parentFile, "${file.nameWithoutExtension}.info.json")
        sidecar.takeIf { it.exists() }?.readText()?.let(::JSONObject)
    }.getOrNull()

    private fun readDuration(file: File): Long = runCatching {
        MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally { retriever.release() }
        }
    }.getOrDefault(0L)

    private fun sourceDomain(info: JSONObject?): String {
        if (info == null) return tr("不明", "Unknown")
        val sourceUrl = listOf("webpage_url", "original_url", "source_url")
            .asSequence().map(info::optString)
            .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
        val host = sourceUrl?.let { runCatching { Uri.parse(it).host }.getOrNull() }
            ?.lowercase()?.removePrefix("www.")?.trim('.')
        if (!host.isNullOrBlank()) return host
        return listOf("source_domain", "webpage_url_domain")
            .asSequence().map(info::optString)
            .map { it.lowercase().removePrefix("www.").trim('.') }
            .firstOrNull { domain -> domain.matches(Regex("[a-z0-9.-]+\\.[a-z]{2,}")) }
            ?: tr("不明", "Unknown")
    }

    @Synchronized
    fun start(
        context: Context,
        url: String,
        title: String,
        fallbackUrls: List<String>,
        preferDirect: Boolean = false,
        refererUrl: String = url
    ) {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) return
        if (_active.value.count { it.running } >= 3) {
            Toast.makeText(context, tr("同時ダウンロードは3件までです", "Up to 3 simultaneous downloads are allowed"), Toast.LENGTH_SHORT).show()
            return
        }
        val taskId = java.util.UUID.randomUUID().toString()
        _active.value = _active.value + ActiveDownload(
            clean,
            title = title.ifBlank { tr("リンクを解析中", "Analyzing link") },
            detail = tr("ダウンロード可能な動画を確認しています", "Checking available videos"),
            id = taskId
        )
        Toast.makeText(context, tr("ダウンロードを開始しました", "Download started"), Toast.LENGTH_SHORT).show()
        val intent = Intent(context, DownloadService::class.java)
            .setAction(DownloadService.ACTION_DOWNLOAD)
            .putExtra(DownloadService.EXTRA_URL, clean)
            .putExtra(DownloadService.EXTRA_TASK_ID, taskId)
            .putExtra(DownloadService.EXTRA_TITLE, title)
            .putStringArrayListExtra(DownloadService.EXTRA_FALLBACK_URLS, ArrayList(fallbackUrls))
            .putExtra(
                DownloadService.EXTRA_COOKIE,
                CookieManager.getInstance().getCookie(clean)
                    ?: CookieManager.getInstance().getCookie(refererUrl).orEmpty()
            )
            .putExtra(DownloadService.EXTRA_USER_AGENT, BrowserIdentity.USER_AGENT)
            .putExtra(DownloadService.EXTRA_PREFER_DIRECT, preferDirect)
            .putExtra(DownloadService.EXTRA_REFERER_URL, refererUrl)
        ContextCompat.startForegroundService(context, intent)
    }

    fun analyzeUrl(context: Context, url: String) {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) return
        browserProbeResults.remove(clean)
        _urlAnalysis.value = UrlAnalysisState(clean)
        val cookie = CookieManager.getInstance().getCookie(clean).orEmpty()
        val userAgent = BrowserIdentity.USER_AGENT
        thread(name = "noir-url-analysis") {
            val result = runCatching {
                YoutubeDL.getInstance().init(context.applicationContext)
                val request = YoutubeDLRequest(clean).apply {
                    addOption("--dump-single-json")
                    addOption("--flat-playlist")
                    addOption("--skip-download")
                    addOption("--playlist-end", "30")
                    addOption("--no-warnings")
                    if (userAgent.isNotBlank()) addOption("--user-agent", userAgent)
                    addOption("--add-headers", "Referer:$clean")
                    if (cookie.isNotBlank()) addOption("--add-headers", "Cookie:$cookie")
                    val quickJs = File(context.applicationInfo.nativeLibraryDir, "libqjs.so")
                    if (quickJs.exists()) addOption("--js-runtimes", "quickjs:${quickJs.absolutePath}")
                }
                val response = YoutubeDL.getInstance().execute(request, "noir-url-analysis")
                parseAnalysisJson(response.out, clean)
            }
            if (_urlAnalysis.value?.url == clean) {
                _urlAnalysis.value = result.fold(
                    onSuccess = { items ->
                        if (items.isEmpty()) UrlAnalysisState(clean, running = false, error = tr("動画を検出できませんでした", "No video was detected"))
                        else UrlAnalysisState(clean, running = false, items = items)
                    },
                    onFailure = { error -> UrlAnalysisState(clean, running = false, error = error.message?.lineSequence()?.lastOrNull()?.take(120) ?: tr("URLを解析できませんでした", "Could not analyze the URL")) }
                )
                browserProbeResults[clean]?.let { completeBrowserProbe(clean, it) }
            }
        }
    }

    fun completeBrowserProbe(url: String, items: List<AnalyzedMedia>) {
        val safeItems = items.filterNot { isLikelyVideoAdUrl(it.url) }.distinctBy { it.url }
        browserProbeResults[url] = safeItems
        val current = _urlAnalysis.value ?: return
        if (current.url != url) return
        _urlAnalysis.value = current.copy(
            items = (current.items + safeItems).distinctBy { it.url },
            error = if (safeItems.isNotEmpty()) null else current.error,
            browserProbeFinished = true
        )
    }

    fun clearUrlAnalysis(expected: UrlAnalysisState? = null) {
        if (expected == null || _urlAnalysis.value == expected) _urlAnalysis.value = null
    }

    private fun parseAnalysisJson(output: String, sourceUrl: String): List<AnalyzedMedia> {
        val jsonLine = output.lineSequence().map(String::trim).lastOrNull { it.startsWith("{") && it.endsWith("}") }
            ?: throw IllegalStateException(tr("動画情報を取得できませんでした", "Could not retrieve video information"))
        val root = JSONObject(jsonLine)
        val entries = root.optJSONArray("entries")
        val objects = if (entries != null) buildList {
            repeat(entries.length()) { index -> entries.optJSONObject(index)?.let(::add) }
        } else listOf(root)
        return objects.mapNotNull { item ->
            val mediaUrl = listOf("webpage_url", "original_url", "url").asSequence()
                .map(item::optString).firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                ?: return@mapNotNull null
            val thumbnail = item.optString("thumbnail").takeIf { it.startsWith("http") }
                ?: item.optJSONArray("thumbnails")?.let { thumbnails ->
                    (thumbnails.length() - 1 downTo 0).firstNotNullOfOrNull { thumbnails.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank) }
                }
            AnalyzedMedia(
                title = item.optString("title").ifBlank { Uri.parse(mediaUrl).host ?: tr("動画", "Video") },
                url = mediaUrl,
                thumbnailUrl = thumbnail,
                durationSeconds = item.optDouble("duration", 0.0).takeIf { it.isFinite() } ?: 0.0,
                sizeBytes = maxOf(item.optLong("filesize", 0L), item.optLong("filesize_approx", 0L))
            )
        }.distinctBy { it.url }.ifEmpty {
            root.optString("title").takeIf { it.isNotBlank() }?.let { listOf(AnalyzedMedia(it, sourceUrl)) }.orEmpty()
        }
    }

    fun cancel(context: Context, taskId: String) {
        context.startService(
            Intent(context, DownloadService::class.java)
                .setAction(DownloadService.ACTION_CANCEL)
                .putExtra(DownloadService.EXTRA_TASK_ID, taskId)
        )
    }

    fun dismiss(expected: ActiveDownload? = null) {
        _active.value = if (expected == null) emptyList() else _active.value.filterNot { it.id == expected.id }
    }

    @Synchronized
    internal fun update(state: ActiveDownload) {
        val current = _active.value
        _active.value = if (current.any { it.id == state.id }) {
            current.map { if (it.id == state.id) state else it }
        } else current + state
    }

    @Synchronized
    internal fun remove(taskId: String) { _active.value = _active.value.filterNot { it.id == taskId } }
}
