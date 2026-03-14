package com.paperweight.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.paperweight.launcher.data.model.NotificationTier
import com.paperweight.launcher.data.model.PaperNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PaperNotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<PaperNotification>>(emptyList())
        val notifications: StateFlow<List<PaperNotification>> = _notifications

        val tierOverrides = mutableMapOf<String, NotificationTier>()

        private val defaultTier3Packages = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.linkedin.android"
        )

        private val defaultTier1Packages = setOf(
            "com.android.dialer",
            "com.android.phone",
            "com.google.android.dialer"
        )
        fun seedDummyData() {
            _notifications.value = listOf(
                PaperNotification(
                    id = 1,
                    packageName = "com.android.dialer",
                    appLabel = "Phone",
                    title = "Missed call",
                    text = "Mom",
                    tier = NotificationTier.TIER_1,
                    timestamp = System.currentTimeMillis() - 5 * 60 * 1000
                ),
                PaperNotification(
                    id = 2,
                    packageName = "com.google.android.apps.messaging",
                    appLabel = "Messages",
                    title = "Sarah",
                    text = "Are you coming tonight?",
                    tier = NotificationTier.TIER_2,
                    timestamp = System.currentTimeMillis() - 20 * 60 * 1000
                ),
                PaperNotification(
                    id = 3,
                    packageName = "com.google.android.gm",
                    appLabel = "Gmail",
                    title = "Your receipt from Apple",
                    text = "Amount charged: \$0.99",
                    tier = NotificationTier.TIER_2,
                    timestamp = System.currentTimeMillis() - 60 * 60 * 1000
                ),
                PaperNotification(
                    id = 4,
                    packageName = "com.instagram.android",
                    appLabel = "Instagram",
                    title = "New followers",
                    text = "john_doe and 3 others started following you",
                    tier = NotificationTier.TIER_3,
                    timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000
                )
            )
        }

    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return
        if (pkg == packageName) return

        val tier = resolveTier(pkg)

        val notification = PaperNotification(
            id = sbn.id,
            packageName = pkg,
            appLabel = getAppLabel(pkg),
            title = title,
            text = text,
            tier = tier,
            timestamp = sbn.postTime
        )

        val current = _notifications.value.toMutableList()
        current.removeAll { it.packageName == pkg && it.id == sbn.id }
        current.add(0, notification)

        _notifications.value = current
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val current = _notifications.value.toMutableList()
        current.removeAll { it.packageName == sbn.packageName && it.id == sbn.id }
        _notifications.value = current
    }

    private fun resolveTier(packageName: String): NotificationTier {
        tierOverrides[packageName]?.let { return it }

        return when (packageName) {
            in defaultTier1Packages -> NotificationTier.TIER_1
            in defaultTier3Packages -> NotificationTier.TIER_3
            else -> NotificationTier.TIER_2
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}