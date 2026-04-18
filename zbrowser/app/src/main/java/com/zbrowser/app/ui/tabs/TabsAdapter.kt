package com.zbrowser.app.ui.tabs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zbrowser.app.databinding.ItemTabBinding
import com.zbrowser.app.web.WebViewManager

class TabsAdapter(
    private val onTabClick: (String) -> Unit,
    private val onTabClose: (String) -> Unit
) : ListAdapter<WebViewManager.TabInfo, TabsAdapter.TabViewHolder>(TabDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TabViewHolder(
        private val binding: ItemTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tabInfo: WebViewManager.TabInfo) {
            binding.tabTitle.text = tabInfo.title.ifBlank { tabInfo.url }
            binding.tabUrl.text = tabInfo.url

            tabInfo.favicon?.let {
                binding.tabFavicon.setImageBitmap(it)
            }

            binding.root.setOnClickListener {
                onTabClick(tabInfo.id)
            }

            binding.btnCloseTab.setOnClickListener {
                onTabClose(tabInfo.id)
            }
        }
    }

    class TabDiffCallback : DiffUtil.ItemCallback<WebViewManager.TabInfo>() {
        override fun areItemsTheSame(oldItem: WebViewManager.TabInfo, newItem: WebViewManager.TabInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WebViewManager.TabInfo, newItem: WebViewManager.TabInfo): Boolean {
            return oldItem == newItem
        }
    }
}
