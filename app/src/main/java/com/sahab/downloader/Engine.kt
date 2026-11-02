package com.sahab.downloader

import android.content.Context
import android.util.AtomicFile
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

object Engine {
    private val lock = ReentrantLock()
    private var initialized = false
    private val updater = Executors.newSingleThreadExecutor()
    private val updateRunning = AtomicBoolean(false)

    // Only extraction and the brief atomic engine install share this lock.
    // Update network requests and lightweight previews never hold it.
    fun <T> run(context: Context, action: () -> T): T {
        lock.lockInterruptibly()
        try {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            prepare(context.applicationContext)
            return action()
        } finally { lock.unlock() }
    }

    private fun prepare(context: Context) {
        if (initialized) return
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        val prefs = context.getSharedPreferences("engine", Context.MODE_PRIVATE)
        val manifest = JSONObject(context.assets.open("engine/version.json").bufferedReader().use { it.readText() }.trimStart('\uFEFF'))
        val bundled = manifest.getString("version")
        val binary = binary(context)
        if (!binary.exists() || prefs.getString("version", "").orEmpty() < bundled) {
            val bytes = context.assets.open("engine/yt-dlp").use { it.readBytes() }
            check(sha256(bytes) == manifest.getString("sha256")) { "Bundled engine integrity check failed" }
            install(binary, bytes)
            prefs.edit().putString("version", bundled).apply()
        }
        initialized = true
    }

    private fun binary(context: Context) = File(context.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun install(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(bytes); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }

    // Called on launch. Rotation/quick reopens share a five-minute cooldown.
    fun autoUpdate(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("engine", Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - prefs.getLong("lastLaunchCheck", 0) < 5 * 60 * 1000L || !updateRunning.compareAndSet(false, true)) return
        prefs.edit().putLong("lastLaunchCheck", System.currentTimeMillis()).apply()
        updater.execute {
            try {
                val release = JSONObject(fetch("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest", 2 * 1024 * 1024).toString(Charsets.UTF_8))
                val version = release.getString("tag_name")
                check(Regex("\\d{4}\\.\\d{2}\\.\\d{2}(?:\\.\\d+)?").matches(version)) { "Unexpected engine version" }
                val bundled = JSONObject(app.assets.open("engine/version.json").bufferedReader().use { it.readText() }.trimStart('\uFEFF')).getString("version")
                val installed = prefs.getString("version", bundled) ?: bundled
                if (version > maxOf(installed, bundled)) {
                    val assets = release.getJSONArray("assets")
                    var downloadUrl = ""
                    var sumsUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        when (asset.getString("name")) {
                            "yt-dlp" -> downloadUrl = asset.getString("browser_download_url")
                            "SHA2-256SUMS" -> sumsUrl = asset.getString("browser_download_url")
                        }
                    }
                    check(downloadUrl.startsWith("https://github.com/yt-dlp/yt-dlp/releases/") && sumsUrl.startsWith("https://github.com/yt-dlp/yt-dlp/releases/")) { "Unexpected engine download source" }
                    val sums = fetch(sumsUrl, 1024 * 1024).toString(Charsets.UTF_8)
                    val expected = sums.lineSequence().map { it.trim().split(Regex("\\s+")) }.firstOrNull { it.size == 2 && it[1] == "yt-dlp" }?.first() ?: error("Missing engine checksum")
                    val bytes = fetch(downloadUrl, 32 * 1024 * 1024)
                    check(sha256(bytes).equals(expected, true)) { "Downloaded engine integrity check failed" }
                    // A running extraction can finish; its engine is never replaced underneath it.
                    run(app) {
                        if (version > prefs.getString("version", "").orEmpty()) {
                            install(binary(app), bytes)
                            prefs.edit().putString("version", version).apply()
                        }
                    }
                }
                prefs.edit().putString("updateStatus", "Engine checked automatically on launch").apply()
            } catch (_: Exception) {
                prefs.edit().putString("updateStatus", "Update check unavailable; using the installed engine").apply()
            } finally { updateRunning.set(false) }
        }
    }

    private fun fetch(address: String, limit: Int): ByteArray {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "Tellawy-downloader/${BuildConfig.VERSION_NAME}")
        try {
            check(connection.responseCode in 200..299) { "Update server returned ${connection.responseCode}" }
            val bytes = connection.inputStream.use { it.readBytesBounded(limit) }
            return bytes
        } finally { connection.disconnect() }
    }

    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            check(out.size() + count <= limit) { "Response too large" }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    fun version(context: Context): String = context.getSharedPreferences("engine", Context.MODE_PRIVATE).getString("version", "Preparing…").orEmpty()

    fun request(context: Context, link: String, recovery: Boolean = false) = YoutubeDLRequest(link).apply {
        addOption("--no-playlist")
        addOption("--cache-dir", File(context.cacheDir, "yt-dlp").absolutePath)
        addOption("--socket-timeout", "60")
        addOption("--retries", "3")
        addOption("--fragment-retries", "3")
        addOption("--extractor-retries", "3")
        addOption("--retry-sleep", "http:exp=1:4")
        addOption("--retry-sleep", "fragment:exp=1:4")
        addOption("--retry-sleep", "extractor:exp=1:4")
        addOption("--abort-on-unavailable-fragments")
        addOption("--remote-components", "ejs:github")
        addOption("--color", "never")
        if (recovery) addOption("--force-ipv4")
    }

    fun link(text: String): String? {
        val candidate = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(text)?.value ?: return null
        val uri = android.net.Uri.parse(candidate)
        return candidate.takeIf { uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null }
    }
}
