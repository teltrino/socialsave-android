package com.sahab.downloader

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : Activity() {
    private lateinit var url: EditText
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var metadata: TextView
    private lateinit var history: TextView
    private lateinit var progress: ProgressBar
    private lateinit var download: Button
    private lateinit var errorDetails: Button
    private lateinit var mode: Spinner
    private lateinit var videoQuality: Spinner
    private lateinit var audioQuality: Spinner
    private lateinit var videoLabel: TextView
    private lateinit var audioLabel: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val inspector = Executors.newSingleThreadExecutor()
    private var previewTask: Future<*>? = null
    private val prefs by lazy { getSharedPreferences("downloads", MODE_PRIVATE) }
    private var checking = false
    private var generation = 0
    private var previewPending: String? = null
    private var previewDueAt = 0L
    private var inspectedUrl: String? = null
    private var previewError = ""
    private val refresh = object : Runnable {
        override fun run() {
            val active = DownloadService.active
            if (!active && !checking && previewPending != null && SystemClock.elapsedRealtime() >= previewDueAt) loadPreview()
            status.text = prefs.getString("status", "Ready to download")
            val percent = prefs.getInt("progress", 0)
            progress.isIndeterminate = active && percent < 0
            progress.progress = percent.coerceAtLeast(0)
            download.isEnabled = !active && Engine.link(url.text.toString()) != null
            errorDetails.visibility = if (previewError.isNotEmpty() || prefs.getString("errorDetails", "").orEmpty().isNotEmpty()) View.VISIBLE else View.GONE
            history.text = prefs.getString("history", "").orEmpty().ifBlank { "Your completed downloads will appear here." }
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(245, 246, 239))
        }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(24), dp(24) + bars.top, dp(24), dp(24) + bars.bottom)
            insets
        }
        fun label(text: String, size: Float = 16f) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.rgb(24, 48, 44))
            setPadding(0, dp(8), 0, dp(8)); root.addView(this)
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text; isAllCaps = false; setOnClickListener { action() }
            root.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            minHeight = dp(52)
        }
        fun spinner(items: List<String>) = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            minimumHeight = dp(48); root.addView(this)
        }
        label("Tellawy downloader", 29f).setTypeface(null, Typeface.BOLD)
        label("Your link. Your download.", 20f)
        label("Share a link to Tellawy downloader, or paste it below.")
        url = EditText(this).apply {
            hint = "Paste a video or audio link"; textDirection = View.TEXT_DIRECTION_LTR
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true); setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16).toFloat() }
            root.addView(this, LinearLayout.LayoutParams(-1, dp(60)))
        }
        button("Paste link") {
            val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this).toString() else ""
            val link = Engine.link(text)
            if (link == null) toast("No valid link in your clipboard") else url.setText(link)
        }
        title = label("Add a link to see its details", 20f).apply { setTypeface(null, Typeface.BOLD) }
        metadata = label("Link details will appear automatically.", 14f)
        label("Download type")
        mode = spinner(listOf("MP4 video", "MP3 audio"))
        videoLabel = label("Video quality · maximum resolution")
        videoQuality = spinner(DownloadSettings.videoLabels)
        audioLabel = label("MP3 audio quality · target bitrate")
        audioQuality = spinner(DownloadSettings.audioLabels)
        mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { showQuality(position == 1) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        showQuality(false)
        label("Available quality depends on the source. A higher MP3 bitrate cannot improve the original audio.", 13f)
        download = button("↓  Download") { startDownload() }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(19, 125, 112)); setTextColor(Color.WHITE)
            isEnabled = false
        }
        status = label("Ready to download")
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; root.addView(this) }
        errorDetails = button("View error details") { showError() }.apply { visibility = View.GONE }
        label("Your downloads", 23f).setTypeface(null, Typeface.BOLD)
        label("Saved to Downloads / Tellawy downloader")
        history = label("")
        button("Open downloads") {
            try { startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)) }
            catch (_: ActivityNotFoundException) { toast("Open Downloads in your file manager") }
        }
        label("Download support updates automatically when you open the app.", 13f)
        label("One download at a time. Some sources require sign-in or do not support downloading.", 13f)
        button("About & open-source licenses") {
            val updateStatus = getSharedPreferences("engine", MODE_PRIVATE).getString("updateStatus", "Update checks run automatically on launch")
            val notice = "Tellawy downloader ${BuildConfig.VERSION_NAME}\nFree software under GPLv3, without warranty.\n\nEngine: yt-dlp ${Engine.version(this)}\n$updateStatus\nAndroid wrapper: youtubedl-android 0.18.1\nhttps://github.com/yausername/youtubedl-android\nhttps://github.com/yt-dlp/yt-dlp\nMedia processing: FFmpeg\nhttps://ffmpeg.org\n\nMake the accompanying source archive available when sharing the app.\n\n"
            showText("About Tellawy downloader", notice + assets.open("GPL-3.0.txt").bufferedReader().use { it.readText() })
        }
        url.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { queuePreview(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        if (!DownloadService.active) prefs.edit().putString("status", "Ready to download").apply()
        if (savedInstanceState != null) {
            mode.setSelection(savedInstanceState.getInt("mode"))
            videoQuality.setSelection(savedInstanceState.getInt("videoQuality"))
            audioQuality.setSelection(savedInstanceState.getInt("audioQuality"))
            url.setText(savedInstanceState.getString("url", ""))
        } else acceptShare(intent)
    }

    private fun showQuality(audio: Boolean) {
        videoLabel.visibility = if (audio) View.GONE else View.VISIBLE
        videoQuality.visibility = videoLabel.visibility
        audioLabel.visibility = if (audio) View.VISIBLE else View.GONE
        audioQuality.visibility = audioLabel.visibility
    }

    private fun queuePreview(text: String) {
        generation++
        previewTask?.cancel(true)
        checking = false
        inspectedUrl = null
        previewError = ""
        download.isEnabled = false
        val link = Engine.link(text)
        previewPending = link
        previewDueAt = SystemClock.elapsedRealtime() + 200
        title.text = if (link == null) "Add a link to see its details" else "Loading link details…"
        metadata.text = if (link == null) "Enter a valid http or https link." else "Fetching a quick preview. You can start downloading now."
    }

    private fun loadPreview() {
        val link = previewPending ?: return
        previewPending = null
        checking = true
        val token = generation
        previewTask = inspector.submit {
            try {
                val info = LinkPreview.load(applicationContext, link)
                val name = info.title
                val creator = info.creator
                val duration = info.duration
                val time = if (duration > 0) "%d:%02d".format(duration / 60, duration % 60) else ""
                runOnUiThread {
                    if (!isDestroyed && token == generation) {
                        title.text = name
                        metadata.text = listOf(creator, time).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Choose your format and quality below." }
                        inspectedUrl = link
                        checking = false
                    }
                }
            } catch (_: InterruptedException) {
                // A new link or a destroyed Activity superseded this request.
            } catch (e: Exception) {
                val details = DownloadFailure.details(e)
                runOnUiThread {
                    if (!isDestroyed && token == generation) {
                        title.text = "Could not load link details"
                        metadata.text = DownloadFailure.summary(details)
                        previewError = details
                        checking = false
                    }
                }
            }
        }
    }

    private fun startDownload() {
        val link = Engine.link(url.text.toString()) ?: return toast("Enter a valid http or https link")
        if (DownloadService.active) return
        generation++
        previewTask?.cancel(true)
        checking = false
        previewPending = null
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        previewError = ""
        prefs.edit().remove("errorDetails").apply()
        startForegroundService(Intent(this, DownloadService::class.java).apply {
            putExtra("url", link)
            putExtra("audio", mode.selectedItemPosition == 1)
            putExtra("height", DownloadSettings.height(videoQuality.selectedItemPosition))
            putExtra("audioQuality", DownloadSettings.audioQuality(audioQuality.selectedItemPosition))
            putExtra("title", if (inspectedUrl == link) title.text.toString() else "New download")
        })
    }

    private fun showError() {
        val details = if (previewError.isNotEmpty()) previewError else prefs.getString("errorDetails", "No error details available.").orEmpty()
        showText("Error details", details, true)
    }
    private fun showText(heading: String, contents: String, copy: Boolean = false) {
        val text = TextView(this).apply { this.text = contents; setPadding(dp(20), dp(16), dp(20), dp(16)); setTextIsSelectable(true) }
        val dialog = AlertDialog.Builder(this).setTitle(heading).setView(ScrollView(this).apply { addView(text) }).setPositiveButton("Close", null)
        if (copy) dialog.setNeutralButton("Copy details") { _, _ ->
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Download error", detailsForCopy(contents)))
            toast("Error details copied")
        }
        dialog.show()
    }
    private fun detailsForCopy(details: String) = "Tellawy downloader ${BuildConfig.VERSION_NAME}\nEngine: ${Engine.version(this)}\nAndroid: ${Build.VERSION.RELEASE}\n\n$details"
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); acceptShare(intent) }
    private fun acceptShare(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) Engine.link(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())?.let { url.setText(it) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("url", url.text.toString()); outState.putInt("mode", mode.selectedItemPosition)
        outState.putInt("videoQuality", videoQuality.selectedItemPosition); outState.putInt("audioQuality", audioQuality.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }
    override fun onStart() { super.onStart(); Engine.autoUpdate(applicationContext); handler.post(refresh) }
    override fun onStop() { handler.removeCallbacks(refresh); super.onStop() }
    override fun onDestroy() { generation++; previewTask?.cancel(true); inspector.shutdownNow(); super.onDestroy() }
    private fun toast(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
