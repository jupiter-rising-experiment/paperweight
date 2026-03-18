package com.paperweight.launcher.ui.notifications

import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperweight.launcher.R
import com.paperweight.launcher.data.model.NotificationItem
import com.paperweight.launcher.data.model.PaperNotification
import com.paperweight.launcher.databinding.ItemNotificationBinding
import com.paperweight.launcher.databinding.ItemNotificationGroupBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onGroupToggle: (String) -> Unit
) : ListAdapter<NotificationItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_SINGLE = 0
        private const val VIEW_GROUP = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is NotificationItem.Single, is NotificationItem.GroupChild -> VIEW_SINGLE
        is NotificationItem.GroupHeader -> VIEW_GROUP
    }

    inner class SingleViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class GroupViewHolder(val binding: ItemNotificationGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_GROUP -> {
                val binding = ItemNotificationGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                val holder = GroupViewHolder(binding)
                binding.root.setOnClickListener {
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        (getItem(pos) as? NotificationItem.GroupHeader)?.let { onGroupToggle(it.groupKey) }
                    }
                }
                holder
            }
            else -> SingleViewHolder(
                ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationItem.Single -> bindSingle(holder as SingleViewHolder, item.notification)
            is NotificationItem.GroupChild -> bindSingle(holder as SingleViewHolder, item.notification)
            is NotificationItem.GroupHeader -> bindGroup(holder as GroupViewHolder, item)
        }
    }

    private fun bindSingle(holder: SingleViewHolder, notification: PaperNotification) {
        val b = holder.binding
        b.notifApp.text = notification.appLabel
        b.notifTitle.text = notification.title
        b.notifText.text = notification.text

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        b.notifTime.text = timeFormat.format(Date(notification.timestamp))

        val grayscale = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
        val customIcon = getCustomIcon(notification.packageName)
        if (customIcon != null) {
            b.notifIcon.setImageResource(customIcon)
        } else {
            try {
                b.notifIcon.setImageDrawable(
                    b.root.context.packageManager.getApplicationIcon(notification.packageName)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                b.notifIcon.setImageDrawable(null)
            }
        }
        b.notifIcon.colorFilter = grayscale
    }

    private fun bindGroup(holder: GroupViewHolder, group: NotificationItem.GroupHeader) {
        val b = holder.binding
        val latest = group.notifications.first()

        b.groupApp.text = group.appLabel
        b.groupTitle.text = latest.title
        b.groupSummary.text = if (group.isExpanded) {
            "tap to collapse"
        } else {
            "(${group.notifications.size} more)"
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        b.groupTime.text = timeFormat.format(Date(latest.timestamp))

        val grayscale = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
        val customIcon = getCustomIcon(group.packageName)
        if (customIcon != null) {
            b.groupIcon.setImageResource(customIcon)
        } else {
            try {
                b.groupIcon.setImageDrawable(
                    b.root.context.packageManager.getApplicationIcon(group.packageName)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                b.groupIcon.setImageDrawable(null)
            }
        }
        b.groupIcon.colorFilter = grayscale
    }

    private fun getCustomIcon(packageName: String): Int? = when (packageName) {
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone" -> R.drawable.ic_custom_phone

        "com.google.android.apps.messaging",
        "com.android.mms" -> R.drawable.ic_custom_messages

        "com.google.android.gm",
        "com.android.email" -> R.drawable.ic_custom_gmail

        else -> null
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(old: NotificationItem, new: NotificationItem): Boolean {
            return when {
                old is NotificationItem.Single && new is NotificationItem.Single ->
                    old.notification.packageName == new.notification.packageName &&
                            old.notification.id == new.notification.id
                old is NotificationItem.GroupHeader && new is NotificationItem.GroupHeader ->
                    old.groupKey == new.groupKey
                old is NotificationItem.GroupChild && new is NotificationItem.GroupChild ->
                    old.groupKey == new.groupKey &&
                            old.notification.packageName == new.notification.packageName &&
                            old.notification.id == new.notification.id
                else -> false
            }
        }

        override fun areContentsTheSame(old: NotificationItem, new: NotificationItem) = old == new
    }
}
