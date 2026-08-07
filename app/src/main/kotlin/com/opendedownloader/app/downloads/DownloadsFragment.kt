package com.opendedownloader.app.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.R
import com.opendedownloader.app.db.AppDatabase
import com.opendedownloader.app.db.DownloadEntity
import com.opendedownloader.app.download.DownloadEngine
import com.opendedownloader.app.download.DownloadListener
import com.opendedownloader.app.utils.PackageInstallerHelper
import java.io.File
import java.util.concurrent.Executors

class DownloadsFragment : Fragment(), DownloadListener {

    private lateinit var rvDownloads: RecyclerView
    private lateinit var tvNoDownloads: TextView
    private lateinit var adapter: DownloadsAdapter
    private val dbQueryExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_downloads, container, false)

        rvDownloads = view.findViewById(R.id.rvDownloads)
        tvNoDownloads = view.findViewById(R.id.tvNoDownloads)

        rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        adapter = DownloadsAdapter(
            downloads = emptyList(),
            onInstallClick = { item ->
                installApk(item)
            },
            onDeleteClick = { item ->
                deleteDownload(item)
            }
        )
        rvDownloads.adapter = adapter

        // Observe downloads LiveData
        val db = AppDatabase.getInstance(requireContext())
        db.downloadDao().getAllLiveData().observe(viewLifecycleOwner) { downloadList ->
            if (downloadList.isNullOrEmpty()) {
                tvNoDownloads.visibility = View.VISIBLE
                rvDownloads.visibility = View.GONE
            } else {
                tvNoDownloads.visibility = View.GONE
                rvDownloads.visibility = View.VISIBLE
                adapter.updateList(downloadList)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        DownloadEngine.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        DownloadEngine.removeListener(this)
    }

    // DownloadListener callbacks to refresh UI
    override fun onProgress(downloadId: Long, downloadedBytes: Long, totalBytes: Long) {
        // Room LiveData auto-notifies, but we can also manually notify if we want instant local updates
    }

    override fun onStatusChanged(downloadId: Long, status: String) {
        // Room LiveData auto-notifies, but we can also manually notify if we want instant local updates
    }

    private fun installApk(entity: DownloadEntity) {
        val file = File(entity.filePath)
        if (file.exists()) {
            PackageInstallerHelper.installApk(requireContext(), file)
        } else {
            Toast.makeText(requireContext(), "Error: APK file not found on disk.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteDownload(entity: DownloadEntity) {
        dbQueryExecutor.execute {
            // First stop active download if running
            if (DownloadEngine.isDownloading(entity.id)) {
                DownloadEngine.cancelDownload(requireContext(), entity.id)
            } else {
                val db = AppDatabase.getInstance(requireContext())
                db.downloadDao().delete(entity)
                val file = File(entity.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Download entry deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
