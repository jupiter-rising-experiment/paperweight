package com.paperweight.launcher.ui.appgrid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.databinding.ItemCoreAppBinding
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

class AppGridAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppGridAdapter.ViewHolder>(AppDiffCallback()) {

    inner class ViewHolder(private val binding: ItemCoreAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            binding.appIcon.colorFilter = ColorMatrixColorFilter(matrix)
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
            old.label == new.label
    }
}