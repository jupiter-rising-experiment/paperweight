package com.paperweight.launcher.data.model

sealed class NotificationItem {
    data class Single(val notification: PaperNotification) : NotificationItem()
    data class GroupHeader(
        val groupKey: String,
        val packageName: String,
        val appLabel: String,
        val notifications: List<PaperNotification>,
        val isExpanded: Boolean = false
    ) : NotificationItem()
    data class GroupChild(
        val notification: PaperNotification,
        val groupKey: String
    ) : NotificationItem()
}
