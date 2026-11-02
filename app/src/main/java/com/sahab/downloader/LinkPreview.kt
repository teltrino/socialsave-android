package com.sahab.downloader

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MediaPreview(val title: String, val creator: String, val duration: Long = 0)

object LinkPreview {
    fun load(context: Context, link: String): MediaPreview {
        val id = YouTubeLink.videoId(link)
        val cache = context.getSharedPreferences("previews", Context.MODE_PRIVATE)
        val key = id ?: link
        cache.getString(key, null)?.let { value ->
            try {
                val info = JSONObject(value)
                if (System.currentTimeMillis() - info.getLong("cachedAt") < 24 * 60 * 60 * 1000L) return decode(info)
            } catch (_: Exception) { cache.edit().remove(key).apply() }
        }
        val preview = if (id != null) {
            try { youtubePreview(id) }
            catch (e: Exception) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                extractorPreview(context, link, true)
            }
        } else extractorPreview(context, link, false)
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        val value = JSONObject().put("title", preview.title).put("creator", preview.creator)
            .put("duration", preview.duration).put("cachedAt", System.currentTimeMillis())
        if (cache.all.size >= 100 && !cache.contains(key)) cache.edit().clear().apply()
        cache.edit().putString(key, value.toString()).apply()
        return preview
    }

    private fun youtubePreview(id: String): MediaPreview {
        val canonical = "https://www.youtube.com/watch?v=$id"
        val connection = URL("https://www.youtube.com/oembed?url=${URLEncoder.encode(canonical, "UTF-8")}&format=json").openConnection() as HttpURLConnection
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.setRequestProperty("User-Agent", "Tellawy-downloader/${BuildConfig.VERSION_NAME}")
        try {
            check(connection.responseCode == 200) { "Preview is unavailable" }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val title = json.getString("title")
            check(title.isNotBlank()) { "Preview has no title" }
            return MediaPreview(title, json.optString("author_name", ""))
        } finally { connection.disconnect() }
    }

    private fun extractorPreview(context: Context, link: String, youtube: Boolean): MediaPreview = Engine.run(context) {
        val request = Engine.request(context, link).apply {
            addOption("--dump-single-json")
            addOption("--skip-download")
            addOption("--ignore-no-formats-error")
            addOption("--socket-timeout", "10")
            addOption("--extractor-retries", "1")
            if (youtube) addOption("--extractor-args", "youtube:player_skip=js;skip=hls,dash")
        }
        val info = JSONObject(YoutubeDL.getInstance().execute(request, "preview-${System.nanoTime()}", null).out)
        MediaPreview(info.optString("title", "Untitled media"), info.optString("uploader", "").takeUnless { it == "null" }.orEmpty(), info.optDouble("duration", 0.0).toLong())
    }

    private fun decode(json: JSONObject) = MediaPreview(json.getString("title"), json.optString("creator"), json.optLong("duration"))
}
