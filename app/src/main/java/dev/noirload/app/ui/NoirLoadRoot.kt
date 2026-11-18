@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package dev.noirload.app.ui

import android.app.Activity
import android.app.DownloadManager
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.PowerManager
import android.os.Message
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.URLUtil
import android.view.ViewGroup
import android.view.View
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayInputStream
import dev.noirload.app.download.ActiveDownload
import dev.noirload.app.download.AnalyzedMedia
import dev.noirload.app.download.DownloadsStore
import dev.noirload.app.download.MediaFile
import dev.noirload.app.download.isLikelyVideoAdUrl
import dev.noirload.app.R
import dev.noirload.app.BuildConfig
import dev.noirload.app.BrowserIdentity
import dev.noirload.app.AppLanguage
import dev.noirload.app.AppLocale
import dev.noirload.app.languageLabel
import dev.noirload.app.tr
import dev.noirload.app.update.UpdateChecker
import dev.noirload.app.update.UpdateInstaller
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.*
import com.adamglin.phosphoricons.regular.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFF000000)
private val Panel = Color(0xFF1C1C1E)
private val Elevated = Color(0xFF2C2C2E)
private val Line = Color(0xFF38383A)
private val Paper = Color(0xFFF2F2F7)
private val Muted = Color(0xFF8E8E93)
private var currentAccentColor by mutableStateOf(Color(0xFFFF9F0A))
private val Acid: Color get() = currentAccentColor
private val Blue = Color(0xFF64D2FF)
private val Red = Color(0xFFFF453A)

private const val DIRECT_MEDIA_DISCOVERY_SCRIPT = """
    (() => {
      const found = new Set();
      const add = value => {
        try {
          if (!value) return;
          const url = new URL(value, document.baseURI).href;
          if (url.startsWith('http://') || url.startsWith('https://')) found.add(url);
        } catch (_) {}
      };
      document.querySelectorAll('video, video source, audio, audio source').forEach(node => {
        add(node.currentSrc); add(node.src); add(node.getAttribute('src'));
      });
      const mediaExtensions = ['.mp4', '.m4v', '.webm', '.mov', '.m3u8', '.mpd', '.m4a', '.mp3'];
      performance.getEntriesByType('resource').forEach(entry => {
        const clean = entry.name.toLowerCase().split(/[?#]/)[0];
        if (mediaExtensions.some(extension => clean.endsWith(extension))) add(entry.name);
      });
      return JSON.stringify(Array.from(found).slice(0, 12));
    })()
"""

private const val PAGE_MEDIA_ANALYSIS_SCRIPT = """
    (() => {
      const found = new Map();
      const add = (value, metadata = {}, base = document.baseURI) => {
        try {
          if (!value) return;
          const url = new URL(value, base).href;
          if (!url.startsWith('http://') && !url.startsWith('https://')) return;
          const previous = found.get(url) || { url, poster: '', duration: 0, size: 0, height: 0 };
          let poster = metadata.poster || previous.poster || '';
          if (poster && !poster.startsWith('data:image/')) poster = new URL(poster, base).href;
          found.set(url, {
            url,
            poster,
            duration: metadata.duration || previous.duration || 0,
            size: Math.max(metadata.size || 0, previous.size || 0),
            height: metadata.height || previous.height || 0
          });
        } catch (_) {}
      };
      const captureFrame = player => {
        let captured = '';
        if (player.tagName === 'VIDEO' && player.videoWidth > 0 && player.videoHeight > 0) {
          try {
            const canvas = document.createElement('canvas');
            canvas.width = 320;
            canvas.height = Math.max(180, Math.round(320 * player.videoHeight / player.videoWidth));
            canvas.getContext('2d').drawImage(player, 0, 0, canvas.width, canvas.height);
            captured = canvas.toDataURL('image/jpeg', 0.72);
          } catch (_) {}
        }
        return captured;
      };
      const documents = [];
      const visit = doc => {
        if (!doc || documents.includes(doc)) return;
        documents.push(doc);
        doc.querySelectorAll('iframe').forEach(frame => {
          try { if (frame.contentDocument) visit(frame.contentDocument); } catch (_) {}
          const src = frame.src || frame.getAttribute('src') || '';
          const hint = [src, frame.name || '', frame.id || '', frame.className || '', frame.allow || ''].join(' ').toLowerCase();
          if (/(video|player|embed|stream|youtube|youtu\.be|vimeo|dailymotion|fullscreen|autoplay)/.test(hint)) {
            add(src, {}, doc.baseURI);
          }
        });
      };
      visit(document);
      const players = documents.flatMap(doc => Array.from(doc.querySelectorAll('video, audio')).map(player => ({ player, doc })));
      players.forEach(({ player, doc }) => {
        const metadata = { poster: player.poster || player.getAttribute('poster') || captureFrame(player), duration: Number.isFinite(player.duration) ? player.duration : 0, height: player.videoHeight || 0 };
        add(player.currentSrc, metadata, doc.baseURI); add(player.src, metadata, doc.baseURI); add(player.getAttribute('src'), metadata, doc.baseURI);
        player.querySelectorAll('source').forEach(source => { add(source.src, metadata, doc.baseURI); add(source.getAttribute('src'), metadata, doc.baseURI); });
      });
      const primary = players.length === 1 ? {
        poster: players[0].player.poster || players[0].player.getAttribute('poster') || captureFrame(players[0].player),
        duration: Number.isFinite(players[0].player.duration) ? players[0].player.duration : 0,
        height: players[0].player.videoHeight || 0
      } : {};
      const extensions = ['.mp4', '.m4v', '.webm', '.mov', '.m3u8', '.mpd', '.m4a', '.mp3'];
      documents.forEach(doc => {
        try { doc.defaultView.performance.getEntriesByType('resource').forEach(entry => {
          const clean = entry.name.toLowerCase().split(/[?#]/)[0];
          if (entry.initiatorType === 'video' || extensions.some(extension => clean.endsWith(extension))) add(entry.name, { ...primary, size: entry.transferSize || entry.encodedBodySize || 0 }, doc.baseURI);
        }); } catch (_) {}
      });
      return JSON.stringify({ pageTitle: document.title || '', hasVideo: players.length > 0, items: Array.from(found.values()).slice(0, 30) });
    })()
"""

private const val SITE_DOWNLOAD_REARM_SCRIPT = """
    (() => {
      document.querySelectorAll('a[download], a[href]').forEach(node => {
        const href = (node.href || '').toLowerCase().split(/[?#]/)[0];
        if (node.hasAttribute('download') || /\.(mp4|m4v|webm|mov|mkv|avi|m3u8|mpd)$/.test(href)) {
          node.removeAttribute('disabled');
          node.removeAttribute('aria-disabled');
          node.style.pointerEvents = '';
          if (!node.dataset.noirDownloadRearmed) {
            node.dataset.noirDownloadRearmed = '1';
            node.addEventListener('click', event => {
              if (!node.href) return;
              event.preventDefault();
              event.stopImmediatePropagation();
              window.location.assign(node.href);
            }, true);
          }
        }
      });
      document.querySelectorAll('button[disabled], [role="button"][aria-disabled="true"]').forEach(node => {
        const label = (node.textContent || node.getAttribute('aria-label') || '').toLowerCase();
        if (/download|保存|ダウンロード/.test(label)) {
          node.removeAttribute('disabled');
          node.removeAttribute('aria-disabled');
          node.style.pointerEvents = '';
          node.dataset.noirDownloadRearmedButton = '1';
        }
      });
    })()
"""

private enum class Tab { UrlDownload, Browser, Library, MyPage }
private enum class DurationFilter { All, Short, Medium, Long }
private const val ALL_SITES = "__all_sites__"
private enum class MediaAvailability { Analyzing, Available, Unavailable }

private data class DetectedMedia(
    val url: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Double = 0.0,
    val sizeBytes: Long = 0L,
    val refererUrl: String? = null
)

private data class BrowserFullscreenView(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback
)

private data class SiteDownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long,
    val fileName: String
)

private enum class SearchEngine(val label: String, val homeUrl: String, val searchUrl: String) {
    Google("Google", "https://www.google.com", "https://www.google.com/search?q="),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com", "https://duckduckgo.com/?q="),
    Bing("Bing", "https://www.bing.com", "https://www.bing.com/search?q="),
    Yahoo("Yahoo! JAPAN", "https://www.yahoo.co.jp", "https://search.yahoo.co.jp/search?p="),
    Yandex("Yandex", "https://yandex.com", "https://yandex.com/search/?text="),
    Brave("Brave Search", "https://search.brave.com", "https://search.brave.com/search?q=");

    companion object {
        fun from(value: String?): SearchEngine = entries.firstOrNull { it.name == value } ?: Google
    }
}

private enum class AccentChoice(val japaneseLabel: String, val englishLabel: String, val color: Color) {
    RedAccent("赤", "Red", Color(0xFFFF453A)),
    Orange("オレンジ", "Orange", Color(0xFFFF9F0A)),
    BlueAccent("青", "Blue", Color(0xFF0A84FF)),
    Green("緑", "Green", Color(0xFF30D158));

    val label get() = tr(japaneseLabel, englishLabel)

    companion object {
        fun from(value: String?): AccentChoice = entries.firstOrNull { it.name == value } ?: Orange
    }
}

private object WebAdBlocker {
    private val fallbackDomains = setOf(
        "2mdn.net", "adform.net", "adnxs.com", "adsrvr.org", "advertising.com",
        "amazon-adsystem.com", "casalemedia.com", "criteo.com", "criteo.net",
        "doubleclick.net", "googleadservices.com", "googlesyndication.com", "imasdk.googleapis.com",
        "moatads.com", "openx.net", "outbrain.com", "pubmatic.com",
        "quantserve.com", "rubiconproject.com", "scorecardresearch.com",
        "smartadserver.com", "taboola.com"
    )
    private val advertisingMarkers = listOf(
        "/adserver", "/adsystem/", "/pagead/", "/prebid", "/vads/",
        "adservice.", "adservice/", "googleads.", "securepubads."
    )
    private data class Rules(
        val blockedDomains: Set<String> = emptySet(),
        val allowedDomains: Set<String> = emptySet(),
        val blockedPaths: Map<String, Set<String>> = emptyMap(),
        val allowedPaths: Map<String, Set<String>> = emptyMap(),
        val blockedTokens: Map<String, Set<String>> = emptyMap(),
        val allowedTokens: Map<String, Set<String>> = emptyMap(),
        val genericCosmetic: Set<String> = emptySet(),
        val domainCosmetic: Map<String, Set<String>> = emptyMap(),
        val genericCosmeticExceptions: Set<String> = emptySet(),
        val domainCosmeticExceptions: Map<String, Set<String>> = emptyMap(),
        val count: Int = 0
    )
    @Volatile private var easyList = Rules()

    suspend fun loadAndRefresh(context: Context) = withContext(Dispatchers.IO) {
        val directory = java.io.File(context.filesDir, "filters").apply { mkdirs() }
        val subscriptions = listOf(
            Triple("easylist", EASYLIST_URL, java.io.File(directory, "easylist.txt")),
            Triple("easyprivacy", EASYPRIVACY_URL, java.io.File(directory, "easyprivacy.txt"))
        )
        subscriptions.forEach { (name, url, cache) -> refreshSubscription(directory, name, url, cache) }
        val combined = subscriptions.mapNotNull { it.third.takeIf(java.io.File::exists)?.readText() }
            .joinToString("\n")
        if (combined.isNotBlank()) runCatching { parse(combined) }.getOrNull()?.let { easyList = it }
    }

