package id.aditiya.ytmanifest

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(16, 21, 28)
    private val panel = Color.rgb(27, 35, 45)
    private val accent = Color.rgb(99, 214, 197)
    private val mainText = Color.rgb(238, 243, 248)
    private val mutedText = Color.rgb(164, 177, 191)

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private lateinit var titleText: TextView
    private lateinit var streamText: TextView
    private lateinit var extractButton: Button
    private lateinit var playButton: Button
    private lateinit var copyButton: Button
    private lateinit var playerView: PlayerView

    private var player: ExoPlayer? = null
    private var currentStreamUrl: String? = null
    private var extracting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()

        try {
            YoutubeDL.getInstance().init(applicationContext)
            setStatus("Mesin yt-dlp siap. Tempel URL YouTube publik.")
        } catch (e: YoutubeDLException) {
            setStatus("Gagal menyiapkan yt-dlp: ${safeMessage(e)}")
            extractButton.isEnabled = false
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(TextView(this).apply {
            text = "YT MANIFEST"
            textSize = 24f
            setTextColor(accent)
            letterSpacing = 0.08f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Extractor & native player"
            textSize = 14f
            setTextColor(mutedText)
            setPadding(0, dp(4), 0, dp(18))
        })

        val inputCard = card()
        inputCard.addView(TextView(this).apply {
            text = "URL VIDEO / LIVE"
            textSize = 12f
            setTextColor(mutedText)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        urlInput = EditText(this).apply {
            hint = "https://www.youtube.com/watch?v=..."
            setHintTextColor(Color.rgb(111, 125, 140))
            setTextColor(mainText)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(false)
            minLines = 2
            maxLines = 3
            background = null
            setPadding(0, dp(10), 0, dp(10))
        }
        inputCard.addView(urlInput, matchWrap())

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        extractButton = Button(this).apply {
            text = "EKSTRAK"
            isAllCaps = false
            setTextColor(Color.rgb(10, 24, 29))
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { extractUrl() }
        }
        buttonRow.addView(extractButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        val pasteButton = Button(this).apply {
            text = "TEMPEL"
            isAllCaps = false
            setTextColor(mainText)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(56, 69, 83)))
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    urlInput.setText(clip.getItemAt(0).coerceToText(this@MainActivity))
                }
            }
        }
        val pasteParams = LinearLayout.LayoutParams(dp(104), dp(48)).apply { marginStart = dp(8) }
        buttonRow.addView(pasteButton, pasteParams)
        inputCard.addView(buttonRow, matchWrap())
        content.addView(inputCard, matchWrap())

        statusText = TextView(this).apply {
            text = "Siap."
            textSize = 13f
            setTextColor(mutedText)
            setPadding(dp(2), dp(12), dp(2), dp(12))
        }
        content.addView(statusText, matchWrap())

        playerView = PlayerView(this).apply {
            useController = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setBackgroundColor(Color.BLACK)
        }
        val playerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(220)
        ).apply { bottomMargin = dp(14) }
        content.addView(playerView, playerParams)

        val resultCard = card()
        resultCard.addView(TextView(this).apply {
            text = "HASIL EKSTRAKSI"
            textSize = 12f
            setTextColor(accent)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        titleText = TextView(this).apply {
            text = "Belum ada stream"
            textSize = 16f
            setTextColor(mainText)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
        resultCard.addView(titleText, matchWrap())
        streamText = TextView(this).apply {
            text = "URL hasil ekstraksi akan muncul di sini."
            textSize = 12f
            setTextColor(mutedText)
            setTextIsSelectable(true)
            setPadding(0, 0, 0, dp(10))
        }
        resultCard.addView(streamText, matchWrap())

        val resultActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        playButton = Button(this).apply {
            text = "PUTAR"
            isAllCaps = false
            isEnabled = false
            setTextColor(Color.rgb(10, 24, 29))
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { playCurrentStream() }
        }
        resultActions.addView(playButton, LinearLayout.LayoutParams(0, dp(46), 1f))
        copyButton = Button(this).apply {
            text = "SALIN URL"
            isAllCaps = false
            isEnabled = false
            setTextColor(mainText)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(56, 69, 83)))
            setOnClickListener { copyStreamUrl() }
        }
        val copyParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) }
        resultActions.addView(copyButton, copyParams)
        resultCard.addView(resultActions, matchWrap())
        content.addView(resultCard, matchWrap())

        content.addView(TextView(this).apply {
            text = "Catatan: hanya URL publik yang bisa diakses tanpa login/cookies. Link YouTube Kids khusus, video privat/terbatas, dan konten DRM tidak dipaksa dibuka. URL stream dapat kedaluwarsa; ekstrak ulang bila gagal diputar."
            textSize = 11f
            setTextColor(mutedText)
            setPadding(dp(2), dp(14), dp(2), 0)
        }, matchWrap())

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setContentView(root)
    }

    private fun extractUrl() {
        if (extracting) return
        val input = urlInput.text.toString().trim()
        val uri = runCatching { Uri.parse(input) }.getOrNull()
        val host = uri?.host?.lowercase().orEmpty()
        val path = uri?.path.orEmpty().lowercase()

        if (input.isBlank() || uri == null || uri.scheme !in listOf("http", "https")) {
            setStatus("Masukkan URL YouTube yang valid.")
            return
        }
        if (host == "kids.youtube.com" || host.endsWith(".kids.youtube.com") || path.startsWith("/kids")) {
            setStatus("URL YouTube Kids khusus tetap dibuka melalui aplikasi YouTube Kids resmi.")
            return
        }
        val allowedHost = host == "youtu.be" || host == "youtube.com" ||
            host.endsWith(".youtube.com")
        if (!allowedHost) {
            setStatus("Host tidak didukung. Gunakan URL youtube.com atau youtu.be.")
            return
        }

        extracting = true
        extractButton.isEnabled = false
        playButton.isEnabled = false
        copyButton.isEnabled = false
        currentStreamUrl = null
        titleText.text = "Sedang mengambil informasi…"
        streamText.text = ""
        setStatus("yt-dlp sedang mencari format HLS / stream terbaik…")

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val request = YoutubeDLRequest(input).apply {
                        addOption("--no-playlist")
                        addOption("--no-cookies")
                        addOption("--no-cookies-from-browser")
                        addOption("--no-cache-dir")
                        addOption("--no-warnings")
                        // Prefer a playable HLS manifest, then fall back to the best single stream.
                        addOption("-f", "best[protocol^=m3u8_native]/best[protocol^=m3u8]/best")
                    }
                    YoutubeDL.getInstance().getInfo(request)
                }
                val mediaUrl = result.url?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                    ?: throw IllegalStateException("yt-dlp tidak mengembalikan URL media yang bisa diputar.")

                currentStreamUrl = mediaUrl
                titleText.text = result.title?.takeIf { it.isNotBlank() } ?: "Video YouTube"
                streamText.text = mediaUrl
                playButton.isEnabled = true
                copyButton.isEnabled = true
                setStatus("Berhasil. Format terbaik yang tersedia sudah diekstrak.")
            } catch (e: Exception) {
                titleText.text = "Ekstraksi belum berhasil"
                streamText.text = ""
                setStatus(explainError(e))
            } finally {
                extracting = false
                extractButton.isEnabled = true
            }
        }
    }

    private fun playCurrentStream() {
        val url = currentStreamUrl ?: return
        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(
                    mapOf(
                        "Referer" to "https://www.youtube.com/",
                        "Origin" to "https://www.youtube.com",
                        "Accept" to "*/*"
                    )
                )
                .setAllowCrossProtocolRedirects(true)
            val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            player?.release()
            player = ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .also { exo ->
                    playerView.player = exo
                    exo.addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            setStatus("Pemutaran gagal: ${error.errorCodeName}. Coba ekstrak ulang; URL bisa kedaluwarsa atau format tidak didukung.")
                        }
                    })
                    exo.setMediaItem(MediaItem.fromUri(url))
                    exo.prepare()
                    exo.playWhenReady = true
                }
            setStatus("Memutar URL stream langsung dengan ExoPlayer.")
        } catch (e: Exception) {
            setStatus("Gagal memulai player: ${safeMessage(e)}")
        }
    }

    private fun copyStreamUrl() {
        val url = currentStreamUrl ?: return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("YouTube stream URL", url))
        Toast.makeText(this, "URL stream disalin", Toast.LENGTH_SHORT).show()
    }

    private fun explainError(error: Throwable): String {
        val raw = safeMessage(error)
        val lower = raw.lowercase()
        return when {
            "private" in lower || "login" in lower || "sign in" in lower ->
                "Video ini memerlukan akses/login. Aplikasi tidak memakai cookies atau melewati pembatasan akun."
            "not available" in lower || "unavailable" in lower ->
                "Video tidak tersedia untuk ekstraksi publik. Periksa URL atau gunakan pemutar resmi."
            "unsupported url" in lower ->
                "URL tidak dikenali oleh yt-dlp. Pastikan tautan YouTube benar."
            else -> "Ekstraksi gagal: $raw"
        }
    }

    private fun safeMessage(error: Throwable): String =
        error.message?.take(240)?.ifBlank { error.javaClass.simpleName } ?: error.javaClass.simpleName

    private fun setStatus(message: String) {
        if (::statusText.isInitialized) statusText.text = message
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(panel)
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.bottomMargin = dp(12)
        layoutParams = p
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
