package com.opendedownloader.app.browser

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.opendedownloader.app.R
import com.opendedownloader.app.db.AppDatabase
import com.opendedownloader.app.db.BookmarkEntity
import com.opendedownloader.app.download.DownloadEngine
import java.io.File
import java.util.concurrent.Executors

class BrowserFragment : Fragment() {

    private lateinit var webView: VirtualCursorWebView
    private lateinit var etUrl: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnGo: Button
    private lateinit var btnBookmark: ImageButton
    private lateinit var btnToggleCursor: Button

    private val defaultHomeUrl = "https://www.google.com"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        webView = view.findViewById(R.id.webView)
        etUrl = view.findViewById(R.id.etUrl)
        btnBack = view.findViewById(R.id.btnBack)
        btnForward = view.findViewById(R.id.btnForward)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        btnHome = view.findViewById(R.id.btnHome)
        btnGo = view.findViewById(R.id.btnGo)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnToggleCursor = view.findViewById(R.id.btnToggleCursor)

        setupWebView()
        setupListeners()

        // Check for navigation arguments
        val targetUrl = arguments?.getString("TARGET_URL")
        if (!targetUrl.isNullOrEmpty()) {
            loadUrl(targetUrl)
        } else {
            loadUrl(defaultHomeUrl)
        }

        return view
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    if (url.endsWith(".apk", ignoreCase = true)) {
                        promptApkDownload(url)
                        return true
                    }
                    view?.loadUrl(url)
                    etUrl.setText(url)
                    updateBookmarkStar(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    etUrl.setText(url)
                    updateBookmarkStar(url)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }
        }

        webView.setDownloadListener { url, _, contentDisposition, mimeType, contentLength ->
            promptApkDownload(url)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        btnRefresh.setOnClickListener {
            webView.reload()
        }

        btnHome.setOnClickListener {
            loadUrl(defaultHomeUrl)
        }

        btnGo.setOnClickListener {
            performNavigation()
        }

        etUrl.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performNavigation()
                true
            } else {
                false
            }
        }

        btnBookmark.setOnClickListener {
            val currentUrl = webView.url ?: ""
            val currentTitle = webView.title ?: "Bookmark"
            if (currentUrl.isNotEmpty()) {
                toggleBookmark(currentUrl, currentTitle)
            }
        }

        btnToggleCursor.setOnClickListener {
            val enabled = !webView.isCursorEnabled()
            webView.setCursorEnabled(enabled)
            btnToggleCursor.text = if (enabled) "Cursor On" else "Cursor Off"
            btnToggleCursor.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (enabled) android.graphics.Color.parseColor("#00E5FF") else android.graphics.Color.parseColor("#1E1E1E")
            )
        }

        // Set initial cursor button background
        btnToggleCursor.text = "Cursor On"
        btnToggleCursor.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E5FF"))
    }

    private fun performNavigation() {
        val query = etUrl.text.toString().trim()
        if (query.isNotEmpty()) {
            if (URLUtil.isValidUrl(query) || query.contains(".")) {
                var url = query
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                loadUrl(url)
            } else {
                // Search Google
                val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                loadUrl(searchUrl)
            }
        }
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
        etUrl.setText(url)
        updateBookmarkStar(url)
    }

    private fun promptApkDownload(url: String) {
        val fileName = URLUtil.guessFileName(url, null, "application/vnd.android.package-archive")
        AlertDialog.Builder(requireContext())
            .setTitle("Download APK")
            .setMessage("Do you want to download this file?\n\nFile Name: $fileName\n\nURL: $url")
            .setPositiveButton("Download") { _, _ ->
                val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: requireContext().filesDir
                val downloadId = DownloadEngine.startDownload(requireContext(), url, fileName, downloadsDir)
                Toast.makeText(requireContext(), "Download started! ID: $downloadId", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleBookmark(url: String, title: String) {
        val db = AppDatabase.getInstance(requireContext())
        Executors.newSingleThreadExecutor().execute {
            val existing = db.bookmarkDao().getByUrl(url)
            if (existing != null) {
                db.bookmarkDao().delete(existing)
                activity?.runOnUiThread {
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
                    Toast.makeText(requireContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show()
                }
            } else {
                val bookmark = BookmarkEntity(title = title, url = url, isFavorite = true)
                db.bookmarkDao().insert(bookmark)
                activity?.runOnUiThread {
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
                    Toast.makeText(requireContext(), "Added to bookmarks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBookmarkStar(url: String) {
        val db = AppDatabase.getInstance(requireContext())
        Executors.newSingleThreadExecutor().execute {
            val existing = db.bookmarkDao().getByUrl(url)
            activity?.runOnUiThread {
                if (existing != null) {
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
                } else {
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
                }
            }
        }
    }
}
