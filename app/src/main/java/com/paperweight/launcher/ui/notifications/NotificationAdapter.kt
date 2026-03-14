package com.paperweight.launcher.ui.notifications

import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.data.model.PaperNotification
import com.paperweight.launcher.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat

class NotificationAdapter : ListAdapter<PaperNotification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: PaperNotification) {
            binding.notifApp.text = notification.appLabel
            binding.notifTitle.text = notification.title
            binding.notifText.text = notification.text

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.notifTime.text = timeFormat.format(Date(notification.timestamp))

            val grayscale = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
            try {
                val icon = binding.root.context.packageManager
                    .getApplicationIcon(notification.packageName)
                binding.notifIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                val fallback = if (notification.packageName.contains("dialer") ||
                    notification.packageName.contains("phone")) {
                    ContextCompat.getDrawable(binding.root.context, android.R.drawable.ic_menu_call)
                } else null
                binding.notifIcon.setImageDrawable(fallback)
            }
            binding.notifIcon.colorFilter = grayscale
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<PaperNotification>() {
        override fun areItemsTheSame(old: PaperNotification, new: PaperNotification) =
            old.id == new.id && old.packageName == new.packageName
        override fun areContentsTheSame(old: PaperNotification, new: PaperNotification) =
            old == new
    }
}
