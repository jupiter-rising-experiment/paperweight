package com.paperweight.launcher.ui.minimalistgrid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.databinding.ItemMinimalistAppBinding
import com.paperweight.launcher.databinding.ItemMinimalistHeaderBinding
import com.paperweight.launcher.databinding.ItemMinimalistMoreBinding

sealed class MinimalistItem {
    data class Header(val categoryName: String) : MinimalistItem()
    data class App(val appInfo: AppInfo) : MinimalistItem()
    object More : MinimalistItem()
}

class MinimalistAppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onMoreClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_APP = 1
        const val VIEW_TYPE_MORE = 2
    }

    private var items: List<MinimalistItem> = emptyList()

    fun submitItems(newItems: List<MinimalistItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is MinimalistItem.Header -> VIEW_TYPE_HEADER
        is MinimalistItem.App    -> VIEW_TYPE_APP
        is MinimalistItem.More   -> VIEW_TYPE_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderHolder(ItemMinimalistHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_APP    -> AppHolder(ItemMinimalistAppBinding.inflate(inflater, parent, false))
            else             -> MoreHolder(ItemMinimalistMoreBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MinimalistItem.Header -> (holder as HeaderHolder).bind(item)
            is MinimalistItem.App    -> (holder as AppHolder).bind(item)
            is MinimalistItem.More   -> (holder as MoreHolder).bind()
        }
    }

    inner class HeaderHolder(private val binding: ItemMinimalistHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MinimalistItem.Header) {
            binding.categoryLabel.text = item.categoryName
        }
    }

    inner class AppHolder(private val binding: ItemMinimalistAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MinimalistItem.App) {
            binding.appName.text = item.appInfo.label
            binding.appName.setOnClickListener { onAppClick(item.appInfo) }
        }
    }

    inner class MoreHolder(private val binding: ItemMinimalistMoreBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.moreLabel.setOnClickListener { onMoreClick() }
        }
    }
}
