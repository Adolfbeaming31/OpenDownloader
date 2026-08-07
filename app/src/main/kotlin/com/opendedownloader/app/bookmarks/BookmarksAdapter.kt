package com.opendedownloader.app.bookmarks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.R
import com.opendedownloader.app.db.BookmarkEntity

class BookmarksAdapter(
    private var bookmarks: List<BookmarkEntity>,
    private val onItemClick: (BookmarkEntity) -> Unit,
    private val onDeleteClick: (BookmarkEntity) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.tvTitle.text = bookmark.title
        holder.tvUrl.text = bookmark.url

        holder.itemView.setOnClickListener {
            onItemClick(bookmark)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(bookmark)
        }
    }

    override fun getItemCount() = bookmarks.size

    fun updateList(newList: List<BookmarkEntity>) {
        bookmarks = newList
        notifyDataSetChanged()
    }
}