    private fun refreshSubscription(directory: java.io.File, name: String, url: String, cache: java.io.File) {
        if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < UPDATE_INTERVAL_MS) return
        val temporary = java.io.File(directory, "$name.download")
        runCatching {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            try {
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", BrowserIdentity.USER_AGENT)
                check(connection.responseCode in 200..299) { "$name HTTP ${connection.responseCode}" }
                connection.inputStream.use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= MAX_LIST_BYTES) { "$name is too large" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
            } finally { connection.disconnect() }
            check(parse(temporary.readText()).count >= MIN_RULE_COUNT) { "$name did not contain enough rules" }
            if (!temporary.renameTo(cache)) {
                temporary.copyTo(cache, overwrite = true)
                temporary.delete()
            }
            cache.setLastModified(System.currentTimeMillis())
        }.onFailure { temporary.delete() }
    }

    fun shouldBlock(request: WebResourceRequest, pageUrl: String, blockVideoAds: Boolean): Boolean {
        if (request.isForMainFrame) return false
        val url = request.url.toString()
        val requestHost = request.url.host.orEmpty().lowercase().removePrefix("www.")
        if (requestHost.isBlank()) return false
        val videoAdRequest = isLikelyVideoAdUrl(url)
        if (!videoAdRequest && isEssentialPlaybackRequest(requestHost, request.url.path.orEmpty())) return false
        if (!blockVideoAds && (videoAdRequest || isDirectMediaUrl(url))) return false
        val rules = easyList
        if (matchesDomain(requestHost, rules.allowedDomains) || matchesPath(requestHost, url, rules.allowedPaths)) return false
        if (matchesDomain(requestHost, rules.blockedDomains) || matchesPath(requestHost, url, rules.blockedPaths)) return true
        if (matchesDomain(requestHost, fallbackDomains)) return true
        if (blockVideoAds && videoAdRequest) return true
        val pageHost = runCatching {
            Uri.parse(pageUrl).host.orEmpty().lowercase().removePrefix("www.")
        }.getOrDefault("")
        return requestHost != pageHost && advertisingMarkers.any { url.contains(it, ignoreCase = true) }
    }

    private fun isEssentialPlaybackRequest(host: String, path: String): Boolean = when {
        host == "googlevideo.com" || host.endsWith(".googlevideo.com") -> true
        host == "ytimg.com" || host.endsWith(".ytimg.com") -> true
        host == "youtube.com" || host.endsWith(".youtube.com") ->
            path.startsWith("/youtubei/v1/player") || path.startsWith("/youtubei/v1/next") ||
                path.startsWith("/api/stats/") || path.startsWith("/s/player/")
        else -> false
    }

    private fun matchesDomain(host: String, domains: Set<String>): Boolean {
        var candidate = host
        while (true) {
            if (candidate in domains) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    private fun matchesPath(host: String, url: String, paths: Map<String, Set<String>>): Boolean {
        var candidate = host
        while (true) {
            if (paths[candidate]?.any { url.contains(it, ignoreCase = true) } == true) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    private fun matchesToken(url: String, index: Map<String, Set<String>>): Boolean {
        if (index.isEmpty() || url.length < 3) return false
        val value = url.lowercase()
        for (position in 0..value.length - 3) {
            val candidates = index[value.substring(position, position + 3)] ?: continue
            if (candidates.any(value::contains)) return true
        }
        return false
    }

    private fun parse(text: String): Rules {
        val blockedDomains = HashSet<String>()
        val allowedDomains = HashSet<String>()
        val blockedPaths = HashMap<String, MutableSet<String>>()
        val allowedPaths = HashMap<String, MutableSet<String>>()
        val blockedTokens = HashSet<String>()
        val allowedTokens = HashSet<String>()
        val genericCosmetic = HashSet<String>()
        val domainCosmetic = HashMap<String, MutableSet<String>>()
        val genericCosmeticExceptions = HashSet<String>()
        val domainCosmeticExceptions = HashMap<String, MutableSet<String>>()
        var count = 0
        text.lineSequence().forEach { raw ->
            var line = raw.trim()
            if (line.isBlank() || line.startsWith('!') || line.startsWith('[')) return@forEach
            val cosmeticMarker = when {
                "#@#" in line -> "#@#"
                "##" in line && "#?#" !in line -> "##"
                else -> null
            }
            if (cosmeticMarker != null) {
                val selector = line.substringAfter(cosmeticMarker).trim()
                if (selector.isBlank() || selector.length > 400 || selector.startsWith("+js(") ||
                    '{' in selector || '}' in selector) return@forEach
                val exception = cosmeticMarker == "#@#"
                val domains = line.substringBefore(cosmeticMarker).split(',').map(String::trim).filter(String::isNotBlank)
                if (domains.isEmpty()) {
                    (if (exception) genericCosmeticExceptions else genericCosmetic) += selector
                } else {
                    domains.forEach { rawDomain ->
                        val excluded = rawDomain.startsWith('~')
                        val domain = rawDomain.removePrefix("~").lowercase().removePrefix("www.")
                        if (domain.isNotBlank() && '.' in domain) {
                            val target = if (exception || excluded) domainCosmeticExceptions else domainCosmetic
                            target.getOrPut(domain) { HashSet() } += selector
                        }
                    }
                }
                count++
                return@forEach
            }
            if ("#?#" in line || line.startsWith('/')) return@forEach
            val exception = line.startsWith("@@")
            if (exception) line = line.removePrefix("@@")
            val networkRule = line.substringBefore('$').trim()
            if (!networkRule.startsWith("||")) {
                val token = networkRule.trim('|')
                    .split('*', '^', '|')
                    .maxByOrNull { it.length }
                    .orEmpty().lowercase()
                if (token.length >= 6) {
                    (if (exception) allowedTokens else blockedTokens) += token
                    count++
                }
                return@forEach
            }
            val anchored = networkRule.removePrefix("||")
            val host = anchored.takeWhile { it.isLetterOrDigit() || it == '.' || it == '-' }
                .trim('.').lowercase()
            if (host.isBlank() || '.' !in host) return@forEach
            val remainder = anchored.substring(host.length).trimStart('^', '|')
            if (remainder.isBlank()) {
                (if (exception) allowedDomains else blockedDomains) += host
            } else {
                val token = remainder.split('*', '^', '|').maxByOrNull { it.length }.orEmpty()
                if (token.length >= 4) {
                    val target = if (exception) allowedPaths else blockedPaths
                    target.getOrPut(host) { HashSet() } += token
                }
            }
            count++
        }
        return Rules(
            blockedDomains = blockedDomains,
            allowedDomains = allowedDomains,
            blockedPaths = blockedPaths.mapValues { it.value.toSet() },
            allowedPaths = allowedPaths.mapValues { it.value.toSet() },
            blockedTokens = indexTokens(blockedTokens),
            allowedTokens = indexTokens(allowedTokens),
            genericCosmetic = genericCosmetic,
            domainCosmetic = domainCosmetic.mapValues { it.value.toSet() },
            genericCosmeticExceptions = genericCosmeticExceptions,
            domainCosmeticExceptions = domainCosmeticExceptions.mapValues { it.value.toSet() },
            count = count
        )
    }

    fun cosmeticCss(pageUrl: String): String {
        val host = runCatching { Uri.parse(pageUrl).host.orEmpty().lowercase().removePrefix("www.") }.getOrDefault("")
        if (host.isBlank()) return ""
        val rules = easyList
        val selectors = LinkedHashSet(rules.genericCosmetic)
        val exceptions = HashSet(rules.genericCosmeticExceptions)
        var domain = host
        while (true) {
            rules.domainCosmetic[domain]?.let(selectors::addAll)
            rules.domainCosmeticExceptions[domain]?.let(exceptions::addAll)
            val dot = domain.indexOf('.')
            if (dot < 0) break
            domain = domain.substring(dot + 1)
        }
        selectors.removeAll(exceptions)
        return selectors.asSequence().take(MAX_COSMETIC_SELECTORS).chunked(120).joinToString("\n") { chunk ->
            "${chunk.joinToString(",")}{display:none!important;visibility:hidden!important;}"
        }
    }

    private fun indexTokens(tokens: Set<String>): Map<String, Set<String>> {
        val result = HashMap<String, MutableSet<String>>()
        tokens.forEach { token -> result.getOrPut(token.take(3)) { HashSet() } += token }
        return result.mapValues { it.value.toSet() }
    }

    fun emptyResponse() = WebResourceResponse(
        "text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))
    )

    private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
    private const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
    private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
    private const val MAX_LIST_BYTES = 15L * 1024 * 1024
    private const val MIN_RULE_COUNT = 1_000
    private const val MAX_COSMETIC_SELECTORS = 2_400
}

private class BrowserTabState(val id: Long, initialUrl: String, initialTitle: String = tr("新しいタブ", "New tab")) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf(initialTitle)
    var progress by mutableIntStateOf(0)
    var canBack by mutableStateOf(false)
    var canForward by mutableStateOf(false)
    var mediaAvailability by mutableStateOf(MediaAvailability.Analyzing)
    var favicon by mutableStateOf<Bitmap?>(null)
    val mediaCandidates = mutableStateListOf<DetectedMedia>()
    val popupWebViews = mutableListOf<WebView>()
    var siteDownloadHandler: ((String, String?, String?, String?, Long) -> Unit)? = null
    var lastSiteDownload: SiteDownloadRequest? = null
    var analysisToken = 0
    var webView: WebView? = null
    var savedState: Bundle? = null
}

private class BrowserSession(context: Context, initialUrl: String) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("browser_session", Context.MODE_PRIVATE)
    private var nextId = preferences.getLong("next_id", 1L)
    val tabs = mutableStateListOf<BrowserTabState>()
    var selectedId by mutableStateOf(0L)
        private set
    @Volatile var adBlockEnabled = true
    @Volatile var videoAdBlockEnabled = false
    @Volatile var cosmeticFilteringEnabled = false
    val selected get() = tabs.first { it.id == selectedId }

    init {
        val ids = preferences.getString("tab_ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        ids.forEach { id ->
            val url = preferences.getString("url_$id", null) ?: return@forEach
            tabs += BrowserTabState(id, url, preferences.getString("title_$id", tr("新しいタブ", "New tab")).orEmpty()).also {
                it.savedState = readWebState(id)
                it.favicon = BitmapFactory.decodeFile(faviconFile(id).absolutePath)
            }
        }
        if (tabs.isEmpty()) tabs += BrowserTabState(nextId++, initialUrl)
        selectedId = preferences.getLong("selected_id", tabs.first().id).takeIf { id -> tabs.any { it.id == id } } ?: tabs.first().id
        nextId = maxOf(nextId, (tabs.maxOfOrNull { it.id } ?: 0L) + 1L)
    }

    fun add(homeUrl: String) {
        val tab = BrowserTabState(nextId++, homeUrl)
        tabs += tab
        selectedId = tab.id
        persist()
    }

    fun select(id: Long) {
        if (tabs.any { it.id == id }) {
            selectedId = id
            persist()
        }
    }

    fun close(id: Long, homeUrl: String) {
        if (tabs.size == 1) {
            tabs.first().webView?.loadUrl(homeUrl)
            return
        }
        val index = tabs.indexOfFirst { it.id == id }
        val closing = tabs[index]
        if (selectedId == id) selectedId = tabs[if (index > 0) index - 1 else 1].id
        tabs.removeAt(index)
        closing.popupWebViews.forEach(WebView::destroy)
        closing.popupWebViews.clear()
        closing.webView?.destroy()
        closing.webView = null
        webStateFile(id).delete()
        faviconFile(id).delete()
        persist()
    }

    fun persist() {
        tabs.forEach { tab ->
            tab.webView?.let { view ->
                runCatching {
                    val state = Bundle()
                    view.saveState(state)
                    writeWebState(tab.id, state)
                }
            }
        }
        preferences.edit().apply {
            putString("tab_ids", tabs.joinToString(",") { it.id.toString() })
            putLong("selected_id", selectedId)
            putLong("next_id", nextId)
            tabs.forEach { putString("url_${it.id}", it.url); putString("title_${it.id}", it.title) }
        }.apply()
    }

    private fun webStateFile(id: Long) = java.io.File(appContext.filesDir, "browser-state-$id.bin")
    private fun faviconFile(id: Long) = java.io.File(appContext.filesDir, "browser-favicon-$id.png")

    fun saveFavicon(tab: BrowserTabState, bitmap: Bitmap) {
        tab.favicon = bitmap
        runCatching {
            faviconFile(tab.id).outputStream().buffered().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val host = Uri.parse(tab.url).host.orEmpty().lowercase().removePrefix("www.")
            if (host.isNotBlank()) {
                siteFaviconFile(appContext, host).apply { parentFile?.mkdirs() }
                    .outputStream().buffered().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    private fun writeWebState(id: Long, state: Bundle) {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(state)
            webStateFile(id).writeBytes(parcel.marshall())
        } finally { parcel.recycle() }
    }

    private fun readWebState(id: Long): Bundle? = runCatching {
        val bytes = webStateFile(id).takeIf { it.exists() }?.readBytes() ?: return@runCatching null
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            parcel.readBundle(WebView::class.java.classLoader)
        } finally { parcel.recycle() }
    }.getOrNull()

    fun destroy() {
        persist()
        CookieManager.getInstance().flush()
        tabs.forEach {
            it.popupWebViews.forEach(WebView::destroy)
            it.popupWebViews.clear()
            it.webView?.destroy()
            it.webView = null
        }
    }
}

@Composable
fun NoirLoadRoot(initialUrl: String?) {
    var tab by remember { mutableStateOf(Tab.UrlDownload) }
    var animateTabTransition by remember { mutableStateOf(true) }
    var playingMedia by remember { mutableStateOf<MediaFile?>(null) }
    var browserFullscreen by remember { mutableStateOf<BrowserFullscreenView?>(null) }
    var launchVisible by rememberSaveable { mutableStateOf(true) }
    var launchStarted by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val launchScale by animateFloatAsState(if (launchStarted) 1f else .72f, spring(dampingRatio = .66f, stiffness = 240f), label = "launch-scale")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appPreferences = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var appLanguage by remember { mutableStateOf(AppLocale.selected) }
    remember(appPreferences) {
        if (!appPreferences.getBoolean("ad_block_default_on_v4", false)) {
            appPreferences.edit()
                .putBoolean("ad_block_enabled", true)
                .putBoolean("ad_block_default_on_v4", true)
                .apply()
        }
        true
    }
    remember(appPreferences) {
        if (!appPreferences.getBoolean("cosmetic_filter_default_on_v5", false)) {
            appPreferences.edit()
                .putBoolean("cosmetic_filter_enabled", true)
                .putBoolean("cosmetic_filter_default_on_v5", true)
                .apply()
        }
        true
    }
    var searchEngine by remember { mutableStateOf(SearchEngine.from(appPreferences.getString("search_engine", null))) }
    var accentChoice by remember { mutableStateOf(AccentChoice.from(appPreferences.getString("accent_color", null))) }
    val animatedAccent by animateColorAsState(accentChoice.color, tween(420, easing = FastOutSlowInEasing), label = "accent-theme")
    currentAccentColor = animatedAccent
    var adBlockEnabled by remember { mutableStateOf(appPreferences.getBoolean("ad_block_enabled", true)) }
    var videoAdBlockEnabled by remember { mutableStateOf(appPreferences.getBoolean("video_ad_block_enabled", false)) }
    var cosmeticFilteringEnabled by remember { mutableStateOf(appPreferences.getBoolean("cosmetic_filter_enabled", true)) }
    var focusModeEnabled by remember { mutableStateOf(appPreferences.getBoolean("focus_mode_enabled", false)) }
    val browserSession = remember { BrowserSession(context = context, initialUrl = initialUrl?.takeIf { it.startsWith("http") } ?: searchEngine.homeUrl) }
    browserSession.adBlockEnabled = adBlockEnabled
    browserSession.videoAdBlockEnabled = videoAdBlockEnabled
    browserSession.cosmeticFilteringEnabled = cosmeticFilteringEnabled
    val activeDownloads by DownloadsStore.active.collectAsState()
    val globalUrlAnalysis by DownloadsStore.urlAnalysis.collectAsState()
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        WebAdBlocker.loadAndRefresh(context.applicationContext)
    }
    LaunchedEffect(Unit) {
        pendingUpdate = UpdateChecker.check()
    }
    LaunchedEffect(launchVisible) {
        if (launchVisible) {
            launchStarted = true
            delay(1_150L)
            launchVisible = false
        }
    }

    val downloadCompletionStates = activeDownloads.map { Triple(it.id, it.running, it.error != null) }
    LaunchedEffect(downloadCompletionStates) {
        val completed = activeDownloads.filterNot { it.running }
        if (completed.isNotEmpty()) {
            delay(if (completed.any { it.error != null }) 5_000 else 3_000)
            completed.forEach(DownloadsStore::dismiss)
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (!animateTabTransition) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(430, easing = FastOutSlowInEasing)) { it * direction / 4 } + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally(tween(360)) { -it * direction / 5 } + fadeOut(tween(220)))
                }
            }, label = "screen"
        ) { page ->
            when (page) {
                Tab.Browser -> BrowserScreen(
                    browserSession,
                    searchEngine,
                    onShowFullscreen = { view, callback ->
                        browserFullscreen?.callback?.onCustomViewHidden()
                        browserFullscreen = BrowserFullscreenView(view, callback)
                    },
                    onHideFullscreen = { browserFullscreen = null },
                    onDownload = { url, title, alternatives, preferDirect, referer ->
                        DownloadsStore.start(context, url, title, alternatives, preferDirect, referer)
                    }
                )
                Tab.UrlDownload -> UrlDownloadScreen(onDownload = { url, title, alternatives ->
                    DownloadsStore.start(context, url, title, alternatives)
                })
                Tab.Library -> LibraryScreen(onPlay = { playingMedia = it })
                Tab.MyPage -> SettingsScreen(
                    searchEngine = searchEngine,
                    accentChoice = accentChoice,
                    appLanguage = appLanguage,
                    adBlockEnabled = adBlockEnabled,
                    videoAdBlockEnabled = videoAdBlockEnabled,
                    cosmeticFilteringEnabled = cosmeticFilteringEnabled,
                    focusModeEnabled = focusModeEnabled,
                    onSearchEngineChange = { engine ->
                        searchEngine = engine
                        appPreferences.edit().putString("search_engine", engine.name).apply()
                    },
                    onAccentChoiceChange = { choice ->
                        accentChoice = choice
                        appPreferences.edit().putString("accent_color", choice.name).apply()
                    },
                    onLanguageChange = { language ->
                        AppLocale.set(context, language)
                        appLanguage = language
                        DownloadsStore.scan(context)
                    },
                    onFocusModeChange = { enabled ->
                        focusModeEnabled = enabled
                        appPreferences.edit().putBoolean("focus_mode_enabled", enabled).apply()
                    },
                    onAdBlockEnabledChange = { enabled ->
                        adBlockEnabled = enabled
                        browserSession.adBlockEnabled = enabled
                        appPreferences.edit().putBoolean("ad_block_enabled", enabled).apply()
                        browserSession.selected.webView?.reload()
                    },
                    onVideoAdBlockEnabledChange = { enabled ->
                        videoAdBlockEnabled = enabled
                        browserSession.videoAdBlockEnabled = enabled
                        appPreferences.edit().putBoolean("video_ad_block_enabled", enabled).apply()
                        browserSession.selected.webView?.reload()
                    },
                    onCosmeticFilteringEnabledChange = { enabled ->
                        cosmeticFilteringEnabled = enabled
                        browserSession.cosmeticFilteringEnabled = enabled
                        appPreferences.edit().putBoolean("cosmetic_filter_enabled", enabled).apply()
                        browserSession.selected.webView?.reload()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = !imeVisible && browserFullscreen == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it / 2 }
        ) {
            NoirTabBar(tab, onSelect = { selected, animate ->
                animateTabTransition = animate
                tab = selected
            })
        }

        AnimatedVisibility(
            visible = playingMedia != null,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(220)) + scaleIn(tween(320), initialScale = .96f),
            exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = .97f)
        ) {
            playingMedia?.let { AppVideoPlayer(it, onClose = { playingMedia = null }) }
        }

        browserFullscreen?.let { fullscreen ->
            AndroidView(
                factory = {
                    (fullscreen.view.parent as? ViewGroup)?.removeView(fullscreen.view)
                    fullscreen.view.apply { keepScreenOn = true }
                },
                modifier = Modifier.fillMaxSize().background(Color.Black)
            )
        }

        globalUrlAnalysis?.takeIf { !it.browserProbeFinished }?.let { analysis ->
            UrlAnalysisProbe(analysis.url, cosmeticFilteringEnabled) { detected ->
                DownloadsStore.completeBrowserProbe(
                    analysis.url,
                    detected.map { item ->
                        AnalyzedMedia(
                            title = item.title.orEmpty(),
                            url = item.url,
                            thumbnailUrl = item.thumbnailUrl,
                            durationSeconds = item.durationSeconds,
                            sizeBytes = item.sizeBytes
                        )
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = launchVisible,
            modifier = Modifier.fillMaxSize(),
            exit = fadeOut(tween(320)) + scaleOut(tween(360), targetScale = 1.04f)
        ) {
            Box(
                Modifier.fillMaxSize().background(Ink).pointerInput(Unit) { detectTapGestures(onTap = {}) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(launchScale)) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(82.dp).clip(RoundedCornerShape(27.dp))
                            .shadow(24.dp, RoundedCornerShape(27.dp), ambientColor = Acid.copy(.3f), spotColor = Acid.copy(.4f))
                    )
                    Spacer(Modifier.height(20.dp))
                    Label("VEILOAD", 20.sp, Paper, FontWeight.Black, letterSpacing = 3.4f)
                    Spacer(Modifier.height(7.dp))
                    Label("SAVE WHAT MOVES", 8.sp, Muted, FontWeight.Bold, letterSpacing = 2.1f)
                }
            }
        }

        pendingUpdate?.let { update ->
            UpdateInstallDialog(update, onDismiss = { pendingUpdate = null })
        }
    }
    val activity = context as? Activity
    LaunchedEffect(focusModeEnabled, browserFullscreen, activity) {
        if (activity == null) return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (focusModeEnabled || browserFullscreen != null) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
    DisposableEffect(activity) {
        onDispose {
            browserFullscreen?.view?.keepScreenOn = false
            if (activity != null) WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }
    BackHandler(browserFullscreen != null) {
        val fullscreen = browserFullscreen
        browserFullscreen = null
        fullscreen?.view?.keepScreenOn = false
        fullscreen?.callback?.onCustomViewHidden()
    }
    BackHandler(browserFullscreen == null && playingMedia != null) { playingMedia = null }
    BackHandler(
        enabled = browserFullscreen == null && playingMedia == null &&
            (tab != Tab.Browser || browserSession.selected.webView?.canGoBack() == true || browserSession.selected.canBack)
    ) {
        if (tab != Tab.Browser) {
            animateTabTransition = true
            tab = Tab.Browser
        }
        else browserSession.selected.webView?.goBack()
    }
    DisposableEffect(browserSession, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) browserSession.persist() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            browserSession.destroy()
        }
    }
}

@Composable
private fun AmbientField() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(-0.15f, 0.16f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "drift")
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(Brush.radialGradient(listOf(Acid.copy(.075f), Color.Transparent)), size.minDimension * .55f, Offset(size.width * (.12f + drift), size.height * .08f))
        drawCircle(Brush.radialGradient(listOf(Blue.copy(.06f), Color.Transparent)), size.minDimension * .7f, Offset(size.width * .9f, size.height * (.7f - drift)))
    }
}

@Composable
private fun NoirTabBar(tab: Tab, onSelect: (Tab, Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var snapIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(tab, snapIndicator) {
        if (snapIndicator) {
            delay(16L)
            snapIndicator = false
        }
    }
    Box(modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).height(64.dp).fillMaxWidth()
        .shadow(
            elevation = 20.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0x995B606B),
            spotColor = Color(0xCC5B606B)
        )
        .background(Color(0xED1C1C1E), shape)
        .border(1.dp, Color.White.copy(.12f), shape)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / 4
            val density = LocalDensity.current
            val itemWidthPx = with(density) { itemWidth.toPx() }
            val settledOffset by animateDpAsState(
                targetValue = itemWidth * tab.ordinal,
                animationSpec = if (snapIndicator) tween(0) else spring(dampingRatio = .68f, stiffness = 390f),
                label = "tab-indicator"
            )
            val dragOffset = with(density) { dragOffsetPx.toDp() }
            val indicatorOffset = if (dragging) itemWidth * tab.ordinal + dragOffset else settledOffset
            val stretch = 1f + (abs(dragOffsetPx) / itemWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f) * .14f
            val liquidShape = RoundedCornerShape(17.dp)
            Box(
                Modifier.fillMaxSize().pointerInput(itemWidthPx, tab) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            dragOffsetPx = 0f
                        },
                        onDragEnd = {
                            val target = (tab.ordinal + dragOffsetPx / itemWidthPx.coerceAtLeast(1f))
                                .roundToInt().coerceIn(0, Tab.entries.lastIndex)
                            snapIndicator = true
                            dragging = false
                            dragOffsetPx = 0f
                            onSelect(Tab.entries[target], true)
                        },
                        onHorizontalDrag = { _, amount ->
                            val minimum = -tab.ordinal * itemWidthPx
                            val maximum = (Tab.entries.lastIndex - tab.ordinal) * itemWidthPx
                            dragOffsetPx = (dragOffsetPx + amount).coerceIn(minimum, maximum)
                        }
                    )
                }
            ) {
                Box(
                    Modifier.offset(x = indicatorOffset).width(itemWidth).fillMaxHeight().padding(horizontal = 2.dp)
                        .graphicsLayer {
                            scaleX = stretch
                        }
                        .shadow(
                            elevation = if (dragging) 6.dp else 4.dp,
                            shape = liquidShape,
                            clip = false,
                            ambientColor = Color.Black.copy(.24f),
                            spotColor = Color.Black.copy(.34f)
                        )
                        .background(Acid.copy(alpha = .10f), liquidShape)
                        .border(1.dp, Acid.copy(alpha = .18f), liquidShape)
                )
                Row(Modifier.fillMaxSize()) {
                    TabItem(Glyph.Link, tab == Tab.UrlDownload, Modifier.weight(1f)) { onSelect(Tab.UrlDownload, true) }
                    TabItem(Glyph.Compass, tab == Tab.Browser, Modifier.weight(1f)) { onSelect(Tab.Browser, true) }
                    TabItem(Glyph.Stack, tab == Tab.Library, Modifier.weight(1f)) { onSelect(Tab.Library, true) }
                    TabItem(Glyph.User, tab == Tab.MyPage, Modifier.weight(1f)) { onSelect(Tab.MyPage, true) }
                }
            }
        }
    }
}

@Composable
private fun TabItem(icon: Glyph, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val selectedShape = RoundedCornerShape(17.dp)
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = .68f, stiffness = 520f),
        label = "tab-icon"
    )
    Box(
        modifier.fillMaxHeight().padding(horizontal = 2.dp)
            .clip(selectedShape)
            .noirClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.scale(iconScale)) {
            GlyphIcon(icon, if (selected) Acid else Muted, if (selected) 20.dp else 19.dp)
        }
    }
}

