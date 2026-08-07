package com.opendedownloader.app.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.R
import com.opendedownloader.app.db.DownloadEntity
import com.opendedownloader.app.download.DownloadEngine
import java.io.File
import java.util.Locale

class DownloadsAdapter(
    private var downloads: List<DownloadEntity>,
    private val onInstallClick: (DownloadEntity) -> Unit,
    private val onDeleteClick: (DownloadEntity) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
        val pbDownload: ProgressBar = view.findViewById(R.id.pbDownload)
        val tvSha256: TextView = view.findViewById(R.id.tvSha256)
        val btnPauseResume: Button = view.findViewById(R.id.btnPauseResume)
        val btnInstall: Button = view.findViewById(R.id.btnInstall)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = downloads[position]
        holder.tvFileName.text = item.fileName

        val file = File(item.filePath)
        val fileExists = file.exists()

        // Formatting downloaded size and total size
        val downloadedStr = formatSize(item.downloadedBytes)
        val totalStr = formatSize(item.totalBytes)
        val percent = if (item.totalBytes > 0) {
            (item.downloadedBytes * 100 / item.totalBytes).toInt()
        } else {
            0
        }

        // Configure UI depending on status
        when (item.status) {
            "COMPLETED" -> {
                holder.pbDownload.visibility = View.GONE
                holder.btnPauseResume.visibility = View.GONE

                if (fileExists) {
                    holder.btnInstall.visibility = View.VISIBLE
                    holder.tvDetails.text = "Status: COMPLETED | Size: $totalStr"
                } else {
                    holder.btnInstall.visibility = View.GONE
                    holder.tvDetails.text = "Status: FILE MISSING / DELETED"
                }

                if (!item.sha256.isNullOrEmpty()) {
                    holder.tvSha256.text = "SHA-256: ${item.sha256}"
                    holder.tvSha256.visibility = View.VISIBLE
                } else {
                    holder.tvSha256.visibility = View.GONE
                }
            }
            "DOWNLOADING" -> {
                holder.pbDownload.visibility = View.VISIBLE
                holder.pbDownload.progress = percent
                holder.btnPauseResume.visibility = View.VISIBLE
                holder.btnPauseResume.text = "Pause"
                holder.btnInstall.visibility = View.GONE
                holder.tvSha256.visibility = View.GONE
                holder.tvDetails.text = "Status: DOWNLOADING | $downloadedStr / $totalStr ($percent%)"
            }
            "PAUSED" -> {
                holder.pbDownload.visibility = View.VISIBLE
                holder.pbDownload.progress = percent
                holder.btnPauseResume.visibility = View.VISIBLE
                holder.btnPauseResume.text = "Resume"
                holder.btnInstall.visibility = View.GONE
                holder.tvSha256.visibility = View.GONE
                holder.tvDetails.text = "Status: PAUSED | $downloadedStr / $totalStr ($percent%)"
            }
            "PENDING" -> {
                holder.pbDownload.visibility = View.VISIBLE
                holder.pbDownload.progress = 0
                holder.btnPauseResume.visibility = View.VISIBLE
                holder.btnPauseResume.text = "Pause"
                holder.btnInstall.visibility = View.GONE
                holder.tvSha256.visibility = View.GONE
                holder.tvDetails.text = "Status: PENDING..."
            }
            "FAILED" -> {
                holder.pbDownload.visibility = View.GONE
                holder.btnPauseResume.visibility = View.VISIBLE
                holder.btnPauseResume.text = "Retry"
                holder.btnInstall.visibility = View.GONE
                holder.tvSha256.visibility = View.GONE
                val errorMsg = item.errorMessage ?: "Unknown error"
                holder.tvDetails.text = "Status: FAILED | $errorMsg"
            }
            else -> {
                holder.pbDownload.visibility = View.GONE
                holder.btnPauseResume.visibility = View.GONE
                holder.btnInstall.visibility = View.GONE
                holder.tvSha256.visibility = View.GONE
                holder.tvDetails.text = "Status: ${item.status}"
            }
        }

        // Button Click listeners
        holder.btnPauseResume.setOnClickListener {
            val context = holder.itemView.context
            if (item.status == "DOWNLOADING") {
                DownloadEngine.pauseDownload(context, item.id)
            } else {
                DownloadEngine.resumeDownload(context, item.id)
            }
        }

        holder.btnInstall.setOnClickListener {
            onInstallClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = downloads.size

    fun updateList(newList: List<DownloadEntity>) {
        downloads = newList
        notifyDataSetChanged()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
