package com.paperweight.launcher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.databinding.ItemCoreAppBinding

class CoreAppAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, CoreAppAdapter.ViewHolder>(AppDiffCallback()) {

    inner class ViewHolder(private val binding: ItemCoreAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appLabel.text = app.label
            binding.root.setOnClickListener { onAppClick(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCoreAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(old: AppInfo, new: AppInfo) =
            old.packageName == new.packageName
        override fun areContentsTheSame(old: AppInfo, new: AppInfo) =
            old.label == new.label && old.isCoreApp == new.isCoreApp
    }
}