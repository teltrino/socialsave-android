package com.sahab.downloader

object DownloadSettings {
    val videoLabels = listOf("720p", "1080p", "480p", "Best available")
    val audioLabels = listOf("Best available (VBR)", "320 kbps", "256 kbps", "192 kbps", "128 kbps")
    fun height(index: Int) = intArrayOf(720, 1080, 480, 0).getOrElse(index) { 720 }
    fun audioQuality(index: Int) = listOf("0", "320K", "256K", "192K", "128K").getOrElse(index) { "0" }
    fun videoFormat(height: Int): String {
        val limit = if (height > 0) "[height<=$height]" else ""
        return "bv*${limit}[ext=mp4][vcodec^=avc1][protocol=https]+ba[ext=m4a][protocol=https]/" +
            "b${limit}[ext=mp4][protocol=https]/" +
            "bv*${limit}[ext=mp4]+ba[ext=m4a]/b${limit}[ext=mp4]/bv*$limit+ba/b$limit"
    }
}

object DownloadFailure {
    fun details(error: Throwable): String = (error.message ?: error.javaClass.simpleName)
        .replace(Regex("\u001B\\[[;\\d]*m"), "").takeLast(24000)

    fun summary(details: String): String {
        if (NetworkFailure.isDns(details)) return "Cannot reach the media server (DNS lookup failed). Check your connection or Private DNS settings and retry."
        val lines = details.lines().map { it.trim() }.filter { it.isNotBlank() }
        val error = lines.lastOrNull { it.startsWith("ERROR:") }?.removePrefix("ERROR:")?.trim()
        return (error?.takeIf { it.isNotEmpty() }
            ?: lines.lastOrNull { !it.startsWith("WARNING:") && !it.startsWith("https://") }
            ?: "Unknown error. Open error details for more information.").take(280)
    }
}

object NetworkFailure {
    fun isDns(details: String): Boolean {
        val text = details.lowercase()
        return listOf("no address associated with hostname", "name or service not known", "temporary failure in name resolution", "unable to resolve host", "unknownhostexception").any { it in text }
    }

    fun canRetry(details: String): Boolean {
        val text = details.lowercase()
        return isDns(text) || listOf("timed out", "connection reset", "network is unreachable", "connection aborted", "http error 403", "http error 502", "http error 503", "http error 504").any { it in text }
    }
}
