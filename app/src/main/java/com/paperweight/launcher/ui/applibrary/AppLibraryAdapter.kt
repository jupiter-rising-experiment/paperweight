package com.paperweight.launcher.ui.applibrary

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.databinding.ItemAppListRowBinding

class AppLibraryAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppLibraryAdapter.ViewHolder>(AppDiffCallback()) {

    inner class ViewHolder(private val binding: ItemAppListRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            binding.appLabel.text = app.label
            binding.root.setOnClickListener { onAppClick(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppListRowBinding.inflate(
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
            old.label == new.label
    }
}