package com.opendedownloader.app

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.opendedownloader.app.bookmarks.BookmarksFragment
import com.opendedownloader.app.browser.BrowserFragment
import com.opendedownloader.app.downloads.DownloadsFragment
import com.opendedownloader.app.github.GitHubFragment
import com.opendedownloader.app.updater.UpdaterFragment

class MainActivity : AppCompatActivity() {

    private lateinit var navBrowser: Button
    private lateinit var navDownloads: Button
    private lateinit var navBookmarks: Button
    private lateinit var navGitHub: Button
    private lateinit var navUpdater: Button

    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navBrowser = findViewById(R.id.navBrowser)
        navDownloads = findViewById(R.id.navDownloads)
        navBookmarks = findViewById(R.id.navBookmarks)
        navGitHub = findViewById(R.id.navGitHub)
        navUpdater = findViewById(R.id.navUpdater)

        setupSidebarNavigation()

        // Default to Browser screen on startup
        if (savedInstanceState == null) {
            switchFragment(BrowserFragment(), navBrowser)
        }
    }

    private fun setupSidebarNavigation() {
        navBrowser.setOnClickListener {
            switchFragment(BrowserFragment(), navBrowser)
        }
        navDownloads.setOnClickListener {
            switchFragment(DownloadsFragment(), navDownloads)
        }
        navBookmarks.setOnClickListener {
            switchFragment(BookmarksFragment(), navBookmarks)
        }
        navGitHub.setOnClickListener {
            switchFragment(GitHubFragment(), navGitHub)
        }
        navUpdater.setOnClickListener {
            switchFragment(UpdaterFragment(), navUpdater)
        }
    }

    private fun switchFragment(fragment: Fragment, selectedButton: Button) {
        // Update selected state of sidebar buttons
        navBrowser.isSelected = false
        navDownloads.isSelected = false
        navBookmarks.isSelected = false
        navGitHub.isSelected = false
        navUpdater.isSelected = false

        selectedButton.isSelected = true

        activeFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    /**
     * Public method to allow other fragments to request opening a URL in the browser.
     */
    fun openUrlInBrowser(url: String) {
        val browserFragment = BrowserFragment()
        // We can pass the URL via a bundle argument
        val args = Bundle().apply {
            putString("TARGET_URL", url)
        }
        browserFragment.arguments = args
        switchFragment(browserFragment, navBrowser)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // If active screen is Browser, let back button go back in web history if possible
        if (keyCode == KeyEvent.KEYCODE_BACK && activeFragment is BrowserFragment) {
            val browserView = activeFragment?.view?.findViewById<android.webkit.WebView>(R.id.webView)
            if (browserView != null && browserView.canGoBack()) {
                browserView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
