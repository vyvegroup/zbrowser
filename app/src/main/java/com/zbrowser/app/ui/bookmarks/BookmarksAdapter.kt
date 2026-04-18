package com.zbrowser.app.ui.bookmarks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zbrowser.app.data.Bookmark
import com.zbrowser.app.databinding.ItemBookmarkBinding

class BookmarksAdapter(
    private val onBookmarkClick: (Bookmark) -> Unit,
    private val onBookmarkLongClick: (Bookmark) -> Unit
) : ListAdapter<Bookmark, BookmarksAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            binding.bookmarkTitle.text = bookmark.title
            binding.bookmarkUrl.text = bookmark.url

            binding.root.setOnClickListener {
                onBookmarkClick(bookmark)
            }

            binding.root.setOnLongClickListener {
                onBookmarkLongClick(bookmark)
                true
            }
        }
    }

    class BookmarkDiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem == newItem
        }
    }
}
