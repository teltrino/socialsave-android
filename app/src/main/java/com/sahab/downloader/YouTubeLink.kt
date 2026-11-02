package com.sahab.downloader

import java.net.URI
import java.net.URLDecoder

object YouTubeLink {
    fun videoId(link: String): String? = try {
        val uri = URI(link)
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val candidate = when {
            host == "youtu.be" || host == "www.youtu.be" -> path.firstOrNull()
            host == "youtube.com" || host.endsWith(".youtube.com") || host == "www.youtube-nocookie.com" -> {
                if (path.firstOrNull() in listOf("shorts", "live", "embed", "v")) path.getOrNull(1)
                else uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("v=") }?.substringAfter("v=")?.let { URLDecoder.decode(it, "UTF-8") }
            }
            else -> null
        }
        candidate?.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
    } catch (_: Exception) { null }
}
