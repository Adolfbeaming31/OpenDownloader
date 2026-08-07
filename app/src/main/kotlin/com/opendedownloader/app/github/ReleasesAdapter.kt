package com.opendedownloader.app.github

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.R
import java.util.Locale

data class GitHubAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class GitHubRelease(
    val tagName: String,
    val title: String,
    val description: String,
    val date: String,
    val assets: List<GitHubAsset>
)

class ReleasesAdapter(
    private var releases: List<GitHubRelease>,
    private val onAssetClick: (GitHubAsset) -> Unit
) : RecyclerView.Adapter<ReleasesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvReleaseTag: TextView = view.findViewById(R.id.tvReleaseTag)
        val tvReleaseDate: TextView = view.findViewById(R.id.tvReleaseDate)
        val tvReleaseTitle: TextView = view.findViewById(R.id.tvReleaseTitle)
        val tvReleaseDesc: TextView = view.findViewById(R.id.tvReleaseDesc)
        val containerAssets: LinearLayout = view.findViewById(R.id.containerAssets)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_github_release, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val release = releases[position]
        holder.tvReleaseTag.text = release.tagName
        holder.tvReleaseDate.text = release.date.take(10) // Format date yyyy-MM-dd
        holder.tvReleaseTitle.text = if (release.title.isNullOrEmpty()) "Release ${release.tagName}" else release.title
        holder.tvReleaseDesc.text = release.description

        // Populate assets
        holder.containerAssets.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }

        if (apkAssets.isEmpty()) {
            val emptyTv = TextView(holder.itemView.context).apply {
                text = "No APK assets found in this release."
                setTextColor(android.graphics.Color.parseColor("#88FFFFFF"))
                textSize = 12f
                setPadding(0, 4, 0, 4)
            }
            holder.containerAssets.addView(emptyTv)
        } else {
            for (asset in apkAssets) {
                val assetButton = inflater.inflate(R.layout.item_github_asset, holder.containerAssets, false) as Button
                val sizeStr = formatSize(asset.size)
                assetButton.text = "📥 ${asset.name} ($sizeStr)"

                assetButton.setOnClickListener {
                    onAssetClick(asset)
                }

                holder.containerAssets.addView(assetButton)
            }
        }
    }

    override fun getItemCount() = releases.size

    fun updateList(newList: List<GitHubRelease>) {
        releases = newList
        notifyDataSetChanged()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
