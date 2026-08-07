package com.opendedownloader.app.github

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.R
import com.opendedownloader.app.download.DownloadEngine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class GitHubFragment : Fragment() {

    private lateinit var etRepoPath: EditText
    private lateinit var btnFetchReleases: Button
    private lateinit var tvGitStatus: TextView
    private lateinit var rvReleases: RecyclerView
    private lateinit var adapter: ReleasesAdapter

    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_github, container, false)

        etRepoPath = view.findViewById(R.id.etRepoPath)
        btnFetchReleases = view.findViewById(R.id.btnFetchReleases)
        tvGitStatus = view.findViewById(R.id.tvGitStatus)
        rvReleases = view.findViewById(R.id.rvReleases)

        rvReleases.layoutManager = LinearLayoutManager(requireContext())
        adapter = ReleasesAdapter(emptyList()) { asset ->
            promptAssetDownload(asset)
        }
        rvReleases.adapter = adapter

        // Set default path to search
        etRepoPath.setText("Kodi-Game/game.libretro")

        btnFetchReleases.setOnClickListener {
            fetchReleases()
        }

        etRepoPath.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                fetchReleases()
                true
            } else {
                false
            }
        }

        return view
    }

    private fun fetchReleases() {
        val path = etRepoPath.text.toString().trim()
        if (path.isEmpty() || !path.contains("/")) {
            tvGitStatus.text = "Error: Please enter a valid 'owner/repo' path."
            return
        }

        tvGitStatus.text = "Fetching releases for $path..."
        tvGitStatus.visibility = View.VISIBLE
        rvReleases.visibility = View.GONE

        val url = "https://api.github.com/repos/$path/releases"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OpenDownloader-App")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GitHub", "Failed to fetch releases: ${e.message}", e)
                activity?.runOnUiThread {
                    tvGitStatus.text = "Failed to fetch releases: Network Error."
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    if (!response.isSuccessful || bodyStr.isNullOrEmpty()) {
                        throw IOException("HTTP error ${response.code}")
                    }

                    val releasesArray = JSONArray(bodyStr)
                    val parsedReleases = ArrayList<GitHubRelease>()

                    for (i in 0 until releasesArray.length()) {
                        val relObj = releasesArray.getJSONObject(i)
                        val tagName = relObj.optString("tag_name", "")
                        val title = relObj.optString("name", "")
                        val description = relObj.optString("body", "")
                        val date = relObj.optString("published_at", "")

                        val assetsArray = relObj.optJSONArray("assets")
                        val assetsList = ArrayList<GitHubAsset>()
                        if (assetsArray != null) {
                            for (j in 0 until assetsArray.length()) {
                                val assetObj = assetsArray.getJSONObject(j)
                                val name = assetObj.optString("name", "")
                                val downloadUrl = assetObj.optString("browser_download_url", "")
                                val size = assetObj.optLong("size", 0L)
                                assetsList.add(GitHubAsset(name, downloadUrl, size))
                            }
                        }

                        parsedReleases.add(GitHubRelease(tagName, title, description, date, assetsList))
                    }

                    activity?.runOnUiThread {
                        if (parsedReleases.isEmpty()) {
                            tvGitStatus.text = "No releases found for this repository."
                        } else {
                            tvGitStatus.visibility = View.GONE
                            rvReleases.visibility = View.VISIBLE
                            adapter.updateList(parsedReleases)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("GitHub", "Failed to parse releases json: ${e.message}", e)
                    activity?.runOnUiThread {
                        tvGitStatus.text = "Failed to load releases. Ensure repo is public and path is correct."
                    }
                } finally {
                    response.close()
                }
            }
        })
    }

    private fun promptAssetDownload(asset: GitHubAsset) {
        AlertDialog.Builder(requireContext())
            .setTitle("Download APK Asset")
            .setMessage("Do you want to download this APK?\n\nFile Name: ${asset.name}\n\nSize: ${formatSize(asset.size)}")
            .setPositiveButton("Download") { _, _ ->
                val downloadsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: requireContext().filesDir
                val downloadId = DownloadEngine.startDownload(requireContext(), asset.downloadUrl, asset.name, downloadsDir)
                Toast.makeText(requireContext(), "Download started! ID: $downloadId", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
