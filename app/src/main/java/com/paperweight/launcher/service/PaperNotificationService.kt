package com.paperweight.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.paperweight.launcher.data.model.NotificationTier
import com.paperweight.launcher.data.model.PaperNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PaperNotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<PaperNotification>>(dummyNotifications())
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

private fun dummyNotifications(): List<PaperNotification> {
    val now = System.currentTimeMillis()
    val min = 60_000L
    return listOf(
        // Tier 1 — calls (should group by package+title)
        PaperNotification(1001, "com.google.android.dialer", "Phone", "Missed call", "Mom", NotificationTier.TIER_1, now - 2 * min),
        PaperNotification(1002, "com.google.android.dialer", "Phone", "Missed call", "Dad", NotificationTier.TIER_1, now - 5 * min),
        PaperNotification(1003, "com.google.android.dialer", "Phone", "Missed call", "Work", NotificationTier.TIER_1, now - 8 * min),

        // Tier 2 — messages (group by package+title = per-sender thread)
        PaperNotification(2001, "com.google.android.apps.messaging", "Messages", "Alice", "Are you free tonight?", NotificationTier.TIER_2, now - 1 * min),
        PaperNotification(2002, "com.google.android.apps.messaging", "Messages", "Alice", "I was thinking dinner", NotificationTier.TIER_2, now - 3 * min),
        PaperNotification(2003, "com.google.android.apps.messaging", "Messages", "Bob", "Can you send me the doc?", NotificationTier.TIER_2, now - 10 * min),

        // Tier 2 — email (group by package)
        PaperNotification(2004, "com.google.android.gm", "Gmail", "New invoice from Stripe", "Your payout of \$240.00 is on its way", NotificationTier.TIER_2, now - 15 * min),
        PaperNotification(2005, "com.google.android.gm", "Gmail", "GitHub: new PR review", "Someone reviewed your pull request", NotificationTier.TIER_2, now - 20 * min),
        PaperNotification(2006, "com.google.android.gm", "Gmail", "Meeting reminder", "Standup in 10 minutes", NotificationTier.TIER_2, now - 25 * min),

        // Tier 3 — social (group by package)
        PaperNotification(3001, "com.instagram.android", "Instagram", "New follower", "john_doe started following you", NotificationTier.TIER_3, now - 30 * min),
        PaperNotification(3002, "com.instagram.android", "Instagram", "New follower", "jane_smith started following you", NotificationTier.TIER_3, now - 35 * min),
        PaperNotification(3003, "com.twitter.android", "X (Twitter)", "New mention", "@someuser mentioned you in a reply", NotificationTier.TIER_3, now - 40 * min),
        PaperNotification(3004, "com.reddit.frontpage", "Reddit", "Hot post in r/android", "Check out this new launcher concept", NotificationTier.TIER_3, now - 45 * min)
    )
}