@Composable
private fun UrlDownloadScreen(onDownload: (String, String, List<String>) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val analysis by DownloadsStore.urlAnalysis.collectAsState()
    val imeVisible = WindowInsets.isImeVisible
    val normalized = remember(input) { normalizeDirectDownloadUrl(input) }
    val analyzing = analysis?.running == true

    val analyze = {
        val url = normalizeDirectDownloadUrl(input)
        if (url == null) error = tr("正しいURLを入力してください", "Enter a valid URL")
        else {
            input = url
            error = null
            focus.clearFocus()
            DownloadsStore.analyzeUrl(context, url)
        }
    }

    LaunchedEffect(analysis) {
        val result = analysis ?: return@LaunchedEffect
        if (!result.running && result.browserProbeFinished) {
            val downloadableItems = result.items.filterNot { item ->
                isLikelyVideoAdUrl(item.url) || item.title.trim().lowercase() in setOf("advertisement", "広告", "ad")
            }
            if (downloadableItems.isNotEmpty()) {
                val pageThumbnail = pageMediaCandidate(result.url, "").thumbnailUrl
                val candidates = downloadableItems.map { item ->
                    val itemThumbnail = pageMediaCandidate(item.url, "").thumbnailUrl
                    DetectedMedia(item.url, item.title, item.thumbnailUrl ?: itemThumbnail ?: pageThumbnail, item.durationSeconds, item.sizeBytes)
                }
                val single = downloadableItems.singleOrNull()
            pendingDownload = PendingDownload(single?.title ?: tr("${downloadableItems.size}件の動画", "${downloadableItems.size} videos"), result.url, candidates)
            } else {
                // yt-dlp の事前解析が失敗しても、サービス側の HTML / manifest
                // フォールバックを試せるよう確認画面へ進める。
                val host = runCatching { Uri.parse(result.url).host.orEmpty().removePrefix("www.") }
                .getOrDefault("").ifBlank { tr("動画", "Video") }
                val candidate = pageMediaCandidate(result.url, host)
                pendingDownload = PendingDownload(candidate.title ?: host, result.url, listOf(candidate))
                error = null
            }
            DownloadsStore.clearUrlAnalysis(result)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = if (imeVisible) 20.dp else 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Label(tr("URLからダウンロード", "Download from URL"), 27.sp, Acid, FontWeight.Black, letterSpacing = -.5f)
                Spacer(Modifier.height(5.dp))
                Label(tr("動画ページや動画ファイルのURLを入力してください", "Enter a video page or media file URL"), 12.sp, Muted)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel)
                    .border(1.dp, Color.White.copy(.08f), RoundedCornerShape(20.dp)).padding(14.dp)
            ) {
                Label("URL", 11.sp, Muted, FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(Ink)
                        .border(1.dp, if (error == null) Color.White.copy(.08f) else Red.copy(.55f), RoundedCornerShape(14.dp)).padding(start = 13.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphIcon(Glyph.Link, Muted, 18.dp)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        input, { input = it; error = null }, Modifier.weight(1f), singleLine = true,
                        textStyle = TextStyle(Paper, 14.sp, fontWeight = FontWeight.Medium),
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { analyze() }),
                        cursorBrush = Brush.verticalGradient(listOf(Acid, Acid)),
                        decorationBox = { inner -> Box { if (input.isBlank()) Label("https://example.com/video", 13.sp, Muted); inner() } }
                    )
                    if (input.isNotBlank()) Box(Modifier.size(30.dp).noirClick { input = ""; error = null }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 12.dp) }
                }
                AnimatedVisibility(error != null) {
                    error?.let { Label(it, 10.sp, Red, FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, top = 7.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(13.dp)).background(Elevated)
                        .noirClick {
                            clipboard.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let { input = it; error = null }
                        }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphIcon(Glyph.Clipboard, Muted, 17.dp); Spacer(Modifier.width(9.dp))
                        Label(tr("クリップボードから貼り付け", "Paste from clipboard"), 11.sp, Paper, FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (normalized != null) Acid else Color(0xFF303037))
                        .noirClick(enabled = !analyzing) { analyze() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    if (analyzing) AnalysisSpinner() else GlyphIcon(Glyph.Search, if (normalized != null) Ink else Muted, 17.dp)
                    Spacer(Modifier.width(8.dp))
                    Label(if (analyzing) tr("解析しています", "Analyzing") else tr("動画を確認", "Check video"), 12.sp, if (normalized != null) Ink else Paper, FontWeight.Black)
                }
            }
        }
        item {
            DownloaderCapabilityPanel()
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp), verticalAlignment = Alignment.Top) {
                GlyphIcon(Glyph.Info, Muted, 15.dp); Spacer(Modifier.width(9.dp))
                Label(tr("動画が複数見つかった場合は、ダウンロードする動画を選べます。", "If multiple videos are found, you can choose which one to download."), 11.sp, Muted, maxLines = 2)
            }
        }
    }

    pendingDownload?.let { target ->
        DownloadConfirmation(target, onDismiss = { pendingDownload = null }) { selectedUrl ->
            pendingDownload = null
            val selected = target.mediaCandidates.firstOrNull { it.url == selectedUrl }
            val chosenUrl = selectedUrl ?: target.mediaCandidates.singleOrNull()?.url ?: target.pageUrl
            onDownload(chosenUrl, selected?.title ?: target.title, listOf(chosenUrl))
        }
    }

}

private const val URL_PROBE_PLAY_SCRIPT = """
    (() => {
      const start = () => document.querySelectorAll('video, audio').forEach(media => {
        try {
          media.muted = true;
          media.defaultMuted = true;
          media.volume = 0;
          media.autoplay = true;
          media.preload = 'auto';
          media.setAttribute('muted', '');
          media.setAttribute('playsinline', '');
          const promise = media.play();
          if (promise && promise.catch) promise.catch(() => {});
        } catch (_) {}
      });
      start();
      if (!window.__noirProbeObserver) {
        window.__noirProbeObserver = new MutationObserver(start);
        window.__noirProbeObserver.observe(document.documentElement, {
          childList: true, subtree: true, attributes: true, attributeFilter: ['src']
        });
        window.__noirProbeTimer = setInterval(start, 500);
      }
    })();
"""

