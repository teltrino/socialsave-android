package com.sahab.downloader

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.MediaStore
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.concurrent.Executors

class DownloadService : Service() {
    companion object { @Volatile var active = false; private const val CHANNEL = "downloads" }
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("downloads", MODE_PRIVATE) }
    private var lastUpdate = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (active) return START_NOT_STICKY
        startForeground(1, notification("Preparing download…", -1), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        val link = Engine.link(intent?.getStringExtra("url").orEmpty())
        if (link == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        active = true
        prefs.edit().remove("errorDetails").apply()
        val audio = intent?.getBooleanExtra("audio", false) ?: false
        val height = intent?.getIntExtra("height", 720) ?: 720
        val audioQuality = intent?.getStringExtra("audioQuality")?.takeIf { it in listOf("0", "320K", "256K", "192K", "128K") } ?: "0"
        val name = intent?.getStringExtra("title") ?: "New download"
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sahab:download").apply { acquire(6 * 60 * 60 * 1000L) }
        executor.execute { download(link, audio, height, audioQuality, name) }
        return START_NOT_STICKY
    }
    private fun download(link: String, audio: Boolean, height: Int, audioQuality: String, name: String) {
        val staging = File(cacheDir, "download-${System.currentTimeMillis()}")
        try {
            check(staging.mkdirs()) { "Could not prepare download storage" }
            report("Preparing download engine…", -1)
            for (attempt in 0..1) {
                try {
                    Engine.run(this) {
                        val request = Engine.request(this, link, recovery = attempt > 0).apply {
                            addOption("--no-mtime")
                            addOption("--restrict-filenames")
                            addOption("-o", "${staging.absolutePath}/%(title).100s [%(id)s].%(ext)s")
                            if (audio) {
                                addOption("-f", "bestaudio/best")
                                addOption("-x")
                                addOption("--audio-format", "mp3")
                                addOption("--audio-quality", audioQuality)
                            } else {
                                addOption("-f", DownloadSettings.videoFormat(height))
                                addOption("--merge-output-format", "mp4")
                                addOption("--remux-video", "mp4")
                            }
                        }
                        YoutubeDL.getInstance().execute(request, "download") { percent, _, _ ->
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastUpdate > 800) {
                                lastUpdate = now
                                report("Downloading / processing · ${percent.toInt().coerceIn(0, 100)}%", percent.toInt().coerceIn(0, 100))
                            }
                        }
                    }
                    break
                } catch (e: Exception) {
                    if (attempt > 0 || !NetworkFailure.canRetry(DownloadFailure.details(e))) throw e
                    report("Connection interrupted. Refreshing media links and retrying with IPv4…", -1)
                    Thread.sleep(2000)
                }
            }
            report("Saving to Downloads…", -1)
            val files = staging.listFiles()?.filter { it.isFile && it.extension.lowercase() in listOf("mp4", "mkv", "webm", "mp3", "m4a", "opus", "ogg") }.orEmpty()
            check(files.isNotEmpty()) { "The source did not produce a supported media file" }
            files.forEach { publish(it) }
            val record = files.joinToString("\n") { "✓ ${it.name}" }
            val old = prefs.getString("history", "").orEmpty()
            prefs.edit().putString("history", (record + "\n\n" + old).take(12000)).apply()
            report("Download complete · ${files.size} file(s) in Downloads/Tellawy downloader", 100)
            notifyIfAllowed(2, notification("Download complete: $name", 100, false))
        } catch (e: Exception) {
            val details = DownloadFailure.details(e)
            prefs.edit().putString("errorDetails", details).apply()
            val message = "Download failed: ${DownloadFailure.summary(details)}"
            report(message, 0)
            notifyIfAllowed(2, notification("Download failed · open the app for details", 0, false))
        } finally {
            staging.deleteRecursively()
            finishDownload()
        }
    }
    private fun publish(file: File) {
        val mime = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; "opus", "ogg" -> "audio/ogg"
            "webm" -> "video/webm"; "mkv" -> "video/x-matroska"; else -> "video/mp4"
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Tellawy downloader")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create the file in Downloads")
        try {
            val output = contentResolver.openOutputStream(uri) ?: error("Could not open the download file")
            output.use { sink -> file.inputStream().use { it.copyTo(sink) } }
            check(contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) > 0)
        } catch (e: Exception) { contentResolver.delete(uri, null, null); throw e }
    }
    private fun report(message: String, percent: Int) {
        prefs.edit().putString("status", message).putInt("progress", percent).apply()
        notifyIfAllowed(1, notification(message, percent))
    }
    private fun notifyIfAllowed(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            getSystemService(NotificationManager::class.java).notify(id, notification)
        }
    }
    private fun notification(message: String, percent: Int, ongoing: Boolean = true): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Tellawy downloader")
            .setContentText(message)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .apply { if (ongoing) setProgress(100, percent.coerceAtLeast(0), percent < 0) }
            .build()
    }
    private fun finishDownload() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        active = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        YoutubeDL.getInstance().destroyProcessById("download")
        prefs.edit().putString("status", "Download stopped because the background time limit was reached. Please retry.").apply()
        finishDownload()
    }
    override fun onDestroy() { executor.shutdown(); if (wakeLock?.isHeld == true) wakeLock?.release(); super.onDestroy() }
}
