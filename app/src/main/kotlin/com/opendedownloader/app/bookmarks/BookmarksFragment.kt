package com.opendedownloader.app.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opendedownloader.app.MainActivity
import com.opendedownloader.app.R
import com.opendedownloader.app.db.AppDatabase
import java.util.concurrent.Executors

class BookmarksFragment : Fragment() {

    private lateinit var rvBookmarks: RecyclerView
    private lateinit var tvNoBookmarks: TextView
    private lateinit var adapter: BookmarksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)

        rvBookmarks = view.findViewById(R.id.rvBookmarks)
        tvNoBookmarks = view.findViewById(R.id.tvNoBookmarks)

        rvBookmarks.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookmarksAdapter(
            bookmarks = emptyList(),
            onItemClick = { bookmark ->
                // Open inside the browser
                (activity as? MainActivity)?.openUrlInBrowser(bookmark.url)
            },
            onDeleteClick = { bookmark ->
                deleteBookmark(bookmark)
            }
        )
        rvBookmarks.adapter = adapter

        loadBookmarks()

        return view
    }

    private fun loadBookmarks() {
        val db = AppDatabase.getInstance(requireContext())
        db.bookmarkDao().getAllLiveData().observe(viewLifecycleOwner) { bookmarkList ->
            if (bookmarkList.isNullOrEmpty()) {
                tvNoBookmarks.visibility = View.VISIBLE
                rvBookmarks.visibility = View.GONE
            } else {
                tvNoBookmarks.visibility = View.GONE
                rvBookmarks.visibility = View.VISIBLE
                adapter.updateList(bookmarkList)
            }
        }
    }

    private fun deleteBookmark(bookmark: com.opendedownloader.app.db.BookmarkEntity) {
        val db = AppDatabase.getInstance(requireContext())
        Executors.newSingleThreadExecutor().execute {
            db.bookmarkDao().delete(bookmark)
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Bookmark deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
