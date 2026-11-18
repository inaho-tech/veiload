package dev.noirload.app.download

import android.net.Uri

private val videoAdHosts = setOf(
    "imasdk.googleapis.com",
    "pubads.g.doubleclick.net",
    "securepubads.g.doubleclick.net",
    "googleads.g.doubleclick.net"
)

private val videoAdMarkers = listOf(
    "/vast/", "/vast?", "/vmap/", "/vmap?", "/preroll/", "/midroll/",
    "/adbreak/", "video-ad", "video_ad", "instream-ad", "linear-ad",
    "ad_type=video", "adtype=video", "adunit=video", "ad_break="
)

internal fun isLikelyVideoAdUrl(url: String): Boolean {
    val host = runCatching { Uri.parse(url).host.orEmpty().lowercase() }.getOrDefault("")
    if (videoAdHosts.any { host == it || host.endsWith(".$it") }) return true
    return videoAdMarkers.any { url.contains(it, ignoreCase = true) }
}
