package com.zbrowser.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zbrowser.app.data.HistoryEntry
import com.zbrowser.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onHistoryClick: (HistoryEntry) -> Unit,
    private val onHistoryLongClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(entry: HistoryEntry) {
            binding.historyTitle.text = entry.title.ifBlank { entry.url }
            binding.historyUrl.text = entry.url
            binding.historyTime.text = timeFormat.format(Date(entry.visitedAt))

            binding.root.setOnClickListener {
                onHistoryClick(entry)
            }

            binding.root.setOnLongClickListener {
                onHistoryLongClick(entry)
                true
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}