private const val URL_PROBE_STOP_SCRIPT = """
    (() => {
      document.querySelectorAll('video, audio').forEach(media => {
        try { media.pause(); media.muted = true; } catch (_) {}
      });
      if (window.__noirProbeTimer) {
        clearInterval(window.__noirProbeTimer);
        window.__noirProbeTimer = null;
      }
      if (window.__noirProbeObserver) {
        window.__noirProbeObserver.disconnect();
        window.__noirProbeObserver = null;
      }
      if (window.__noirVideoAdTimer) {
        clearInterval(window.__noirVideoAdTimer);
        window.__noirVideoAdTimer = null;
      }
    })();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun UrlAnalysisProbe(url: String, cosmeticFilteringEnabled: Boolean, onResult: (List<DetectedMedia>) -> Unit) {
    var completed by remember(url) { mutableStateOf(false) }
    var probeView by remember(url) { mutableStateOf<WebView?>(null) }
    val resources = remember(url) { LinkedHashSet<String>() }

    fun finish(view: WebView) {
        if (completed) return
        completed = true
        view.evaluateJavascript(PAGE_MEDIA_ANALYSIS_SCRIPT) { result ->
            view.evaluateJavascript(URL_PROBE_STOP_SCRIPT, null)
            val analysis = parsePageMediaAnalysis(result)
            val titledItems = (analysis.candidates + resources.map(::DetectedMedia)).map { item ->
                if (item.title.isNullOrBlank()) item.copy(title = analysis.pageTitle) else item
            }.ifEmpty { listOf(DetectedMedia(url, analysis.pageTitle)) }
            onResult(titledItems
                .filterNot { isLikelyVideoAdUrl(it.url) }
                .distinctBy { it.url })
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                probeView = this
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                configureAsBrowser(allowAutoplay = true)
                settings.loadsImagesAutomatically = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                        if (WebAdBlocker.shouldBlock(request, url, blockVideoAds = true)) WebAdBlocker.emptyResponse() else null

                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                        if (cosmeticFilteringEnabled) installEasyListCosmeticFilters(view, loadedUrl)
                        installPageAdSuppression(view)
                        view.evaluateJavascript(URL_PROBE_PLAY_SCRIPT, null)
                        view.postDelayed({
                            if (!completed) {
                                installPageAdSuppression(view)
                                view.evaluateJavascript(URL_PROBE_PLAY_SCRIPT, null)
                            }
                        }, 2_000L)
                        view.postDelayed({ finish(view) }, 6_000L)
                    }

                    override fun onLoadResource(view: WebView, resourceUrl: String) {
                        if (isDirectMediaUrl(resourceUrl) && !isLikelyVideoAdUrl(resourceUrl)) resources += resourceUrl
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.size(2.dp).alpha(.01f)
    )

    LaunchedEffect(url) {
        delay(10_000L)
        val view = probeView
        if (view != null) finish(view) else if (!completed) {
            completed = true
            onResult(emptyList())
        }
    }
    DisposableEffect(url) {
        onDispose {
            probeView?.apply {
                evaluateJavascript(URL_PROBE_STOP_SCRIPT, null)
                stopLoading()
                destroy()
            }
            probeView = null
        }
    }
}

@Composable
private fun DecorativeUrlDownloadScreen(onDownload: (String, String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    val context = LocalContext.current
    val urlAnalysis by DownloadsStore.urlAnalysis.collectAsState()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val normalized = remember(input) { normalizeDirectDownloadUrl(input) }
    val ready = normalized != null
    val analyzing = urlAnalysis?.running == true
    val host = remember(normalized) { normalized?.let { Uri.parse(it).host.orEmpty().removePrefix("www.") }.orEmpty() }
    val buttonScale by animateFloatAsState(if (ready) 1f else .985f, spring(dampingRatio = .72f), label = "urlReady")
    val pulse = rememberInfiniteTransition(label = "urlPulse")
    val pulseValue by pulse.animateFloat(.35f, 1f, infiniteRepeatable(tween(1_700), RepeatMode.Reverse), label = "urlPulseValue")
    val analyze = {
        val url = normalizeDirectDownloadUrl(input)
        if (url == null) {
            error = tr("有効な動画ページまたはメディアURLを入力してください", "Enter a valid video page or media URL")
        } else {
            input = url
            error = null
            focus.clearFocus()
            DownloadsStore.analyzeUrl(context, url)
        }
    }

    LaunchedEffect(urlAnalysis) {
        val result = urlAnalysis ?: return@LaunchedEffect
        if (!result.running) {
            if (result.items.isNotEmpty()) {
                val candidates = result.items.map { item ->
                    DetectedMedia(item.url, item.title, item.thumbnailUrl, item.durationSeconds, item.sizeBytes)
                }
                val single = result.items.singleOrNull()
                pendingDownload = PendingDownload(
                    title = single?.title ?: tr("${result.items.size}件の動画を検出", "${result.items.size} videos detected"),
                    pageUrl = single?.url ?: result.url,
                    mediaCandidates = candidates
                )
            } else {
                error = result.error ?: tr("このURLから動画を検出できませんでした", "No video could be detected from this URL")
            }
            DownloadsStore.clearUrlAnalysis(result)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().imePadding()
            .then(if (imeVisible) Modifier else Modifier.navigationBarsPadding()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 17.dp, end = 17.dp, top = 16.dp, bottom = if (imeVisible) 18.dp else 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Acid, CircleShape))
                        Spacer(Modifier.width(7.dp)); Label("URL LAB", 10.sp, Acid, FontWeight.Black, letterSpacing = 2.3f)
                    }
            Spacer(Modifier.height(3.dp)); Label(tr("ダイレクト解析", "Direct analysis"), 30.sp, Paper, FontWeight.Black, letterSpacing = -.8f)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Label("3 ENGINES", 9.sp, Blue, FontWeight.Bold, letterSpacing = 1.1f)
                    Spacer(Modifier.height(3.dp)); Label("AUTO ROUTING", 8.sp, Muted, FontWeight.Bold, letterSpacing = .9f)
                }
            }
        }

        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF172A4F), Color(0xFF101725), Color(0xFF111114))))
                    .border(1.dp, Acid.copy(if (ready) .42f else .2f), RoundedCornerShape(30.dp))
            ) {
                Canvas(Modifier.fillMaxSize().alpha(.8f)) {
                    drawCircle(Acid.copy(.06f * pulseValue), size.minDimension * .55f, Offset(size.width * .9f, size.height * .05f))
                    drawCircle(Blue.copy(.04f), size.minDimension * .35f, Offset(size.width * .05f, size.height * .98f))
                }
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(49.dp).clip(RoundedCornerShape(17.dp))
                                .background(if (ready) Acid else Color.White.copy(.07f)),
                            contentAlignment = Alignment.Center
                        ) { GlyphIcon(Glyph.Link, if (ready) Ink else Muted, 24.dp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                    Label(if (analyzing) tr("動画を検索中", "Searching for video") else if (ready) tr("解析準備完了", "Ready to analyze") else tr("URLを待っています", "Waiting for a URL"), 15.sp, Paper, FontWeight.Black)
                    Spacer(Modifier.height(2.dp)); Label(if (analyzing) tr("yt-dlpでメタデータを解析しています", "Analyzing metadata with yt-dlp") else if (ready) host else tr("動画ページ・MP4・HLS・DASH", "Video pages · MP4 · HLS · DASH"), 10.sp, if (ready || analyzing) Blue else Muted, FontWeight.Bold, maxLines = 1)
                        }
                        Box(
                            Modifier.height(26.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (ready) Acid.copy(.16f) else Color.White.copy(.05f)).padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center
                        ) { Label(if (analyzing) "SCAN" else if (ready) "READY" else "STANDBY", 8.sp, if (ready || analyzing) Acid else Muted, FontWeight.Black, letterSpacing = 1f) }
                    }

                    Spacer(Modifier.height(19.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(.32f))
                            .border(1.dp, if (error == null) Color.White.copy(.08f) else Red.copy(.6f), RoundedCornerShape(20.dp)).padding(7.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().height(48.dp).padding(start = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlyphIcon(Glyph.Link, if (error == null) Acid else Red, 18.dp)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = input,
                                onValueChange = { input = it; error = null },
                                singleLine = true,
                                textStyle = TextStyle(Paper, 14.sp, fontWeight = FontWeight.Medium),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { analyze() }),
                                cursorBrush = Brush.verticalGradient(listOf(Acid, Acid)),
                                modifier = Modifier.weight(1f),
                        decorationBox = { inner -> Box { if (input.isBlank()) Label(tr("URLを入力または貼り付け", "Enter or paste a URL"), 13.sp, Muted); inner() } }
                            )
                            if (input.isNotBlank()) Box(Modifier.size(30.dp).noirClick { input = ""; error = null }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 12.dp) }
                        }
                        Row(
                            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.055f))
                                .noirClick {
                                    clipboard.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let { input = it; error = null }
                                }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlyphIcon(Glyph.Clipboard, Acid, 17.dp); Spacer(Modifier.width(8.dp))
                        Label(tr("クリップボードから貼り付け", "Paste from clipboard"), 11.sp, Paper, FontWeight.Bold)
                            Spacer(Modifier.weight(1f)); GlyphIcon(Glyph.Plus, Muted, 13.dp)
                        }
                    }

                    AnimatedVisibility(error != null, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
                        error?.let { Row(Modifier.padding(start = 5.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(5.dp).background(Red, CircleShape)); Spacer(Modifier.width(7.dp)); Label(it, 10.sp, Red, FontWeight.Medium)
                        } }
                    }
                    Spacer(Modifier.height(13.dp))
                    Row(
                        Modifier.fillMaxWidth().height(55.dp).scale(buttonScale).clip(RoundedCornerShape(18.dp))
                            .background(if (ready) Brush.linearGradient(listOf(Acid, Color(0xFF72B7FF))) else Brush.linearGradient(listOf(Color(0xFF303039), Color(0xFF26262C))))
                            .noirClick(enabled = !analyzing) { analyze() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Label(if (analyzing) tr("解析しています", "Analyzing") else tr("解析を開始", "Start analysis"), 13.sp, if (ready) Ink else Paper, FontWeight.Black)
                        Spacer(Modifier.width(9.dp)); if (analyzing) AnalysisSpinner() else GlyphIcon(Glyph.Go, if (ready) Ink else Muted, 17.dp)
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Panel).border(1.dp, Color.White.copy(.065f), RoundedCornerShape(23.dp)).padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label("AUTO FLOW", 9.sp, Acid, FontWeight.Black, letterSpacing = 1.5f)
                Spacer(Modifier.weight(1f)); Label(tr("最適な取得方法を自動選択", "Automatically selects the best method"), 9.sp, Muted, FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UrlPipelineStep("01", tr("解析", "Analyze"), Glyph.Search, Modifier.weight(1f))
                    PipelineConnector()
                UrlPipelineStep("02", tr("選択", "Select"), Glyph.Check, Modifier.weight(1f))
                    PipelineConnector()
                UrlPipelineStep("03", tr("保存", "Save"), Glyph.ArrowDown, Modifier.weight(1f))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CapabilityChip("yt-dlp", Glyph.Link)
                CapabilityChip("DIRECT", Glyph.ArrowDown)
                CapabilityChip("HLS / DASH", Glyph.Play)
                CapabilityChip("FFmpeg", Glyph.Refresh)
            }
        }
    }

    pendingDownload?.let { target ->
        DownloadConfirmation(target, onDismiss = { pendingDownload = null }) { selectedUrl ->
            pendingDownload = null
            val selected = target.mediaCandidates.firstOrNull { it.url == selectedUrl }
            onDownload(selectedUrl ?: target.pageUrl, selected?.title ?: target.title)
        }
    }
}

@Composable
private fun DownloaderCapabilityPanel() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(18.dp)
    ) {
            Label(tr("URLダウンロードについて", "About URL downloads"), 10.sp, Muted, FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
            Label(tr("URLを貼るだけ。あとはおまかせ。", "Just paste a URL. We'll handle the rest."), 20.sp, Paper, FontWeight.Black, maxLines = 2)
        Spacer(Modifier.height(17.dp))

        CapabilityDescriptionRow(
            glyph = Glyph.Check,
            title = tr("うまくいく方法を自動で探します", "Automatically finds a method that works"),
            detail = tr("ひとつの方法で保存できなくても、別の方法を順番に試します。", "If one method fails, other methods are tried in sequence.")
        )
        Box(Modifier.fillMaxWidth().padding(start = 43.dp, top = 14.dp, bottom = 14.dp).height(1.dp).background(Line.copy(.55f)))
        CapabilityDescriptionRow(
            glyph = Glyph.Compass,
            title = tr("幅広いサイトに対応しています", "Works with a wide range of sites"),
            detail = tr("動画サイトやSNS、ページの中に埋め込まれた動画にも対応します。", "Supports video sites, social media, and videos embedded in pages."),
            examples = listOf("YouTube", "X (Twitter)", "bilibili", "Niconico")
        )
        Box(Modifier.fillMaxWidth().padding(start = 43.dp, top = 14.dp, bottom = 14.dp).height(1.dp).background(Line.copy(.55f)))
        CapabilityDescriptionRow(
            glyph = Glyph.ArrowDown,
            title = tr("難しい設定はいりません", "No complicated setup"),
            detail = tr("動画を見つけて、保存できる形に整えるところまで自動で行います。", "Video detection and preparation for saving are handled automatically.")
        )
        Box(Modifier.fillMaxWidth().padding(start = 43.dp, top = 14.dp, bottom = 14.dp).height(1.dp).background(Line.copy(.55f)))
        CapabilityDescriptionRow(
            glyph = Glyph.Lock,
            title = tr("端末の中で解析して保存します", "Analyzed and saved on your device"),
            detail = tr("URLや動画をダウンロード代行サーバーへ送らず、この端末内で処理するためプライバシーにも安心です。", "URLs and videos are processed on this device without sending them to a download proxy server.")
        )
    }
}

@Composable
private fun CapabilityDescriptionRow(
    glyph: Glyph,
    title: String,
    detail: String,
    examples: List<String> = emptyList()
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(31.dp).clip(CircleShape).background(Color.White.copy(.055f)), contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, Acid, 14.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Label(title, 12.sp, Paper, FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Label(detail, 10.sp, Muted, FontWeight.Normal, maxLines = 2)
            if (examples.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    examples.forEach { example ->
                        Box(
                            Modifier.clip(RoundedCornerShape(9.dp)).background(Color.White.copy(.055f))
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Label(example, 9.sp, Paper.copy(.82f), FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlPipelineStep(number: String, title: String, glyph: Glyph, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Acid.copy(.1f)), contentAlignment = Alignment.Center) { GlyphIcon(glyph, Acid, 18.dp) }
        Spacer(Modifier.height(7.dp)); Label(number, 8.sp, Muted, FontWeight.Bold, letterSpacing = 1f)
        Spacer(Modifier.height(2.dp)); Label(title, 11.sp, Paper, FontWeight.Bold)
    }
}

@Composable
private fun PipelineConnector() {
    Box(Modifier.width(22.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Line, Acid.copy(.5f), Line))))
}

@Composable
private fun CapabilityChip(text: String, glyph: Glyph) {
    Row(Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(Elevated).border(1.dp, Color.White.copy(.06f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(glyph, Muted, 13.dp); Spacer(Modifier.width(6.dp)); Label(text, 9.sp, Muted, FontWeight.Bold, letterSpacing = .5f)
    }
}

@Composable
private fun LegacyUrlDownloadScreen(onDownload: (String, String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val analyze = {
        val url = normalizeDirectDownloadUrl(input)
        if (url == null) {
            error = tr("http:// または https:// で始まるURLを入力してください", "Enter a URL beginning with http:// or https://")
        } else {
            input = url
            error = null
            focus.clearFocus()
            val host = Uri.parse(url).host.orEmpty().removePrefix("www.")
            pendingDownload = PendingDownload(
                title = host.ifBlank { tr("URLから取得する動画", "Video from URL") },
                pageUrl = url,
                mediaCandidates = emptyList()
            )
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding()
            .then(if (imeVisible) Modifier else Modifier.navigationBarsPadding())
            .padding(start = 18.dp, end = 18.dp, bottom = if (imeVisible) 12.dp else 96.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label("DIRECT ANALYZER", 10.sp, Acid, FontWeight.Bold, letterSpacing = 2.2f)
            Spacer(Modifier.height(3.dp)); Label(tr("URLダウンロード", "URL download"), 29.sp, Paper, FontWeight.Black, letterSpacing = -.6f)
            }
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(Acid.copy(.13f)).border(1.dp, Acid.copy(.26f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                GlyphIcon(Glyph.Link, Acid, 22.dp)
            }
        }

        Spacer(Modifier.weight(.72f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(82.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Acid, Color(0xFF79B9FF))))
                    .graphicsLayer { shadowElevation = 26.dp.toPx() },
                contentAlignment = Alignment.Center
            ) { GlyphIcon(Glyph.Link, Ink, 37.dp) }
            Spacer(Modifier.height(20.dp))
                    Label(tr("動画URLを解析", "Analyze video URL"), 23.sp, Paper, FontWeight.Black)
            Spacer(Modifier.height(7.dp))
                    Label(tr("動画ページ、MP4、HLSなどのURLを貼り付けてください", "Paste a video page, MP4, HLS, or other media URL"), 11.sp, Muted, FontWeight.Medium)
        }
        Spacer(Modifier.height(28.dp))

        Row(
            Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(Elevated)
                .border(1.dp, if (error == null) Color.White.copy(.09f) else Red.copy(.55f), RoundedCornerShape(20.dp)).padding(start = 16.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphIcon(Glyph.Link, if (error == null) Muted else Red, 19.dp)
            Spacer(Modifier.width(11.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it; error = null },
                singleLine = true,
                textStyle = TextStyle(Paper, 14.sp, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { analyze() }),
                cursorBrush = Brush.verticalGradient(listOf(Acid, Acid)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (input.isBlank()) Label("https://example.com/video", 13.sp, Muted); inner() } }
            )
            if (input.isNotBlank()) {
                Box(Modifier.size(31.dp).noirClick { input = ""; error = null }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 13.dp) }
            }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.06f)).noirClick {
                clipboard.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let { input = it; error = null }
            }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Clipboard, Acid, 18.dp) }
        }
        AnimatedVisibility(error != null, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
            error?.let { Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(Red, CircleShape)); Spacer(Modifier.width(7.dp)); Label(it, 10.sp, Red, FontWeight.Medium)
            } }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Acid, Color(0xFF68AEFF))))
                .noirClick { analyze() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
                    Label(tr("URLを解析する", "Analyze URL"), 13.sp, Ink, FontWeight.Black)
            Spacer(Modifier.width(9.dp)); GlyphIcon(Glyph.Go, Ink, 17.dp)
        }

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(31.dp).clip(RoundedCornerShape(11.dp)).background(Acid.copy(.1f)), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Info, Acid, 15.dp) }
            Spacer(Modifier.width(10.dp))
                Label(tr("yt-dlpで解析し、非対応なら直接取得とFFmpegを自動で試します", "Uses yt-dlp, then automatically tries direct download and FFmpeg if needed"), 10.sp, Muted, FontWeight.Medium, maxLines = 2)
        }
    }

    pendingDownload?.let { target ->
        DownloadConfirmation(
            target = target,
            onDismiss = { pendingDownload = null },
            onConfirm = { selectedUrl ->
                pendingDownload = null
                val selected = target.mediaCandidates.firstOrNull { it.url == selectedUrl }
                onDownload(selectedUrl ?: target.pageUrl, selected?.title ?: target.title)
            }
        )
    }
}

private data class PendingDownload(
    val title: String,
    val pageUrl: String,
    val mediaCandidates: List<DetectedMedia>,
    val siteDirectDownload: Boolean = false
) {
    val directMediaUrls: List<String> get() = mediaCandidates.map { it.url }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureAsBrowser(allowAutoplay: Boolean) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mediaPlaybackRequiresUserGesture = !allowAutoplay
    settings.javaScriptCanOpenWindowsAutomatically = true
    settings.setSupportMultipleWindows(true)
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    settings.allowContentAccess = true
    settings.loadsImagesAutomatically = true
    settings.userAgentString = settings.userAgentString
        .replace("; wv", "")
        .replace(Regex("\\sVersion/4\\.0"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@configureAsBrowser, true)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserScreen(
    session: BrowserSession,
    searchEngine: SearchEngine,
    onShowFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideFullscreen: () -> Unit,
    onDownload: (String, String, List<String>, Boolean, String) -> Unit
) {
    val selected = session.selected
    val context = LocalContext.current
    var address by remember(selected.id) { mutableStateOf(selected.url) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var siteDownloadRequestSerial by remember(selected.id) { mutableIntStateOf(0) }
    var siteDownloadWasRunning by remember(selected.id) { mutableStateOf(false) }
    val activeDownloads by DownloadsStore.active.collectAsState()
    val focus = LocalFocusManager.current
    val siteDownloadHandler: (String, String?, String?, String?, Long) -> Unit =
        { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
            siteDownloadRequestSerial++
            val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
            // WebView's DownloadListener often reports real video responses as
            // application/octet-stream (or with no MIME type at all). Treat those
            // ambiguous download responses as media unless the filename/MIME is
            // clearly a normal document or archive.
            if (isSiteMediaDownload(downloadUrl, mimeType, fileName)) {
                selected.lastSiteDownload = SiteDownloadRequest(downloadUrl, userAgent, contentDisposition, mimeType, contentLength, fileName)
                pendingDownload = PendingDownload(
                    fileName.substringBeforeLast('.').ifBlank { selected.title },
                    selected.url,
                    listOf(DetectedMedia(
                        downloadUrl,
                        fileName,
                        sizeBytes = contentLength.coerceAtLeast(0L),
                        refererUrl = selected.url
                    )),
                    siteDirectDownload = true
                )
            } else {
                enqueueStandardDownload(context, downloadUrl, userAgent, contentDisposition, mimeType)
            }
        }
    selected.siteDownloadHandler = siteDownloadHandler
    LaunchedEffect(siteDownloadRequestSerial, activeDownloads.map { Triple(it.id, it.running, it.error) }) {
        if (siteDownloadRequestSerial == 0) return@LaunchedEffect
        if (activeDownloads.any { it.running }) siteDownloadWasRunning = true
        if (siteDownloadWasRunning && activeDownloads.none { it.running }) {
            delay(250)
            selected.webView?.apply {
                setDownloadListener { url, userAgent, disposition, mime, length ->
                    selected.siteDownloadHandler?.invoke(url, userAgent, disposition, mime, length)
                }
                evaluateJavascript(SITE_DOWNLOAD_REARM_SCRIPT, null)
                selected.lastSiteDownload?.url?.let { replayUrl ->
                    val encodedUrl = org.json.JSONObject.quote(replayUrl)
                    evaluateJavascript(
                        """
                        (() => document.querySelectorAll('[data-noir-download-rearmed-button="1"]').forEach(node => {
                          if (node.dataset.noirReplayBound) return;
                          node.dataset.noirReplayBound = '1';
                          node.addEventListener('click', event => {
                            event.preventDefault();
                            event.stopImmediatePropagation();
                            window.location.assign($encodedUrl);
                          }, true);
                        }))()
                        """.trimIndent(),
                        null
                    )
                }
            }
            siteDownloadWasRunning = false
            siteDownloadRequestSerial = 0
        }
    }
    val navigate = {
        val next = normalizeUrl(address, searchEngine)
        selected.url = next
        selected.webView?.loadUrl(next)
        focus.clearFocus()
    }

    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding()
            .then(if (imeVisible) Modifier else Modifier.navigationBarsPadding())
            .padding(bottom = if (imeVisible) 6.dp else 82.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(start = 17.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(tr("ブラウザ", "Browser"), 19.sp, Acid, FontWeight.Black, letterSpacing = -.2f)
            Spacer(Modifier.weight(1f))
                Label(tr("${session.tabs.size}個のタブ", "${session.tabs.size} tabs"), 10.sp, Muted, FontWeight.Medium)
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(.055f)).noirClick { session.add(searchEngine.homeUrl) }, contentAlignment = Alignment.Center) {
                GlyphIcon(Glyph.Plus, Paper, 17.dp)
            }
        }

        BrowserTabRail(session, searchEngine)

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 7.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(18.dp)).background(Panel)) {
            key(selected.id) {
                AndroidView(
                    factory = { context ->
                        selected.webView?.also { (it.parent as? ViewGroup)?.removeView(it) } ?: WebView(context).apply {
                            setBackgroundColor(android.graphics.Color.rgb(17, 17, 20))
                            configureAsBrowser(allowAutoplay = false)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                                selected.siteDownloadHandler?.invoke(downloadUrl, userAgent, contentDisposition, mimeType, contentLength)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val target = request.url.toString()
                                    val previous = selected.lastSiteDownload
                                    val guessedName = previous?.takeIf { it.url == target }?.fileName
                                        ?: URLUtil.guessFileName(target, null, null)
                                    if ((previous?.url == target || isVideoDownload(target, null, guessedName)) &&
                                        (target.startsWith("http://") || target.startsWith("https://"))) {
                                        selected.siteDownloadHandler?.invoke(
                                            target,
                                            previous?.userAgent ?: request.requestHeaders["User-Agent"],
                                            previous?.contentDisposition,
                                            previous?.mimeType,
                                            previous?.contentLength ?: 0L
                                        )
                                        return true
                                    }
                                    return when (request.url.scheme?.lowercase()) {
                                        "http", "https", "about", "data", "blob" -> false
                                        else -> {
                                            openExternal(view.context, target)
                                            true
                                        }
                                    }
                                }
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                    return if (session.adBlockEnabled && WebAdBlocker.shouldBlock(
                                            request, selected.url, session.videoAdBlockEnabled
                                        )) {
                                        WebAdBlocker.emptyResponse()
                                    } else null
                                }
                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    selected.analysisToken++
                                    selected.url = url
                                    selected.progress = 1
                                    selected.mediaAvailability = MediaAvailability.Analyzing
                                    selected.mediaCandidates.clear()
                                    address = url
                                    selected.canBack = view.canGoBack()
                                    selected.canForward = view.canGoForward()
                                }
                                override fun onPageFinished(view: WebView, url: String) {
                                    selected.analysisToken++
                                    selected.url = url
                                    address = url
                                    selected.title = view.title?.takeIf { it.isNotBlank() } ?: Uri.parse(url).host.orEmpty()
                                    selected.canBack = view.canGoBack()
                                    selected.canForward = view.canGoForward()
                                    CookieManager.getInstance().flush()
                                    if (session.cosmeticFilteringEnabled) installEasyListCosmeticFilters(view, url)
                                    if (session.adBlockEnabled && session.videoAdBlockEnabled) installPageAdSuppression(view)
                                    analyzeBrowserPage(view, selected, url)
                                    view.postDelayed({
                                        if (view.url == url) {
                                            if (session.cosmeticFilteringEnabled) installEasyListCosmeticFilters(view, url)
                                            if (session.adBlockEnabled && session.videoAdBlockEnabled) installPageAdSuppression(view)
                                            analyzeBrowserPage(view, selected, url)
                                        }
                                    }, 1_500L)
                                    view.postDelayed({
                                        if (view.url == url && session.cosmeticFilteringEnabled) installEasyListCosmeticFilters(view, url)
                                    }, 4_000L)
                                }
                                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                                    if (url.startsWith("http") && url != selected.url) {
                                        selected.url = url
                                        address = url
                                        scheduleStableUrlAnalysis(view, selected, url)
                                    }
                                }
                                override fun onLoadResource(view: WebView, url: String) {
                                    view.url?.takeIf { it.startsWith("http") && it != selected.url }?.let { currentUrl ->
                                        selected.url = currentUrl
                                        address = currentUrl
                                        scheduleStableUrlAnalysis(view, selected, currentUrl)
                                    }
                                    if (isDirectMediaUrl(url) && !isLikelyVideoAdUrl(url) && selected.mediaCandidates.none { it.url == url }) {
                                        selected.mediaCandidates += DetectedMedia(url)
                                        selected.mediaAvailability = MediaAvailability.Available
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                                    val popup = WebView(view.context).apply {
                                        configureAsBrowser(allowAutoplay = false)
                                        setDownloadListener { url, userAgent, disposition, mime, length ->
                                            selected.siteDownloadHandler?.invoke(url, userAgent, disposition, mime, length)
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(child: WebView, request: WebResourceRequest): Boolean {
                                                val target = request.url.toString()
                                                val previous = selected.lastSiteDownload
                                                val guessedName = previous?.takeIf { it.url == target }?.fileName
                                                    ?: URLUtil.guessFileName(target, null, null)
                                                if ((previous?.url == target || isVideoDownload(target, null, guessedName)) &&
                                                    (target.startsWith("http://") || target.startsWith("https://"))) {
                                                    selected.siteDownloadHandler?.invoke(
                                                        target,
                                                        previous?.userAgent ?: request.requestHeaders["User-Agent"],
                                                        previous?.contentDisposition,
                                                        previous?.mimeType,
                                                        previous?.contentLength ?: 0L
                                                    )
                                                } else {
                                                    view.loadUrl(target)
                                                }
                                                return true
                                            }

                                            override fun onPageStarted(child: WebView, url: String, favicon: Bitmap?) {
                                                if (url != "about:blank") {
                                                    view.loadUrl(url)
                                                    child.stopLoading()
                                                    child.loadUrl("about:blank")
                                                }
                                            }
                                        }
                                    }
                                    selected.popupWebViews += popup
                                    (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
                                    resultMsg.sendToTarget()
                                    return true
                                }
                                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                    onShowFullscreen(view, callback)
                                }
                                override fun onHideCustomView() {
                                    onHideFullscreen()
                                }
                                override fun onProgressChanged(view: WebView, newProgress: Int) { selected.progress = newProgress }
                                override fun onReceivedTitle(view: WebView, newTitle: String?) {
                                    if (!newTitle.isNullOrBlank()) selected.title = newTitle
                                }
                                override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                                    session.saveFavicon(selected, icon)
                                }
                            }
                            val restored = selected.savedState?.let { restoreState(it) } != null
                            selected.savedState = null
                            if (!restored) loadUrl(normalizeUrl(selected.url, searchEngine))
                            selected.webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (selected.progress in 1..99) Box(Modifier.fillMaxWidth(selected.progress / 100f).height(2.dp).background(Acid))
        }

        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp).fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(Elevated).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(21.dp)).padding(6.dp)) {
            Row(Modifier.height(45.dp).clip(RoundedCornerShape(15.dp)).background(Ink.copy(.62f)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(29.dp), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Lock, Muted, 13.dp) }
                BasicTextField(
                    value = address, onValueChange = { address = it }, singleLine = true,
                    textStyle = TextStyle(Paper, 14.sp, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate() }),
                    modifier = Modifier.weight(1f), cursorBrush = Brush.verticalGradient(listOf(Acid, Acid)),
                            decorationBox = { inner -> Box(Modifier.padding(vertical = 8.dp)) { if (address.isBlank()) Label(tr("検索またはURL", "Search or URL"), 14.sp, Muted); inner() } }
                )
                if (address.isNotEmpty()) Box(Modifier.size(27.dp).noirClick { address = "" }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 11.dp) }
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Acid).noirClick { navigate() }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Go, Ink, 15.dp, 2.2f) }
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CircleAction(Glyph.Back, selected.canBack) { selected.webView?.goBack() }
                CircleAction(Glyph.Forward, selected.canForward) { selected.webView?.goForward() }
                CircleAction(Glyph.Home, true) { selected.webView?.loadUrl(searchEngine.homeUrl) }
                DownloadButton(
                    enabled = selected.mediaAvailability == MediaAvailability.Available,
                    analyzing = selected.mediaAvailability == MediaAvailability.Analyzing
                ) {
                    val webView = selected.webView ?: return@DownloadButton
                    val pageUrl = webView.url ?: return@DownloadButton
                    val pageTitle = selected.title.ifBlank { Uri.parse(pageUrl).host.orEmpty() }
                    val pageThumbnail = pageMediaCandidate(pageUrl, pageTitle).thumbnailUrl
                    val candidates = selected.mediaCandidates.filterNot { isLikelyVideoAdUrl(it.url) }.map { candidate ->
                        if (candidate.thumbnailUrl == null) candidate.copy(thumbnailUrl = pageThumbnail) else candidate
                    }.ifEmpty {
                        listOf(pageMediaCandidate(pageUrl, pageTitle))
                    }
                    pendingDownload = PendingDownload(pageTitle, pageUrl, candidates)
                }
                CircleAction(Glyph.Refresh, true) { selected.webView?.reload() }
                CircleAction(Glyph.External, true) { selected.webView?.url?.let { openExternal(selected.webView!!.context, it) } }
            }
        }
    }
    pendingDownload?.let { target ->
        DownloadConfirmation(
            target = target,
            onDismiss = { pendingDownload = null },
            onConfirm = { selectedUrl ->
                val freshCandidates = selected.mediaCandidates
                    .filterNot { isLikelyVideoAdUrl(it.url) }
                    .map { candidate ->
                        if (candidate.thumbnailUrl == null) candidate.copy(
                            thumbnailUrl = pageMediaCandidate(target.pageUrl, target.title).thumbnailUrl
                        ) else candidate
                    }
                    .distinctBy { it.url }
                if (target.mediaCandidates.size <= 1 && freshCandidates.size > 1) {
                    pendingDownload = target.copy(mediaCandidates = freshCandidates)
                    return@DownloadConfirmation
                }
                pendingDownload = null
                val chosenUrl = selectedUrl ?: target.mediaCandidates.singleOrNull()?.url ?: target.pageUrl
                val chosen = target.mediaCandidates.firstOrNull { it.url == chosenUrl }
                onDownload(
                    chosenUrl,
                    chosen?.title ?: target.title,
                    listOf(chosenUrl),
                    target.siteDirectDownload,
                    target.pageUrl
                )
            }
        )
    }
}

@Composable
private fun DownloadConfirmation(target: PendingDownload, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    val host = remember(target.pageUrl) { Uri.parse(target.pageUrl).host.orEmpty().removePrefix("www.") }
    var selectedUrl by remember(target) {
        mutableStateOf(target.directMediaUrls.firstOrNull().takeIf { target.directMediaUrls.size > 1 })
    }
    val context = LocalContext.current
    LaunchedEffect(target.mediaCandidates) {
        target.mediaCandidates.forEach {
            RemoteThumbnailCache.request(context, it.copy(refererUrl = it.refererUrl ?: target.pageUrl))
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color(0xFF17171B))
                .border(1.dp, Color.White.copy(.1f), RoundedCornerShape(26.dp)).padding(20.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Acid.copy(.14f)),
                contentAlignment = Alignment.Center
            ) { GlyphIcon(Glyph.ArrowDown, Acid, 22.dp) }
            Spacer(Modifier.height(16.dp))
            Label(tr("この動画をダウンロードしますか？", "Download this video?"), 18.sp, Paper, FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            val singleCandidate = target.mediaCandidates.singleOrNull()
            if (singleCandidate != null) {
                Row(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(17.dp)).background(Elevated).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteMediaThumbnail(singleCandidate.copy(refererUrl = singleCandidate.refererUrl ?: target.pageUrl), Modifier.width(108.dp).fillMaxHeight())
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Label(singleCandidate.title ?: target.title, 12.sp, Paper, FontWeight.Bold, maxLines = 2)
                        Spacer(Modifier.height(5.dp)); Label(host, 9.sp, Acid, FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(formatCandidateDuration(singleCandidate.durationSeconds), 8.sp, Muted, FontWeight.Bold)
                            Spacer(Modifier.width(7.dp)); Box(Modifier.size(3.dp).background(Line, CircleShape)); Spacer(Modifier.width(7.dp))
                            Label(formatCandidateSize(singleCandidate.sizeBytes), 8.sp, Muted, FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Elevated).padding(13.dp)) {
                    Label(target.title, 13.sp, Paper, FontWeight.Bold, maxLines = 2)
                    Spacer(Modifier.height(5.dp)); Label(host, 10.sp, Acid, FontWeight.Bold)
                    Spacer(Modifier.height(3.dp)); Label(target.pageUrl, 10.sp, Muted, maxLines = 1)
                }
            }
            if (target.directMediaUrls.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Label(
                if (target.mediaCandidates.size == 1 && target.mediaCandidates.first().url == target.pageUrl) tr("動画情報を取得しました", "Video information retrieved")
                else tr("直接動画を${target.directMediaUrls.size}件検出 · yt-dlp失敗時に自動で試します", "${target.directMediaUrls.size} direct videos detected · tried automatically if yt-dlp fails"),
                    10.sp, Muted
                )
            }
            if (target.directMediaUrls.size > 1) {
                Spacer(Modifier.height(13.dp))
                Label(tr("ダウンロードする動画を選択", "SELECT VIDEO TO DOWNLOAD"), 10.sp, Acid, FontWeight.Bold, letterSpacing = .8f)
                Spacer(Modifier.height(7.dp))
                LazyColumn(Modifier.heightIn(max = 248.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(target.mediaCandidates, key = { it.url }) { candidate ->
                        val mediaUrl = candidate.url
                        val selected = selectedUrl == mediaUrl
                        Row(
                            Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Acid.copy(.14f) else Elevated)
                                .border(1.dp, if (selected) Acid.copy(.38f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .noirClick { selectedUrl = mediaUrl }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RemoteMediaThumbnail(candidate.copy(refererUrl = candidate.refererUrl ?: target.pageUrl), Modifier.width(86.dp).fillMaxHeight())
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Label(candidate.title ?: mediaCandidateLabel(mediaUrl, target.directMediaUrls.indexOf(mediaUrl)), 11.sp, if (selected) Paper else Muted, FontWeight.Bold, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlyphIcon(Glyph.Play, Muted, 10.dp); Spacer(Modifier.width(4.dp))
                                    Label(formatCandidateDuration(candidate.durationSeconds), 9.sp, Muted, FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp)); Box(Modifier.size(3.dp).background(Line, CircleShape)); Spacer(Modifier.width(8.dp))
                                    Label(formatCandidateSize(candidate.sizeBytes), 9.sp, Muted, FontWeight.Bold)
                                }
                                Spacer(Modifier.height(3.dp)); Label(Uri.parse(mediaUrl).host.orEmpty().removePrefix("www."), 8.sp, Muted, maxLines = 1)
                            }
                            Box(Modifier.size(23.dp).clip(CircleShape).background(if (selected) Acid else Color.White.copy(.05f)), contentAlignment = Alignment.Center) {
                                if (selected) GlyphIcon(Glyph.Check, Ink, 12.dp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Elevated)
                        .noirClick(onClick = onDismiss), contentAlignment = Alignment.Center
            ) { Label(tr("キャンセル", "Cancel"), 12.sp, Paper, FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Acid)
                        .noirClick { onConfirm(selectedUrl) }, contentAlignment = Alignment.Center
            ) { Label(if (target.directMediaUrls.size > 1) tr("選択して開始", "Select and start") else tr("開始する", "Start"), 12.sp, Ink, FontWeight.Black) }
            }
        }
    }
}

private object RemoteThumbnailCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val attempted = ConcurrentHashMap.newKeySet<String>()
    private val semaphore = java.util.concurrent.Semaphore(3)
    private val _bitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val bitmaps = _bitmaps.asStateFlow()

    fun key(candidate: DetectedMedia) = "${candidate.thumbnailUrl.orEmpty()}|${candidate.url}|${candidate.refererUrl.orEmpty()}"

    fun request(context: Context, candidate: DetectedMedia) {
        val key = key(candidate)
        if (_bitmaps.value.containsKey(key) || !attempted.add(key)) return
        val appContext = context.applicationContext
        scope.launch {
            semaphore.acquire()
            try {
                load(appContext, candidate, key)?.let { bitmap ->
                    synchronized(this@RemoteThumbnailCache) {
                        _bitmaps.value = _bitmaps.value + (key to bitmap)
                    }
                }
            } finally {
                semaphore.release()
            }
        }
    }

    private fun load(context: Context, candidate: DetectedMedia, key: String): Bitmap? {
        val referer = candidate.refererUrl?.takeIf { it.startsWith("http") } ?: candidate.url
        val cacheFile = File(context.cacheDir, "remote-video-thumbnails/${key.hashCode().toUInt()}.jpg")
        cacheFile.parentFile?.mkdirs()
        if (cacheFile.length() > 0L) BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return it }

        val thumbnailSources = buildList {
            candidate.thumbnailUrl?.let(::add)
            youtubeVideoId(candidate.url)?.let { id ->
                add("https://i.ytimg.com/vi/$id/maxresdefault.jpg")
                add("https://i.ytimg.com/vi/$id/hqdefault.jpg")
                add("https://i.ytimg.com/vi/$id/mqdefault.jpg")
            }
        }.distinct()
        val supplied = thumbnailSources.firstNotNullOfOrNull { source ->
            runCatching {
                when {
                    source.startsWith("data:image/") -> {
                        val bytes = Base64.decode(source.substringAfter(','), Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    source.startsWith("https://") || source.startsWith("http://") -> {
                        val connection = java.net.URL(source).openConnection().apply {
                            connectTimeout = 8_000
                            readTimeout = 10_000
                            setRequestProperty("User-Agent", BrowserIdentity.USER_AGENT)
                            setRequestProperty("Referer", referer)
                            CookieManager.getInstance().getCookie(referer)?.takeIf { it.isNotBlank() }
                                ?.let { setRequestProperty("Cookie", it) }
                        }
                        connection.getInputStream().use(BitmapFactory::decodeStream)
                    }
                    else -> null
                }
            }.getOrNull()
        }
        val retrieved = supplied ?: runCatching {
            MediaMetadataRetriever().let { retriever ->
                try {
                    val headers = mutableMapOf("User-Agent" to BrowserIdentity.USER_AGENT, "Referer" to referer)
                    CookieManager.getInstance().getCookie(referer)?.let { headers["Cookie"] = it }
                    retriever.setDataSource(candidate.url, headers)
                    val middleUs = if (candidate.durationSeconds > 0) (candidate.durationSeconds * 500_000L).toLong() else 1_000_000L
                    retriever.getFrameAtTime(middleUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(middleUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } finally { retriever.release() }
            }
        }.getOrNull()
        if (retrieved != null) {
            runCatching { cacheFile.outputStream().use { retrieved.compress(Bitmap.CompressFormat.JPEG, 82, it) } }
            return retrieved
        }

        val ffmpeg = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (!ffmpeg.exists()) return null
        val middleSeconds = if (candidate.durationSeconds > 0) (candidate.durationSeconds / 2.0).coerceAtLeast(0.5) else 1.0
        val cookie = CookieManager.getInstance().getCookie(referer).orEmpty()
        fun captureFrame(second: Double, seekBeforeInput: Boolean): Bitmap? = runCatching {
            cacheFile.delete()
            val command = mutableListOf(ffmpeg.absolutePath, "-y", "-user_agent", BrowserIdentity.USER_AGENT)
            command += listOf("-headers", "Referer: $referer\r\n${if (cookie.isNotBlank()) "Cookie: $cookie\r\n" else ""}")
            if (seekBeforeInput) command += listOf("-ss", "%.2f".format(java.util.Locale.US, second))
            command += listOf("-i", candidate.url)
            if (!seekBeforeInput) command += listOf("-ss", "%.2f".format(java.util.Locale.US, second))
            command += listOf("-frames:v", "1", "-vf", "scale=480:-2", "-q:v", "4", cacheFile.absolutePath)
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { reader -> while (reader.readLine() != null) Unit }
            if (process.waitFor() == 0 && cacheFile.length() > 0L) BitmapFactory.decodeFile(cacheFile.absolutePath) else null
        }.getOrNull()
        return captureFrame(middleSeconds, true)
            ?: captureFrame(middleSeconds, false)
            ?: captureFrame(0.1, false)
    }
}

@Composable
private fun RemoteMediaThumbnail(candidate: DetectedMedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cached by RemoteThumbnailCache.bitmaps.collectAsState()
    val cacheKey = remember(candidate.thumbnailUrl, candidate.url, candidate.refererUrl) { RemoteThumbnailCache.key(candidate) }
    LaunchedEffect(cacheKey) { RemoteThumbnailCache.request(context, candidate) }
    val bitmap = cached[cacheKey]
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(Color(0xFF23232A)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize()) { drawRect(Brush.linearGradient(listOf(Color(0xFF263A5E), Color(0xFF171820)))) }
            GlyphIcon(Glyph.Play, Muted, 18.dp)
        }
        if (candidate.durationSeconds > 0) {
            Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(.76f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Label(formatCandidateDuration(candidate.durationSeconds), 7.sp, Paper, FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BrowserTabRail(session: BrowserSession, searchEngine: SearchEngine) {
    val scrollState = rememberScrollState()
    val tabWidthPx = with(LocalDensity.current) { 142.dp.roundToPx() }
    val restoredSelectedId = remember { session.selectedId }
    var previousTabCount by remember { mutableIntStateOf(session.tabs.size) }
    LaunchedEffect(restoredSelectedId) {
        delay(100)
        val selectedIndex = session.tabs.indexOfFirst { it.id == restoredSelectedId }
        if (selectedIndex > 0) {
            scrollState.animateScrollTo((selectedIndex * tabWidthPx).coerceAtMost(scrollState.maxValue))
        }
    }
    LaunchedEffect(session.tabs.size) {
        if (session.tabs.size > previousTabCount) {
            delay(80)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        previousTabCount = session.tabs.size
    }
    Row(Modifier.fillMaxWidth().height(40.dp).horizontalScroll(scrollState).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        session.tabs.forEach { tab ->
            val selected = tab.id == session.selectedId
            Row(Modifier.width(142.dp).fillMaxHeight().padding(end = 5.dp).clip(RoundedCornerShape(11.dp))
                .background(if (selected) Color.White.copy(.105f) else Color.White.copy(.035f))
                .border(1.dp, if (selected) Acid.copy(.24f) else Color.Transparent, RoundedCornerShape(11.dp))
                .noirClick { session.select(tab.id) }.padding(start = 10.dp, end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tab.favicon != null) {
                    Image(
                        tab.favicon!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.size(8.dp).background(if (tab.progress in 1..99 || selected) Acid else Muted.copy(.6f), CircleShape))
                }
                Spacer(Modifier.width(7.dp))
                Label(tab.title, 10.sp, if (selected) Paper else Muted, if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, modifier = Modifier.weight(1f))
                Box(Modifier.size(27.dp).noirClick { session.close(tab.id, searchEngine.homeUrl) }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 11.dp) }
            }
        }
    }
}

@Composable
private fun DownloadButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    analyzing: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
        .background(if (enabled) Brush.linearGradient(listOf(Acid, Acid.copy(alpha = .74f))) else Brush.linearGradient(listOf(Color(0xFF29292F), Color(0xFF202025))))
        .noirClick(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        if (analyzing) AnalysisSpinner() else GlyphIcon(Glyph.ArrowDown, if (enabled) Ink else Color(0xFF55555D), 17.dp, 2.5f)
    }
}

@Composable
private fun AnalysisSpinner() {
    val transition = rememberInfiniteTransition(label = "mediaAnalysis")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(780)), label = "mediaAnalysisRotation")
    Canvas(Modifier.size(16.dp).rotate(rotation)) {
        drawArc(Muted, -80f, 255f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun LibraryScreen(onPlay: (MediaFile) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val files by DownloadsStore.files.collectAsState()
    val activeDownloads by DownloadsStore.active.collectAsState()
    val libraryListState = rememberLazyListState()
    var observedFilePaths by remember { mutableStateOf<Set<String>?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedSite by remember { mutableStateOf(ALL_SITES) }
    var durationFilter by remember { mutableStateOf(DurationFilter.All) }
    var filterOpen by remember { mutableStateOf(false) }
    val sites = remember(files) { listOf(ALL_SITES) + files.map { it.site }.filter { it != tr("不明", "Unknown") }.distinct().sorted() }
    val filtered = remember(files, query, selectedSite, durationFilter) {
        files.filter { media ->
            media.title.contains(query.trim(), ignoreCase = true) &&
                (selectedSite == ALL_SITES || media.site == selectedSite) &&
                when (durationFilter) {
                    DurationFilter.All -> true
                    DurationFilter.Short -> media.durationMs in 1 until 5 * 60_000L
                    DurationFilter.Medium -> media.durationMs in 5 * 60_000L until 20 * 60_000L
                    DurationFilter.Long -> media.durationMs >= 20 * 60_000L
                }
        }
    }

    LaunchedEffect(files) {
        val currentPaths = files.mapTo(HashSet()) { it.file.absolutePath }
        val previousPaths = observedFilePaths
        observedFilePaths = currentPaths
        if (previousPaths != null && currentPaths.any { it !in previousPaths }) {
            query = ""
            selectedSite = ALL_SITES
            durationFilter = DurationFilter.All
            delay(120)
            libraryListState.animateScrollToItem(0)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) DownloadsStore.scan(context) }
        lifecycle.lifecycle.addObserver(observer)
        DownloadsStore.scan(context)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Label(tr("ライブラリ", "Library"), 27.sp, Acid, FontWeight.Black, letterSpacing = -.5f)
            }
            Label(tr("${files.size}本", "${files.size} videos"), 12.sp, Muted, FontWeight.Bold)
        }

        SearchBox(query, { query = it }, Modifier.padding(horizontal = 18.dp))
        val activeFilterCount = (if (selectedSite != ALL_SITES) 1 else 0) + (if (durationFilter != DurationFilter.All) 1 else 0)
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.height(40.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (activeFilterCount > 0) Acid.copy(.14f) else Elevated)
                    .border(1.dp, if (activeFilterCount > 0) Acid.copy(.4f) else Color.White.copy(.07f), RoundedCornerShape(14.dp))
                    .noirClick { filterOpen = true }.padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphIcon(Glyph.Filter, if (activeFilterCount > 0) Acid else Muted, 17.dp)
                Spacer(Modifier.width(8.dp)); Label(tr("絞り込み", "Filter"), 11.sp, if (activeFilterCount > 0) Paper else Muted, FontWeight.Bold)
                if (activeFilterCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(20.dp).background(Acid, CircleShape), contentAlignment = Alignment.Center) {
                        Label(activeFilterCount.toString(), 9.sp, Ink, FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.width(9.dp))
            Label(tr("${filtered.size}件", "${filtered.size} items"), 11.sp, Muted, FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (activeFilterCount > 0) {
                Box(Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).noirClick {
                    selectedSite = ALL_SITES; durationFilter = DurationFilter.All
                }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Label(tr("リセット", "Reset"), 10.sp, Acid, FontWeight.Bold) }
            }
        }
        AnimatedVisibility(activeDownloads.isNotEmpty(), enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Spacer(Modifier.height(3.dp))
                activeDownloads.forEach { state ->
                    DownloadCapsule(
                        state,
                        openLibrary = {},
                        cancel = { DownloadsStore.cancel(context, state.id) },
                        dismiss = { DownloadsStore.dismiss(state) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) EmptyLibrary(query.isNotBlank() || selectedSite != ALL_SITES || durationFilter != DurationFilter.All)
        else LazyColumn(Modifier.fillMaxSize(), state = libraryListState, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { it.file.absolutePath }) { media -> MediaCard(media, onPlay) }
        }
    }
    if (filterOpen) {
        LibraryFilterDialog(
            sites = sites,
            selectedSite = selectedSite,
            durationFilter = durationFilter,
            onDismiss = { filterOpen = false },
            onApply = { site, duration ->
                selectedSite = site
                durationFilter = duration
                filterOpen = false
            }
        )
    }
}

@Composable
private fun LibraryFilterDialog(
    sites: List<String>,
    selectedSite: String,
    durationFilter: DurationFilter,
    onDismiss: () -> Unit,
    onApply: (String, DurationFilter) -> Unit
) {
    var draftSite by remember(selectedSite) { mutableStateOf(selectedSite) }
    var draftDuration by remember(durationFilter) { mutableStateOf(durationFilter) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF17171B))
                .border(1.dp, Color.White.copy(.1f), RoundedCornerShape(28.dp)).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Acid.copy(.14f)), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.Filter, Acid, 21.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column { Label(tr("ライブラリを絞り込む", "Filter library"), 17.sp, Paper, FontWeight.Black); Label(tr("サイトと動画の長さ", "Site and video length"), 10.sp, Muted) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(36.dp).noirClick(onClick = onDismiss), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 16.dp) }
            }
            Spacer(Modifier.height(18.dp))
            Label(tr("サイト", "SITE"), 10.sp, Muted, FontWeight.Bold, letterSpacing = 1.2f)
            Spacer(Modifier.height(7.dp))
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 250.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(sites, key = { it }) { site ->
                    FilterOptionRow(if (site == ALL_SITES) tr("すべて", "All") else site, draftSite == site) { draftSite = site }
                }
            }
            Spacer(Modifier.height(15.dp))
            Label(tr("動画の長さ", "VIDEO LENGTH"), 10.sp, Muted, FontWeight.Bold, letterSpacing = 1.2f)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    DurationFilter.All to tr("すべて", "All"),
                    DurationFilter.Short to tr("〜5分", "≤5 min"),
                    DurationFilter.Medium to tr("5〜20分", "5–20 min"),
                    DurationFilter.Long to tr("20分〜", "20+ min")
                ).forEach { (filter, label) ->
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (draftDuration == filter) Acid else Elevated)
                            .noirClick { draftDuration = filter }, contentAlignment = Alignment.Center
                    ) { Label(label, 9.sp, if (draftDuration == filter) Ink else Muted, FontWeight.Bold, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Elevated).noirClick {
                    draftSite = ALL_SITES; draftDuration = DurationFilter.All
                }, contentAlignment = Alignment.Center) { Label(tr("すべて解除", "Clear all"), 11.sp, Paper, FontWeight.Bold) }
                Box(Modifier.weight(1.25f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Acid).noirClick {
                    onApply(draftSite, draftDuration)
                }, contentAlignment = Alignment.Center) { Label(tr("適用する", "Apply"), 11.sp, Ink, FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun FilterOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(43.dp).clip(RoundedCornerShape(13.dp))
            .background(if (selected) Acid.copy(.12f) else Elevated.copy(.55f)).noirClick(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(label, 12.sp, if (selected) Paper else Muted, if (selected) FontWeight.Bold else FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (selected) Box(Modifier.size(23.dp).background(Acid, CircleShape), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Check, Ink, 13.dp) }
    }
}

private enum class UpdateStage { Confirm, Downloading, Permission, Error }

@Composable
private fun UpdateInstallDialog(update: UpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(UpdateStage.Confirm) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(update.assetSize) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    fun launchInstaller(file: File) {
        runCatching { context.startActivity(UpdateInstaller.installIntent(context, file)) }
            .onSuccess { onDismiss() }
            .onFailure {
                errorMessage = tr("インストーラーを開けませんでした。", "Could not open the installer.")
                stage = UpdateStage.Error
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val file = downloadedFile
        if (file != null && UpdateInstaller.canInstall(context)) launchInstaller(file)
        else {
            errorMessage = tr("このアプリからのインストールを許可してください。", "Allow installs from this app.")
            stage = UpdateStage.Permission
        }
    }

    fun beginDownload() {
        if (update.downloadUrl == null) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releasePageUrl))) }
            onDismiss()
            return
        }
        stage = UpdateStage.Downloading
        scope.launch {
            runCatching {
                UpdateInstaller.download(context, update) { value ->
                    downloadedBytes = value.downloadedBytes
                    totalBytes = value.totalBytes
                    progress = value.fraction ?: 0f
                }
            }.onSuccess { file ->
                downloadedFile = file
                if (UpdateInstaller.canInstall(context)) launchInstaller(file)
                else {
                    stage = UpdateStage.Permission
                    permissionLauncher.launch(UpdateInstaller.permissionIntent(context))
                }
            }.onFailure {
                errorMessage = it.message ?: tr("APKを取得できませんでした。", "Could not download the APK.")
                stage = UpdateStage.Error
            }
        }
    }

    Dialog(onDismissRequest = { if (stage != UpdateStage.Downloading) onDismiss() }) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF17171B))
                .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(28.dp)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Acid.copy(.14f)), contentAlignment = Alignment.Center) {
                    GlyphIcon(if (stage == UpdateStage.Error) Glyph.Info else Glyph.ArrowDown, if (stage == UpdateStage.Error) Red else Acid, 22.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Label(
                        when (stage) {
                            UpdateStage.Confirm -> tr("アップデートがあります", "Update available")
                            UpdateStage.Downloading -> tr("アップデートを取得中", "Downloading update")
                            UpdateStage.Permission -> tr("インストールの許可が必要です", "Install permission required")
                            UpdateStage.Error -> tr("アップデートできませんでした", "Update failed")
                        }, 17.sp, Paper, FontWeight.Black
                    )
                    Label("Veiload ${update.latestVersion}", 10.sp, Acid, FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            when (stage) {
                UpdateStage.Confirm -> {
                    Label(tr("新しいバージョンをGitHub Releasesからダウンロードしてインストールできます。", "A new version can be downloaded and installed from GitHub Releases."), 12.sp, Muted, maxLines = 3)
                    update.assetName?.let { Spacer(Modifier.height(8.dp)); Label(it, 10.sp, Paper, FontWeight.Medium) }
                    update.assetSize?.let { Label(formatUpdateBytes(it), 9.sp, Muted) }
                }
                UpdateStage.Downloading -> {
                    BoxWithConstraints(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Elevated)) {
                        Box(Modifier.fillMaxHeight().width(maxWidth * progress.coerceIn(0f, 1f)).background(Acid, CircleShape))
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Label(formatUpdateBytes(downloadedBytes), 9.sp, Muted)
                        Spacer(Modifier.weight(1f))
                        totalBytes?.let { Label("${(progress * 100).roundToInt()}%", 9.sp, Paper, FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(7.dp)); Label(tr("取得後にAPKの整合性を確認します。", "The APK integrity is checked after download."), 10.sp, Muted)
                }
                UpdateStage.Permission -> Label(tr("設定画面で「この提供元のアプリを許可」を有効にして戻ってください。", "Enable ‘Allow from this source’ in Settings, then return."), 12.sp, Muted, maxLines = 3)
                UpdateStage.Error -> Label(errorMessage, 12.sp, Red, maxLines = 4)
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stage != UpdateStage.Downloading) {
                    Box(Modifier.weight(1f).height(45.dp).clip(RoundedCornerShape(14.dp)).background(Elevated).noirClick(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Label(tr("後で", "Later"), 11.sp, Paper, FontWeight.Bold)
                    }
                }
                when (stage) {
                    UpdateStage.Confirm -> Box(Modifier.weight(1.25f).height(45.dp).clip(RoundedCornerShape(14.dp)).background(Acid).noirClick { beginDownload() }, contentAlignment = Alignment.Center) {
                        Label(if (update.downloadUrl == null) tr("Releaseを開く", "Open release") else tr("アップデート", "Update"), 11.sp, Ink, FontWeight.Black)
                    }
                    UpdateStage.Downloading -> Unit
                    UpdateStage.Permission -> Box(Modifier.weight(1.25f).height(45.dp).clip(RoundedCornerShape(14.dp)).background(Acid).noirClick { permissionLauncher.launch(UpdateInstaller.permissionIntent(context)) }, contentAlignment = Alignment.Center) {
                        Label(tr("設定を開く", "Open settings"), 11.sp, Ink, FontWeight.Black)
                    }
                    UpdateStage.Error -> Box(Modifier.weight(1.25f).height(45.dp).clip(RoundedCornerShape(14.dp)).background(Acid).noirClick { beginDownload() }, contentAlignment = Alignment.Center) {
                        Label(tr("再試行", "Retry"), 11.sp, Ink, FontWeight.Black)
                    }
                }
            }
        }
    }
}

private fun formatUpdateBytes(value: Long): String = when {
    value >= 1024L * 1024L -> "%.1f MB".format(value / (1024.0 * 1024.0))
    value >= 1024L -> "%.1f KB".format(value / 1024.0)
    else -> "$value B"
}

private data class ThirdPartyLicense(
    val name: String,
    val license: String,
    val noteJa: String,
    val noteEn: String,
    val url: String
) {
    val note get() = tr(noteJa, noteEn)
}

private val THIRD_PARTY_LICENSES = listOf(
    ThirdPartyLicense(
        "Phosphor Icon for Compose",
        "MIT",
        "アプリ内アイコン",
        "In-app icons",
        "https://github.com/adamglin0/compose-phosphor-icon/blob/master/LICENSE"
    ),
    ThirdPartyLicense(
        "youtubedl-android / FFmpeg",
        "GPL-3.0",
        "Android向け実行環境",
        "Android runtime and media processing",
        "https://github.com/yausername/youtubedl-android/blob/master/LICENSE"
    ),
    ThirdPartyLicense(
        "AndroidX Media3",
        "Apache-2.0",
        "動画プレイヤー",
        "Video player",
        "https://github.com/androidx/media/blob/release/LICENSE"
    ),
    ThirdPartyLicense(
        "AndroidX / Jetpack Compose",
        "Apache-2.0",
        "画面表示とアプリ基盤",
        "UI and application framework",
        "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt"
    ),
    ThirdPartyLicense(
        "Kotlin",
        "Apache-2.0",
        "プログラミング言語ランタイム",
        "Programming language runtime",
        "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt"
    ),
    ThirdPartyLicense(
        "Python",
        "PSF-2.0",
        "動画解析エンジンの実行環境",
        "Runtime used by the video extraction engine",
        "https://docs.python.org/3/license.html"
    ),
    ThirdPartyLicense(
        "QuickJS",
        "MIT",
        "JavaScript実行環境",
        "JavaScript runtime",
        "https://github.com/bellard/quickjs/blob/master/LICENSE"
    ),
    ThirdPartyLicense(
        "EasyList / EasyPrivacy",
        "GPL-3.0+ / CC BY-SA 3.0+",
        "広告・トラッキングフィルター",
        "Ad and tracking filter data",
        "https://easylist.to/pages/licence.html"
    )
)

@Composable
private fun SettingsScreen(
    searchEngine: SearchEngine,
    accentChoice: AccentChoice,
    appLanguage: AppLanguage,
    adBlockEnabled: Boolean,
    videoAdBlockEnabled: Boolean,
    cosmeticFilteringEnabled: Boolean,
    focusModeEnabled: Boolean,
    onSearchEngineChange: (SearchEngine) -> Unit,
    onAccentChoiceChange: (AccentChoice) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onFocusModeChange: (Boolean) -> Unit,
    onAdBlockEnabledChange: (Boolean) -> Unit,
    onVideoAdBlockEnabledChange: (Boolean) -> Unit,
    onCosmeticFilteringEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var licensesExpanded by remember { mutableStateOf(false) }
    var expandedSection by rememberSaveable { mutableStateOf<String?>("personalize") }
    var searchEnginesExpanded by remember { mutableStateOf(false) }
    var backgroundUnrestricted by remember { mutableStateOf(isBackgroundUnrestricted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) backgroundUnrestricted = isBackgroundUnrestricted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val licenses = THIRD_PARTY_LICENSES
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 10.dp)) {
                Label(tr("マイページ", "Settings"), 27.sp, Acid, FontWeight.Black, letterSpacing = -.5f)
            }
        }
        item {
            AuthorCard { openExternal(context, "https://github.com/inaho-tech") }
        }
        item {
            SettingsCategoryHeader(
                Glyph.Gear,
                tr("言語と外観", "Language & appearance"),
                "${languageLabel(appLanguage)} · ${accentChoice.label}",
                expandedSection == "personalize"
            ) { expandedSection = if (expandedSection == "personalize") null else "personalize" }
        }
        item {
            AnimatedVisibility(
                visible = expandedSection == "personalize",
                enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
            ) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(17.dp))) {
                    Label(tr("言語", "LANGUAGE"), 9.sp, Muted, FontWeight.Bold, letterSpacing = 1.1f, modifier = Modifier.padding(start = 14.dp, top = 13.dp, bottom = 4.dp))
                    AppLanguage.entries.forEachIndexed { index, language ->
                        Row(
                            Modifier.fillMaxWidth().height(46.dp).noirClick { onLanguageChange(language) }.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Label(languageLabel(language), 12.sp, Paper, if (language == appLanguage) FontWeight.Bold else FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            if (language == appLanguage) GlyphIcon(Glyph.Check, Acid, 16.dp)
                        }
                        if (index < AppLanguage.entries.lastIndex) PlainDivider()
                    }
                    PlainDivider()
                    Label(tr("アクセントカラー", "ACCENT COLOR"), 9.sp, Muted, FontWeight.Bold, letterSpacing = 1.1f, modifier = Modifier.padding(start = 14.dp, top = 13.dp, bottom = 8.dp))
                    Box(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) { AccentPicker(accentChoice, onAccentChoiceChange) }
                    PlainDivider()
                    SettingToggleRow(
                        tr("フォーカスモード", "Focus mode"),
                        tr("ステータスバーとナビゲーションバーを隠します", "Hides the status and navigation bars"),
                        focusModeEnabled,
                        onFocusModeChange
                    )
                }
            }
        }
        item {
            SettingsCategoryHeader(
                Glyph.Compass,
                tr("ブラウザとプライバシー", "Browser & privacy"),
                tr("広告対策・検索エンジン", "Blocking and search engine"),
                expandedSection == "browser"
            ) { expandedSection = if (expandedSection == "browser") null else "browser" }
        }
        item {
            AnimatedVisibility(
                visible = expandedSection == "browser",
                enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
            ) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(17.dp))) {
                    SettingToggleRow(tr("広告、トラッキングブロッカー", "Ad and tracker blocker"), tr("初期設定ではオン・不具合があるサイトではオフにできます", "On by default; disable it on sites with issues"), adBlockEnabled, onAdBlockEnabledChange)
                    PlainDivider()
                    SettingToggleRow(tr("広告要素を非表示", "Hide ad elements"), tr("EasyListの要素非表示ルール・初期設定ではオン", "EasyList cosmetic rules; on by default"), cosmeticFilteringEnabled, onCosmeticFilteringEnabledChange)
                    PlainDivider()
                    SettingToggleRow(tr("動画広告をブロック", "Block video ads"), tr("再生に不具合が発生することがあるため初期設定ではオフ", "Off by default because it may affect playback"), videoAdBlockEnabled, onVideoAdBlockEnabledChange)
                    PlainDivider()
                    PlainSettingRow(tr("検索エンジン", "Search engine"), searchEngine.label, clickable = true) { searchEnginesExpanded = !searchEnginesExpanded }
                    if (searchEnginesExpanded) SearchEngine.entries.forEach { engine ->
                        Row(Modifier.fillMaxWidth().height(44.dp).noirClick { onSearchEngineChange(engine); searchEnginesExpanded = false }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(8.dp)); Label(engine.label, 12.sp, if (engine == searchEngine) Paper else Muted, if (engine == searchEngine) FontWeight.Bold else FontWeight.Medium)
                            Spacer(Modifier.weight(1f)); if (engine == searchEngine) GlyphIcon(Glyph.Check, Acid, 15.dp)
                        }
                    }
                }
            }
        }
        item {
            SettingsCategoryHeader(Glyph.ArrowDown, tr("ダウンロード", "Downloads"), tr("保存先・形式・バックグラウンド", "Location, format and background"), expandedSection == "downloads") {
                expandedSection = if (expandedSection == "downloads") null else "downloads"
            }
        }
        item {
            AnimatedVisibility(
                visible = expandedSection == "downloads",
                enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
            ) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(17.dp))) {
                    PlainSettingRow(tr("保存先", "Save location"), "Downloads / Veiload")
                    PlainDivider(); PlainSettingRow(tr("動画形式", "Video format"), tr("H.264を優先", "Prefer H.264"))
                    PlainDivider(); PlainSettingRow(tr("解析エンジン", "Extraction engine"), "yt-dlp Nightly")
                    PlainDivider(); PlainSettingRow(tr("バックグラウンド継続", "Continue in background"), if (backgroundUnrestricted) tr("制限なし", "Unrestricted") else tr("バッテリー制限あり", "Battery restricted"), clickable = true) { requestBackgroundUnrestricted(context) }
                }
            }
        }
        item {
            SettingsCategoryHeader(Glyph.Info, tr("アプリ情報", "App information"), tr("設定・ライセンス", "System settings and licenses"), expandedSection == "about") {
                expandedSection = if (expandedSection == "about") null else "about"
            }
        }
        item {
            AnimatedVisibility(
                visible = expandedSection == "about",
                enter = expandVertically(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(17.dp))) {
                        PlainSettingRow("Veiload", tr("バージョン ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"))
                        PlainDivider(); PlainSettingRow(tr("Androidの設定", "Android settings"), tr("権限・通知・ストレージ", "Permissions, notifications, storage"), clickable = true) {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }
                        PlainDivider(); PlainSettingRow(tr("ライセンス", "Licenses"), tr("${licenses.size}件", "${licenses.size} items"), clickable = true) { licensesExpanded = !licensesExpanded }
                    }
                    AnimatedVisibility(
                        visible = licensesExpanded,
                        enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(230, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
                    ) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Elevated.copy(.7f)).padding(horizontal = 10.dp)) {
                            licenses.forEachIndexed { index, license ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).noirClick { openExternal(context, license.url) }.padding(horizontal = 5.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Label(license.name, 12.sp, Paper, FontWeight.Bold)
                                        Label("${license.license} · ${license.note}", 9.sp, Muted)
                                        Label(tr("ライセンス本文を開く", "Open license text"), 8.sp, Acid.copy(.82f), FontWeight.Medium)
                                    }
                                    GlyphIcon(Glyph.External, Muted, 14.dp)
                                }
                                if (index < licenses.lastIndex) PlainDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorCard(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Acid.copy(.20f), Panel, Color(0xFF211B14))))
            .border(1.dp, Acid.copy(.30f), RoundedCornerShape(22.dp))
            .noirClick(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_inaho_tech),
            contentDescription = tr("Inaho Techのロゴ", "Inaho Tech logo"),
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Label(tr("作者", "CREATOR"), 9.sp, Acid, FontWeight.Bold, letterSpacing = 1.3f)
            Spacer(Modifier.height(3.dp)); Label("Inaho Tech", 17.sp, Paper, FontWeight.Black)
            Label("github.com/inaho-tech", 9.sp, Muted, FontWeight.Medium)
        }
        GlyphIcon(Glyph.External, Acid, 16.dp)
    }
}

@Composable
private fun SettingsCategoryHeader(glyph: Glyph, title: String, summary: String, expanded: Boolean, onClick: () -> Unit) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "settings-arrow"
    )
    Row(
        Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(18.dp)).background(Elevated)
            .border(1.dp, if (expanded) Acid.copy(.28f) else Color.White.copy(.07f), RoundedCornerShape(18.dp))
            .noirClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Acid.copy(.11f)), contentAlignment = Alignment.Center) { GlyphIcon(glyph, Acid, 17.dp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) { Label(title, 13.sp, Paper, FontWeight.Bold); Spacer(Modifier.height(2.dp)); Label(summary, 9.sp, Muted, maxLines = 1) }
        Box(Modifier.graphicsLayer { rotationZ = arrowRotation }) { GlyphIcon(Glyph.Forward, Muted, 14.dp) }
    }
}

@Composable
private fun AccentPicker(selected: AccentChoice, onSelect: (AccentChoice) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel)
            .border(1.dp, Color.White.copy(.07f), RoundedCornerShape(17.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AccentChoice.entries.forEach { choice ->
            Column(
                Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (choice == selected) choice.color.copy(alpha = .14f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (choice == selected) choice.color.copy(alpha = .5f) else Color.Transparent,
                        RoundedCornerShape(13.dp)
                    )
                    .noirClick { onSelect(choice) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(22.dp).background(choice.color, CircleShape))
                Spacer(Modifier.height(6.dp))
                Label(choice.label, 9.sp, if (choice == selected) Paper else Muted, FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlainSectionTitle(text: String) {
    Label(text, 11.sp, Muted, FontWeight.Bold, modifier = Modifier.padding(start = 3.dp, top = 9.dp, bottom = 2.dp))
}

@Composable
private fun PlainDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(Line))
}

@Composable
private fun SettingToggleRow(title: String, detail: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val thumbOffset by animateFloatAsState(if (enabled) 20f else 2f, tween(180), label = "setting-toggle")
    Row(
        Modifier.fillMaxWidth().height(58.dp).noirClick { onChange(!enabled) }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Label(title, 13.sp, Paper, FontWeight.SemiBold)
            Label(detail, 9.sp, Muted)
        }
        Box(
            Modifier.width(48.dp).height(28.dp).clip(CircleShape)
                .background(if (enabled) Acid else Color(0xFF3A3A3C)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.offset(x = thumbOffset.dp).size(24.dp)
                    .shadow(3.dp, CircleShape, clip = false)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun PlainSettingRow(title: String, value: String, clickable: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).then(if (clickable) Modifier.noirClick(onClick = onClick) else Modifier).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(title, 12.sp, Paper, FontWeight.Medium)
        Spacer(Modifier.weight(1f)); Label(value, 10.sp, Muted, FontWeight.Medium, maxLines = 1)
        if (clickable) { Spacer(Modifier.width(7.dp)); GlyphIcon(Glyph.Forward, Muted, 13.dp) }
    }
}

@Composable
private fun MyPageScreen(searchEngine: SearchEngine, onSearchEngineChange: (SearchEngine) -> Unit) {
    val context = LocalContext.current
    var licensesExpanded by remember { mutableStateOf(false) }
    val licenses = THIRD_PARTY_LICENSES

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 96.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label("SETTINGS", 10.sp, Acid, FontWeight.Bold, letterSpacing = 2.3f)
                    Spacer(Modifier.height(2.dp)); Label("マイページ", 31.sp, Paper, FontWeight.Black, letterSpacing = -.7f)
                }
                Box(Modifier.size(45.dp).clip(RoundedCornerShape(16.dp)).background(Elevated).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.User, Acid, 24.dp)
                }
            }
        }

        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF18305E), Color(0xFF101B31), Panel)))
                    .border(1.dp, Acid.copy(.25f), RoundedCornerShape(27.dp)).padding(19.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Acid), contentAlignment = Alignment.Center) {
                            GlyphIcon(Glyph.ArrowDown, Ink, 25.dp)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column { Label("Veiload", 21.sp, Paper, FontWeight.Black); Label("Version ${BuildConfig.VERSION_NAME} · Android Native", 10.sp, Blue, FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileMetric("検索", searchEngine.label, Modifier.weight(1f))
                        ProfileMetric("エンジン", "yt-dlp", Modifier.weight(1f))
                        ProfileMetric("形式", "H.264", Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionTitle("ブラウザ", Glyph.Compass) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(22.dp)).padding(7.dp)) {
                Row(Modifier.padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Label("検索エンジン", 13.sp, Paper, FontWeight.Bold); Label("アドレス検索とホームに使用", 10.sp, Muted) }
                    Label(searchEngine.label, 10.sp, Acid, FontWeight.Bold)
                }
                SearchEngine.entries.forEach { engine ->
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (engine == searchEngine) Acid.copy(.12f) else Color.Transparent)
                            .noirClick { onSearchEngineChange(engine) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(if (engine == searchEngine) Acid else Elevated), contentAlignment = Alignment.Center) {
                            Label(engine.label.take(1), 11.sp, if (engine == searchEngine) Ink else Muted, FontWeight.Black)
                        }
                        Spacer(Modifier.width(11.dp)); Label(engine.label, 12.sp, if (engine == searchEngine) Paper else Muted, FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (engine == searchEngine) GlyphIcon(Glyph.Check, Acid, 17.dp)
                    }
                }
            }
        }

        item { SectionTitle("ダウンロード", Glyph.ArrowDown) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(22.dp))) {
                ModernSettingRow(Glyph.Stack, "保存先", "Downloads / Veiload")
                Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 15.dp).background(Line))
                ModernSettingRow(Glyph.Play, "動画の互換性", "H.264優先 · 音声を自動結合")
                Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 15.dp).background(Line))
                ModernSettingRow(Glyph.Refresh, "抽出エンジン", "yt-dlp Nightly · 自動更新")
            }
        }

        item { SectionTitle("情報と管理", Glyph.Info) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(22.dp))) {
                ModernSettingRow(Glyph.Gear, "Androidのアプリ設定", "権限・ストレージ・通知") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 15.dp).background(Line))
                ModernSettingRow(Glyph.FileText, "サードパーティーライセンス", "${licenses.size}件", onClick = { licensesExpanded = !licensesExpanded })
            }
        }

        if (licensesExpanded) {
            items(licenses, key = { it.name }) { license ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Elevated.copy(.75f))
                        .noirClick { openExternal(context, license.url) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(.05f)), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.FileText, Muted, 16.dp) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Label(license.name, 12.sp, Paper, FontWeight.Bold)
                        Label("${license.license} · ${license.note}", 9.sp, Muted)
                        Label("ライセンス本文を開く", 8.sp, Acid.copy(.82f), FontWeight.Medium)
                    }
                    GlyphIcon(Glyph.External, Muted, 14.dp)
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(.2f)).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Label(label, 8.sp, Blue.copy(.75f), FontWeight.Bold, letterSpacing = .7f)
        Spacer(Modifier.height(3.dp)); Label(value, 11.sp, Paper, FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun ModernSettingRow(glyph: Glyph, title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).then(if (onClick != null) Modifier.noirClick(onClick = onClick) else Modifier).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(35.dp).clip(RoundedCornerShape(12.dp)).background(Acid.copy(.1f)), contentAlignment = Alignment.Center) { GlyphIcon(glyph, Acid, 17.dp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) { Label(title, 12.sp, Paper, FontWeight.Bold); Spacer(Modifier.height(2.dp)); Label(value, 9.sp, Muted, maxLines = 1) }
        if (onClick != null) GlyphIcon(Glyph.Forward, Muted, 14.dp)
    }
}

@Composable
private fun LegacyMyPageScreen(searchEngine: SearchEngine, onSearchEngineChange: (SearchEngine) -> Unit) {
    val context = LocalContext.current
    val licenses = THIRD_PARTY_LICENSES

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 96.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(Acid.copy(.14f)).border(1.dp, Acid.copy(.28f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.User, Acid, 25.dp)
                }
                Spacer(Modifier.width(13.dp))
                Column {
                    Label("MY VEILOAD", 10.sp, Acid, FontWeight.Bold, letterSpacing = 2f)
                    Spacer(Modifier.height(2.dp))
                    Label("マイページ", 29.sp, Paper, FontWeight.Black, letterSpacing = -.5f)
                }
            }
        }

        item { SectionTitle("アプリ設定", Glyph.Gear) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(.075f), RoundedCornerShape(22.dp)).padding(16.dp)) {
                Label("検索エンジン", 14.sp, Paper, FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Label("検索とホームボタンに使用します", 11.sp, Muted)
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SearchEngine.entries.forEach { engine ->
                        Box(
                            Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(13.dp))
                                .background(if (engine == searchEngine) Acid else Elevated)
                                .border(1.dp, if (engine == searchEngine) Acid else Color.White.copy(.08f), RoundedCornerShape(13.dp))
                                .noirClick { onSearchEngineChange(engine) },
                            contentAlignment = Alignment.Center
                        ) { Label(engine.label, 10.sp, if (engine == searchEngine) Ink else Muted, FontWeight.Bold, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingValue("保存先", "Downloads / Veiload")
                Spacer(Modifier.height(12.dp))
                SettingValue("互換性", "H.264優先 · 音声を自動結合")
            }
        }

        item { SectionTitle("アプリ情報", Glyph.Info) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Color.White.copy(.075f), RoundedCornerShape(22.dp)).padding(16.dp)) {
                SettingValue("Veiload", "Version ${BuildConfig.VERSION_NAME}")
                Spacer(Modifier.height(12.dp))
                SettingValue("プラットフォーム", "Android Native")
                Spacer(Modifier.height(12.dp))
                SettingValue("ダウンロードエンジン", "yt-dlp Nightly")
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).background(Elevated).noirClick {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.Gear, Muted, 17.dp)
                    Spacer(Modifier.width(9.dp)); Label("Androidのアプリ設定を開く", 12.sp, Paper, FontWeight.Bold)
                    Spacer(Modifier.weight(1f)); GlyphIcon(Glyph.External, Muted, 14.dp)
                }
            }
        }

        item { SectionTitle("サードパーティーライセンス", Glyph.FileText) }
        items(licenses, key = { it.name }) { license ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)
                    .border(1.dp, Color.White.copy(.065f), RoundedCornerShape(18.dp))
                    .noirClick { openExternal(context, license.url) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Label(license.name, 13.sp, Paper, FontWeight.Bold)
                    Spacer(Modifier.height(3.dp)); Label("${license.license}  ·  ${license.note}", 10.sp, Muted, maxLines = 2)
                }
                GlyphIcon(Glyph.External, Muted, 15.dp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, glyph: Glyph) {
    Row(Modifier.padding(top = 9.dp, start = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(glyph, Acid, 15.dp)
        Spacer(Modifier.width(8.dp)); Label(text, 11.sp, Muted, FontWeight.Bold, letterSpacing = 1.1f)
    }
}

@Composable
private fun SettingValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(label, 12.sp, Muted, FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Label(value, 12.sp, Paper, FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(30.dp).clip(RoundedCornerShape(11.dp))
        .background(if (selected) Acid else Elevated)
        .border(1.dp, if (selected) Acid else Color.White.copy(.08f), RoundedCornerShape(11.dp))
        .noirClick(onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Label(text, 10.sp, if (selected) Ink else Muted, FontWeight.Bold)
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(19.dp)).background(Elevated).border(1.dp, Color.White.copy(.075f), RoundedCornerShape(19.dp)).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(Glyph.Search, Muted, 19.dp); Spacer(Modifier.width(12.dp))
        BasicTextField(value, onChange, Modifier.weight(1f), singleLine = true, textStyle = TextStyle(Paper, 15.sp), cursorBrush = Brush.verticalGradient(listOf(Acid, Acid)), decorationBox = { inner -> Box { if (value.isBlank()) Label(tr("タイトルを検索", "Search titles"), 14.sp, Muted); inner() } })
        AnimatedVisibility(value.isNotEmpty(), enter = scaleIn(), exit = scaleOut()) { Box(Modifier.size(28.dp).noirClick { onChange("") }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 15.dp) } }
    }
}

@Composable
private fun EmptyLibrary(searching: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-24).dp)) {
            Box(Modifier.size(92.dp).border(1.dp, Line, CircleShape).drawBehind { drawCircle(Acid.copy(.05f)) }, contentAlignment = Alignment.Center) { GlyphIcon(if (searching) Glyph.Search else Glyph.Stack, Muted, 34.dp) }
        Spacer(Modifier.height(22.dp)); Label(if (searching) tr("見つかりません", "Nothing found") else tr("まだ静かな場所です", "It's quiet here"), 19.sp, Paper, FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Label(if (searching) tr("別のキーワードを試してください", "Try another keyword") else tr("ブラウザから動画をダウンロード", "Download a video from the browser"), 13.sp, Muted)
        }
    }
}

@Composable
private fun MediaCard(media: MediaFile, onPlay: (MediaFile) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(23.dp)).background(Panel).border(1.dp, Color.White.copy(.07f), RoundedCornerShape(23.dp)).noirClick { onPlay(media) }.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.aspectRatio(1.25f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Elevated)) {
            VideoThumbnail(media)
            Box(Modifier.align(Alignment.Center).size(37.dp).background(Color.Black.copy(.62f), CircleShape), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Play, Paper, 15.dp) }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Label(media.title, 14.sp, Paper, FontWeight.SemiBold, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SiteFavicon(media.site, 14.dp); Spacer(Modifier.width(7.dp))
                Label(media.site, 10.sp, Muted, FontWeight.Bold); Spacer(Modifier.width(7.dp)); Label("·", 10.sp, Line); Spacer(Modifier.width(7.dp)); Label(formatPlaybackTime(media.durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), 10.sp, Muted); Spacer(Modifier.width(7.dp)); Label("·", 10.sp, Line); Spacer(Modifier.width(7.dp)); Label(formatBytes(media.size), 10.sp, Muted)
            }
        }
        Box(Modifier.size(38.dp).noirClick { menu = !menu }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.More, Muted, 20.dp) }
    }
    AnimatedVisibility(menu, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
        Row(Modifier.fillMaxWidth().padding(top = 5.dp, start = 12.dp, end = 12.dp).clip(RoundedCornerShape(15.dp)).background(Elevated).padding(6.dp)) {
            MiniAction(tr("共有", "Share"), Glyph.Share, Modifier.weight(1f)) { shareFile(context, media) }
            MiniAction(tr("再生", "Play"), Glyph.Play, Modifier.weight(1f)) { onPlay(media) }
            MiniAction(tr("削除", "Delete"), Glyph.Delete, Modifier.weight(1f), Red) { confirmDelete = true }
        }
    }
    if (confirmDelete) {
        DeleteConfirmation(
            media = media,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                menu = false
                DownloadsStore.delete(context, media)
            }
        )
    }
}

private fun siteFaviconFile(context: Context, domain: String): java.io.File = java.io.File(
    context.cacheDir,
    "site-favicons/${domain.lowercase().hashCode().toUInt()}.png"
)

@Composable
private fun SiteFavicon(domain: String, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, domain) {
        value = withContext(Dispatchers.IO) {
        if (domain == tr("不明", "Unknown") || domain.isBlank()) return@withContext null
            val cache = siteFaviconFile(context, domain)
            cache.takeIf { it.exists() && it.length() > 0 }
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?: runCatching {
                    val connection = java.net.URL("https://$domain/favicon.ico").openConnection().apply {
                        connectTimeout = 5_000
                        readTimeout = 7_000
                        setRequestProperty("User-Agent", BrowserIdentity.USER_AGENT)
                    }
                    val icon = connection.getInputStream().use(BitmapFactory::decodeStream)
                    if (icon != null) {
                        cache.parentFile?.mkdirs()
                        cache.outputStream().buffered().use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    icon
                }.getOrNull()
        }
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(3.dp)).background(Elevated), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(5.dp).background(Acid, CircleShape))
        }
    }
}

@Composable
private fun DeleteConfirmation(media: MediaFile, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color(0xFF17171B))
                .border(1.dp, Color.White.copy(.1f), RoundedCornerShape(26.dp)).padding(20.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Red.copy(.13f)),
                contentAlignment = Alignment.Center
            ) { GlyphIcon(Glyph.Delete, Red, 22.dp) }
            Spacer(Modifier.height(16.dp))
            Label(tr("この動画を削除しますか？", "Delete this video?"), 18.sp, Paper, FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Label(media.title, 13.sp, Muted, FontWeight.Medium, maxLines = 2)
            Spacer(Modifier.height(5.dp))
            Label(tr("端末のDownloads / Veiloadから完全に削除されます。", "It will be permanently removed from Downloads / Veiload."), 11.sp, Muted, maxLines = 2)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Elevated)
                        .noirClick(onClick = onDismiss),
                    contentAlignment = Alignment.Center
            ) { Label(tr("キャンセル", "Cancel"), 12.sp, Paper, FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Red)
                        .noirClick(onClick = onConfirm),
                    contentAlignment = Alignment.Center
            ) { Label(tr("削除する", "Delete"), 12.sp, Ink, FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(media: MediaFile) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, media.file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            loadOrCreateVideoThumbnail(context, media)
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF25252C), Color(0xFF111115))))
        repeat(5) { i -> drawLine(Color.White.copy(.04f), Offset(0f, size.height * i / 5f), Offset(size.width, size.height * (i + 2) / 5f), 1f) }
    }
}

private fun loadOrCreateVideoThumbnail(context: Context, media: MediaFile): Bitmap? {
    val cache = java.io.File(
        context.cacheDir,
        "video-thumbnails/${media.file.absolutePath.hashCode().toUInt()}-${media.modified}.jpg"
    )
    cache.takeIf { it.exists() && it.length() > 0 }
        ?.let { BitmapFactory.decodeFile(it.absolutePath) }
        ?.let { return it }
    cache.parentFile?.mkdirs()

    val middleMs = media.durationMs.takeIf { it > 0 } ?: readLocalVideoDuration(media.file.absolutePath)
    val middleUs = (middleMs.coerceAtLeast(2_000L) / 2L) * 1_000L
    val frame = runCatching {
        MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(media.file.absolutePath)
                retriever.getFrameAtTime(middleUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(middleUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } finally { retriever.release() }
        }
    }.getOrNull()
    if (frame != null) {
        runCatching { cache.outputStream().buffered().use { frame.compress(Bitmap.CompressFormat.JPEG, 86, it) } }
        return frame
    }

    val ffmpeg = java.io.File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
    if (!ffmpeg.exists()) return null
    val seekSeconds = (middleMs.coerceAtLeast(2_000L) / 2_000.0).toString()
    return runCatching {
        val process = ProcessBuilder(
            ffmpeg.absolutePath, "-y", "-ss", seekSeconds, "-i", media.file.absolutePath,
            "-frames:v", "1", "-vf", "scale=640:-2:force_original_aspect_ratio=decrease",
            "-q:v", "3", cache.absolutePath
        ).redirectErrorStream(true).start()
        val drainer = Thread {
            process.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                while (input.read(buffer) >= 0) Unit
            }
        }.apply { start() }
        val finished = process.waitFor(25, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        drainer.join(1_000)
        cache.takeIf { finished && process.exitValue() == 0 && it.length() > 0 }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }.getOrNull()
}

private fun readLocalVideoDuration(path: String): Long = runCatching {
    MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally { retriever.release() }
    }
}.getOrDefault(0L)

@Composable
private fun AppVideoPlayer(media: MediaFile, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val videoOrientation by produceState(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, media.file.absolutePath) {
        value = withContext(Dispatchers.IO) { readVideoScreenOrientation(media.file.absolutePath) }
    }
    val player = remember(media.file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(media.file)))
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember(media.file.absolutePath) { mutableStateOf(false) }
    var duration by remember(media.file.absolutePath) { mutableIntStateOf(0) }
    var position by remember(media.file.absolutePath) { mutableIntStateOf(0) }
    var controlsVisible by remember(media.file.absolutePath) { mutableStateOf(true) }
    var controlActivity by remember(media.file.absolutePath) { mutableIntStateOf(0) }
    var playbackSpeed by remember(media.file.absolutePath) { mutableFloatStateOf(1f) }

    LaunchedEffect(controlsVisible, controlActivity, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(2_800L)
            controlsVisible = false
        }
    }

    DisposableEffect(activity, videoOrientation) {
        if (videoOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity?.requestedOrientation = videoOrientation
        }
        onDispose {
            if (videoOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.duration > 0) duration = player.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            position = player.currentPosition.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
            isPlaying = player.isPlaying
            delay(250)
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(media.file.absolutePath, controlsVisible) {
                detectTapGestures(onTap = {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) controlActivity++
                })
            }
    ) {
        AndroidView(
            factory = { context ->
                (LayoutInflater.from(context).inflate(R.layout.view_texture_player, null, false) as PlayerView).apply {
                    useController = false
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(controlsVisible, enter = fadeIn(tween(180)), exit = fadeOut(tween(220)), modifier = Modifier.align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(.68f)).border(1.dp, Color.White.copy(.12f), CircleShape).noirClick(onClick = onClose), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.Back, Paper, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Label(media.title, 14.sp, Paper, FontWeight.Bold, maxLines = 1)
                    Label("VEILOAD PLAYER", 9.sp, Muted, FontWeight.Bold, letterSpacing = 1.2f)
                }
            }
        }

        AnimatedVisibility(controlsVisible, enter = fadeIn(tween(180)), exit = fadeOut(tween(220)), modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.92f)))).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 56.dp, bottom = 20.dp)) {
                PlaybackSeekBar(position, duration) { target ->
                    player.seekTo(target.toLong())
                    position = target
                    controlActivity++
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Label(formatPlaybackTime(position), 11.sp, Paper, FontWeight.Bold)
                    Label("  /  ${formatPlaybackTime(duration)}", 11.sp, Muted)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(.09f))
                            .border(1.dp, Color.White.copy(.1f), RoundedCornerShape(12.dp))
                            .noirClick {
                                playbackSpeed = when (playbackSpeed) {
                                    1f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2f
                                    else -> 1f
                                }
                                player.setPlaybackSpeed(playbackSpeed)
                                controlsVisible = true
                                controlActivity++
                            }.padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center
                    ) { Label("${playbackSpeed}×", 10.sp, Paper, FontWeight.Black) }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Acid).noirClick {
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                        controlsVisible = true
                        controlActivity++
                    }, contentAlignment = Alignment.Center) {
                        GlyphIcon(if (isPlaying) Glyph.Pause else Glyph.Play, Ink, 21.dp, 2.3f)
                    }
                }
            }
        }
    }

    DisposableEffect(media.file.absolutePath) {
        onDispose {
            player.stop()
            player.release()
        }
    }
}

private fun readVideoScreenOrientation(path: String): Int = runCatching {
    MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(path)
            var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation % 180 != 0) width = height.also { height = width }
            when {
                width > height -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                height > width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } finally { retriever.release() }
    }
}.getOrDefault(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

@Composable
private fun PlaybackSeekBar(position: Int, duration: Int, onSeek: (Int) -> Unit) {
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Canvas(Modifier.fillMaxWidth().height(32.dp).pointerInput(duration) {
        detectTapGestures { point ->
            if (duration > 0 && size.width > 0) onSeek((duration * (point.x / size.width).coerceIn(0f, 1f)).toInt())
        }
    }) {
        val center = size.height / 2f
        drawLine(Color.White.copy(.2f), Offset(0f, center), Offset(size.width, center), 4.dp.toPx(), StrokeCap.Round)
        drawLine(Acid, Offset(0f, center), Offset(size.width * fraction, center), 4.dp.toPx(), StrokeCap.Round)
        drawCircle(Acid, 6.dp.toPx(), Offset(size.width * fraction, center))
    }
}

@Composable
private fun DownloadCapsule(active: ActiveDownload, openLibrary: () -> Unit, cancel: () -> Unit, dismiss: () -> Unit) {
    val target = active.progress.coerceIn(0f, 100f)
    val shown by animateFloatAsState(target, tween(500), label = "downloadProgress")
    Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(62.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFA25252A)).border(1.dp, (if (active.error == null) Acid else Red).copy(.24f), RoundedCornerShape(20.dp)).noirClick(onClick = openLibrary).padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        ProgressOrb(shown, active.running, 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Label(active.title, 13.sp, Paper, FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp)); Label(if (active.error != null) active.error else if (active.running) tr("${shown.toInt()}%  ·  残り ${formatEta(active.etaSeconds)}", "${shown.toInt()}%  ·  ${formatEta(active.etaSeconds)} remaining") else "Downloads/Veiload", 10.sp, if (active.error != null) Red else Muted, maxLines = 1)
        }
        if (active.running) {
            Row(
                Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(Red.copy(.12f))
                    .noirClick(onClick = cancel).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphIcon(Glyph.Stop, Red, 13.dp); Spacer(Modifier.width(6.dp)); Label(tr("中止", "Stop"), 10.sp, Red, FontWeight.Bold)
            }
        } else {
            Box(Modifier.size(36.dp).noirClick(onClick = dismiss), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Close, Muted, 15.dp) }
        }
    }
}

@Composable
private fun ProgressOrb(progress: Float, running: Boolean, size: androidx.compose.ui.unit.Dp = 41.dp) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1400)), label = "rotation")
    Canvas(Modifier.size(size).rotate(if (running) rotation else 0f)) {
        drawCircle(Line, style = Stroke(3.dp.toPx()))
        drawArc(if (running) Acid else if (progress >= 100) Acid else Red, -90f, if (running) progress * 3.6f else 360f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun CircleAction(icon: Glyph, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Paper else Color(0xFF44444A)
    Box(Modifier.size(38.dp).clip(CircleShape).noirClick(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) { GlyphIcon(icon, color, 18.dp) }
}

@Composable
private fun MiniAction(text: String, glyph: Glyph, modifier: Modifier, color: Color = Muted, onClick: () -> Unit) {
    Row(modifier.height(34.dp).clip(RoundedCornerShape(11.dp)).noirClick(onClick = onClick), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(glyph, color, 14.dp); Spacer(Modifier.width(7.dp)); Label(text, 11.sp, if (color == Red) Red else Paper, FontWeight.Bold)
    }
}

@Composable
private fun Label(text: String, size: androidx.compose.ui.unit.TextUnit, color: Color, weight: FontWeight = FontWeight.Normal, letterSpacing: Float = 0f, maxLines: Int = Int.MAX_VALUE, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicText(text, modifier = modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight, letterSpacing = letterSpacing.sp), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

private fun Modifier.noirClick(enabled: Boolean = true, onClick: () -> Unit) = composed {
    val source = remember { MutableInteractionSource() }
    clickable(source, indication = null, enabled = enabled, onClick = onClick)
}

private fun normalizeUrl(value: String, searchEngine: SearchEngine): String {
    val clean = value.trim()
    return when {
        clean.startsWith("https://") || clean.startsWith("http://") -> clean
        clean.contains('.') && !clean.contains(' ') -> "https://$clean"
        else -> "${searchEngine.searchUrl}${Uri.encode(clean)}"
    }
}

private fun normalizeDirectDownloadUrl(value: String): String? {
    val clean = value.trim()
    return when {
        clean.startsWith("https://") || clean.startsWith("http://") -> clean
        clean.contains('.') && !clean.contains(' ') -> "https://$clean"
        else -> null
    }
}

private fun mediaCandidateLabel(url: String, index: Int): String {
    val clean = url.substringBefore('?').substringBefore('#')
    val extension = clean.substringAfterLast('.', "").uppercase().takeIf { it.length in 2..5 }
    return tr("動画 ${index + 1}${extension?.let { " · $it" }.orEmpty()}", "Video ${index + 1}${extension?.let { " · $it" }.orEmpty()}")
}

private fun pageMediaCandidate(url: String, title: String): DetectedMedia {
    val videoId = youtubeVideoId(url)
    return DetectedMedia(
        url = url,
        title = title,
        thumbnailUrl = videoId?.takeIf { it.isNotBlank() }?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
    )
}

private fun youtubeVideoId(url: String): String? = runCatching {
    val uri = Uri.parse(url)
    val host = uri.host.orEmpty().lowercase().removePrefix("www.")
    when {
        host == "youtu.be" -> uri.pathSegments.firstOrNull()
        host.endsWith("youtube.com") && uri.path == "/watch" -> uri.getQueryParameter("v")
        host.endsWith("youtube.com") && (uri.path.orEmpty().startsWith("/shorts/") || uri.path.orEmpty().startsWith("/live/")) -> uri.pathSegments.getOrNull(1)
        else -> null
    }
}.getOrNull()?.takeIf { it.isNotBlank() }

private fun formatCandidateDuration(seconds: Double): String = if (seconds > 0 && seconds.isFinite()) {
    formatPlaybackTime((seconds * 1000).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    } else tr("長さ不明", "Unknown length")

private fun formatCandidateSize(bytes: Long): String = if (bytes > 0) formatBytes(bytes) else tr("容量不明", "Unknown size")

private fun parseDirectMediaUrls(result: String): List<String> = runCatching {
    val encoded = org.json.JSONTokener(result).nextValue() as? String ?: return@runCatching emptyList()
    val array = org.json.JSONArray(encoded)
    buildList {
        repeat(array.length()) { index ->
            array.optString(index).takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let(::add)
        }
    }.distinct()
}.getOrDefault(emptyList())

private fun installPageAdSuppression(view: WebView) {
    view.evaluateJavascript(
        """
        (() => {
          if (window.__noirVideoAdBlock) return;
          window.__noirVideoAdBlock = true;
          const removeOverlays = () => {
            document.querySelectorAll([
              'iframe[src*="doubleclick.net"]',
              'iframe[src*="googlesyndication.com"]',
              '.ytp-ad-overlay-container',
              '.ytp-ad-message-container',
              '[data-ad-overlay]',
              '[class*="video-ad-overlay"]'
            ].join(',')).forEach(node => node.remove());
          };
          const skipVideoAds = () => {
            document.querySelectorAll([
              '.ytp-ad-skip-button',
              '.ytp-ad-skip-button-modern',
              'button[class*="ad-skip"]',
              'button[class*="skip-ad"]'
            ].join(',')).forEach(button => { try { button.click(); } catch (_) {} });
            document.querySelectorAll([
              '.html5-video-player.ad-showing video',
              '[class*="ad-container"] video',
              '[id*="ad-container"] video'
            ].join(',')).forEach(video => {
              try {
                video.muted = true;
                video.playbackRate = 16;
                if (Number.isFinite(video.duration) && video.duration > 0) {
                  video.currentTime = Math.max(video.currentTime, video.duration - 0.05);
                }
              } catch (_) {}
            });
            removeOverlays();
          };
          skipVideoAds();
          window.__noirVideoAdTimer = setInterval(skipVideoAds, 450);
        })();
        """.trimIndent(),
        null
    )
}

private fun installEasyListCosmeticFilters(view: WebView, pageUrl: String) {
    if (isSearchEnginePage(pageUrl)) {
        view.evaluateJavascript("document.getElementById('__noir_easylist_cosmetic')?.remove();", null)
        return
    }
    val css = WebAdBlocker.cosmeticCss(pageUrl)
    if (css.isBlank()) return
    val encodedCss = org.json.JSONObject.quote(css)
    view.evaluateJavascript(
        """
        (() => {
          const id = '__noir_easylist_cosmetic';
          let style = document.getElementById(id);
          if (!style) {
            style = document.createElement('style');
            style.id = id;
            (document.head || document.documentElement).appendChild(style);
          }
          style.textContent = $encodedCss;
        })();
        """.trimIndent(),
        null
    )
}

private fun isSearchEnginePage(pageUrl: String): Boolean = runCatching {
    val host = Uri.parse(pageUrl).host.orEmpty().lowercase().removePrefix("www.")
    host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") ||
        host == "bing.com" || host.endsWith(".bing.com") ||
        host == "yandex.com" || host.endsWith(".yandex.com") ||
        host == "search.brave.com" ||
        host == "google.com" || host.startsWith("google.") || host.endsWith(".google.com") ||
        host == "yahoo.co.jp" || host.startsWith("search.yahoo.")
}.getOrDefault(false)

private data class PageMediaAnalysis(
    val hasVideoElement: Boolean,
    val candidates: List<DetectedMedia>,
    val pageTitle: String = ""
)

private fun scheduleStableUrlAnalysis(view: WebView, tab: BrowserTabState, expectedUrl: String) {
    val token = ++tab.analysisToken
    tab.mediaAvailability = MediaAvailability.Analyzing
    tab.mediaCandidates.clear()
    view.postDelayed({
        if (tab.analysisToken == token && view.url == expectedUrl) {
            tab.title = view.title?.takeIf { it.isNotBlank() } ?: Uri.parse(expectedUrl).host.orEmpty()
            analyzeBrowserPage(view, tab, expectedUrl)
        }
    }, 3_000L)
}

private fun analyzeBrowserPage(view: WebView, tab: BrowserTabState, expectedUrl: String) {
    tab.mediaAvailability = MediaAvailability.Analyzing
    view.evaluateJavascript(PAGE_MEDIA_ANALYSIS_SCRIPT) { result ->
        if (view.url != expectedUrl) return@evaluateJavascript
        val analysis = parsePageMediaAnalysis(result)
        analysis.candidates.filterNot { isLikelyVideoAdUrl(it.url) }.forEach { candidate ->
            val index = tab.mediaCandidates.indexOfFirst { it.url == candidate.url }
            if (index < 0) tab.mediaCandidates += candidate
            else {
                val current = tab.mediaCandidates[index]
                tab.mediaCandidates[index] = candidate.copy(
                    thumbnailUrl = candidate.thumbnailUrl ?: current.thumbnailUrl,
                    durationSeconds = candidate.durationSeconds.takeIf { it > 0 } ?: current.durationSeconds,
                    sizeBytes = maxOf(candidate.sizeBytes, current.sizeBytes)
                )
            }
        }
        tab.mediaAvailability = if (
            analysis.hasVideoElement || tab.mediaCandidates.isNotEmpty() || isKnownVideoPage(expectedUrl)
        ) MediaAvailability.Available else MediaAvailability.Unavailable
    }
}

private fun parsePageMediaAnalysis(result: String): PageMediaAnalysis = runCatching {
    val encoded = org.json.JSONTokener(result).nextValue() as? String
        ?: return@runCatching PageMediaAnalysis(false, emptyList())
    val json = org.json.JSONObject(encoded)
    val candidates = json.optJSONArray("items")?.let { array ->
        buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val url = item.optString("url")
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    add(
                        DetectedMedia(
                            url = url,
                            thumbnailUrl = item.optString("poster").takeIf { it.startsWith("https://") || it.startsWith("http://") || it.startsWith("data:image/") },
                            durationSeconds = item.optDouble("duration", 0.0).takeIf { it.isFinite() } ?: 0.0,
                            sizeBytes = item.optLong("size", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }
    }.orEmpty()
    PageMediaAnalysis(
        json.optBoolean("hasVideo"),
        candidates.distinctBy { it.url },
        json.optString("pageTitle").trim()
    )
}.getOrDefault(PageMediaAnalysis(false, emptyList()))

private fun isDirectMediaUrl(url: String): Boolean {
    val clean = url.substringBefore('?').substringBefore('#').lowercase()
    return listOf(".mp4", ".m4v", ".webm", ".mov", ".m3u8", ".mpd", ".m4a", ".mp3").any(clean::endsWith)
}

private fun isVideoDownload(url: String, mimeType: String?, fileName: String): Boolean {
    val extension = fileName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
    val mime = mimeType.orEmpty().lowercase()
    return mime.startsWith("video/") || mime.startsWith("audio/") || mime.contains("mp4") || mime.contains("mpegurl") || mime.contains("dash+xml") ||
        extension in MEDIA_DOWNLOAD_EXTENSIONS ||
        isDirectMediaUrl(url)
}

private val MEDIA_DOWNLOAD_EXTENSIONS = setOf(
    "mp4", "m4v", "webm", "mov", "mkv", "avi", "m3u8", "mpd",
    "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac"
)

private val STANDARD_DOWNLOAD_EXTENSIONS = setOf(
    "pdf", "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "apk", "aab",
    "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "json", "xml",
    "jpg", "jpeg", "png", "gif", "webp", "svg", "avif", "heic",
    "exe", "msi", "dmg", "iso", "deb", "rpm"
)

private fun isSiteMediaDownload(url: String, mimeType: String?, fileName: String): Boolean {
    if (isVideoDownload(url, mimeType, fileName)) return true

    val extension = fileName.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
    if (extension in STANDARD_DOWNLOAD_EXTENSIONS) return false

    val mime = mimeType.orEmpty().substringBefore(';').trim().lowercase()
    if (mime.startsWith("image/") || mime.startsWith("text/") || mime.startsWith("font/")) return false
    if (mime in setOf(
            "application/pdf", "application/zip", "application/x-7z-compressed",
            "application/x-rar-compressed", "application/vnd.android.package-archive"
        ) || mime.startsWith("application/vnd.openxmlformats") || mime.startsWith("application/msword")) return false

    // Generic download endpoints commonly hide MP4/WebM behind these MIME types.
    return mime.isBlank() || mime in setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/binary",
        "application/force-download",
        "application/download"
    )
}

private val standardDownloadNameLock = Any()
private val reservedStandardDownloadNames = mutableSetOf<String>()

private fun reserveUniqueStandardDownloadName(suggested: String): String = synchronized(standardDownloadNameLock) {
    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
    val safe = suggested.replace(Regex("[\\/:*?\"<>|]"), "_").ifBlank { "download" }
    val dot = safe.lastIndexOf('.').takeIf { it > 0 }
    val base = dot?.let { safe.substring(0, it) } ?: safe
    val extension = dot?.let { safe.substring(it) }.orEmpty()
    var candidate = safe
    var suffix = 2
    while (File(directory, candidate).exists() || candidate in reservedStandardDownloadNames) {
        candidate = "$base ($suffix)$extension"
        suffix++
    }
    reservedStandardDownloadNames += candidate
    candidate
}

private fun enqueueStandardDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        Toast.makeText(context, tr("この形式のダウンロードには対応していません", "This file type is not supported"), Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val fileName = reserveUniqueStandardDownloadName(URLUtil.guessFileName(url, contentDisposition, mimeType))
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription(Uri.parse(url).host.orEmpty())
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            mimeType?.takeIf { it.isNotBlank() }?.let(::setMimeType)
            userAgent?.takeIf { it.isNotBlank() }?.let { addRequestHeader("User-Agent", it) }
            CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }
                ?.let { addRequestHeader("Cookie", it) }
        }
        context.getSystemService(DownloadManager::class.java).enqueue(request)
    }.onSuccess {
        Toast.makeText(context, tr("ダウンロードフォルダーに保存します", "Saving to the Downloads folder"), Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, tr("ダウンロードを開始できませんでした", "Could not start the download"), Toast.LENGTH_SHORT).show()
    }
}

private fun isKnownVideoPage(url: String): Boolean = runCatching {
    val uri = Uri.parse(url)
    val host = uri.host.orEmpty().lowercase().removePrefix("www.")
    val path = uri.path.orEmpty().lowercase()
    when {
        host == "youtu.be" -> path.length > 1
        host.endsWith("youtube.com") -> path == "/watch" || path.startsWith("/shorts/") || path.startsWith("/live/")
        host.endsWith("tiktok.com") -> "/video/" in path
        host.endsWith("instagram.com") -> path.startsWith("/reel/") || path.startsWith("/p/") || path.startsWith("/tv/")
        host == "x.com" || host.endsWith("twitter.com") -> "/status/" in path
        host.endsWith("vimeo.com") -> path.trim('/').toLongOrNull() != null
        host.endsWith("twitch.tv") -> path.startsWith("/videos/") || "/clip/" in path
        else -> isDirectMediaUrl(url)
    }
}.getOrDefault(false)

private fun isBackgroundUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

private fun requestBackgroundUnrestricted(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val directRequest = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
    runCatching { context.startActivity(directRequest) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}

private fun openExternal(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun playFile(context: Context, media: MediaFile) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", media.file)
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
}

private fun shareFile(context: Context, media: MediaFile) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", media.file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("video/*").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), tr("動画を共有", "Share video")))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824f)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    else -> "%.0f KB".format(bytes / 1024f)
}

private fun formatEta(seconds: Long): String = when {
        seconds <= 0 -> tr("計算中", "Calculating")
        seconds < 60 -> tr("${seconds}秒", "${seconds}s")
        else -> tr("${seconds / 60}分${seconds % 60}秒", "${seconds / 60}m ${seconds % 60}s")
}

private fun formatPlaybackTime(milliseconds: Int): String {
    val total = (milliseconds.coerceAtLeast(0) / 1000)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private enum class Glyph {
    Compass, Stack, ArrowDown, Lock, Refresh, Back, Forward, Home, Go, External,
    Search, Close, Play, Pause, Stop, Plus, More, Share, Delete, Filter, Check, Link, Clipboard, User, Gear, Info, FileText
}

@Composable
private fun GlyphIcon(glyph: Glyph, color: Color, size: androidx.compose.ui.unit.Dp, stroke: Float = 1.8f) {
    val vector = when (glyph) {
        Glyph.Compass -> PhosphorIcons.Regular.Compass
        Glyph.Stack -> PhosphorIcons.Regular.Stack
        Glyph.ArrowDown -> PhosphorIcons.Regular.DownloadSimple
        Glyph.Lock -> PhosphorIcons.Regular.LockSimple
        Glyph.Refresh -> PhosphorIcons.Regular.ArrowClockwise
        Glyph.Back -> PhosphorIcons.Regular.ArrowLeft
        Glyph.Forward, Glyph.Go -> PhosphorIcons.Regular.ArrowRight
        Glyph.Home -> PhosphorIcons.Regular.House
        Glyph.External -> PhosphorIcons.Regular.ArrowUpRight
        Glyph.Search -> PhosphorIcons.Regular.MagnifyingGlass
        Glyph.Close -> PhosphorIcons.Regular.X
        Glyph.Play -> PhosphorIcons.Regular.Play
        Glyph.Pause -> PhosphorIcons.Regular.Pause
        Glyph.Stop -> PhosphorIcons.Regular.Stop
        Glyph.Plus -> PhosphorIcons.Regular.Plus
        Glyph.More -> PhosphorIcons.Regular.DotsThree
        Glyph.Share -> PhosphorIcons.Regular.ShareNetwork
        Glyph.Delete -> PhosphorIcons.Regular.Trash
        Glyph.Filter -> PhosphorIcons.Regular.Funnel
        Glyph.Check -> PhosphorIcons.Regular.Check
        Glyph.Link -> PhosphorIcons.Regular.LinkSimple
        Glyph.Clipboard -> PhosphorIcons.Regular.ClipboardText
        Glyph.User -> PhosphorIcons.Regular.UserCircle
        Glyph.Gear -> PhosphorIcons.Regular.Gear
        Glyph.Info -> PhosphorIcons.Regular.Info
        Glyph.FileText -> PhosphorIcons.Regular.FileText
    }
    Image(
        imageVector = vector,
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = Modifier.size(size)
    )
}
