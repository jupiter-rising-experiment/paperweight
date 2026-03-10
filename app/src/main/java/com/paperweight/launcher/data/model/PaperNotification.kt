package com.paperweight.launcher.data.model

enum class NotificationTier {
    TIER_1,
    TIER_2,
    TIER_3
}

data class PaperNotification(
    val id: Int,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val tier: NotificationTier,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)