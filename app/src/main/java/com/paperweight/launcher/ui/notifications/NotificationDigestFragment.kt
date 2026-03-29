package com.paperweight.launcher.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperweight.launcher.data.model.NotificationItem
import com.paperweight.launcher.data.model.NotificationTier
import com.paperweight.launcher.data.model.PaperNotification
import com.paperweight.launcher.databinding.FragmentNotificationDigestBinding
import com.paperweight.launcher.service.PaperNotificationService
import kotlinx.coroutines.launch

class NotificationDigestFragment : Fragment() {

    private var _binding: FragmentNotificationDigestBinding? = null
    private val binding get() = _binding!!

    private lateinit var tier1Adapter: NotificationAdapter
    private lateinit var tier2Adapter: NotificationAdapter
    private lateinit var tier3Adapter: NotificationAdapter

    private val expandedGroups = mutableSetOf<String>()

    private var currentTier1 = emptyList<PaperNotification>()
    private var currentTier2 = emptyList<PaperNotification>()
    private var currentTier3 = emptyList<PaperNotification>()

    companion object {
        private val MESSAGING_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.android.mms"
        )
        private val CALL_PACKAGES = setOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.phone"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationDigestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTierRecyclers()
        observeNotifications()
    }

    private fun setupTierRecyclers() {
        tier1Adapter = NotificationAdapter(::onGroupToggle)
        tier2Adapter = NotificationAdapter(::onGroupToggle)
        tier3Adapter = NotificationAdapter(::onGroupToggle)

        binding.tier1Recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tier1Adapter
        }
        binding.tier2Recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tier2Adapter
        }
        binding.tier3Recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tier3Adapter
        }
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                PaperNotificationService.notifications.collect { notifications ->
                    currentTier1 = notifications.filter { it.tier == NotificationTier.TIER_1 }
                    currentTier2 = notifications.filter { it.tier == NotificationTier.TIER_2 }
                    currentTier3 = notifications.filter { it.tier == NotificationTier.TIER_3 }
                    refreshAdapters(notifications.isEmpty())
                }
            }
        }
    }

    private fun onGroupToggle(groupKey: String) {
        if (!expandedGroups.remove(groupKey)) expandedGroups.add(groupKey)
        refreshAdapters(currentTier1.isEmpty() && currentTier2.isEmpty() && currentTier3.isEmpty())
    }

    private fun refreshAdapters(isEmpty: Boolean) {
        tier1Adapter.submitList(buildDisplayList(currentTier1))
        tier2Adapter.submitList(buildDisplayList(currentTier2))
        tier3Adapter.submitList(buildDisplayList(currentTier3))

        binding.tier1Section.visibility = if (currentTier1.isEmpty()) View.GONE else View.VISIBLE
        binding.tier2Section.visibility = if (currentTier2.isEmpty()) View.GONE else View.VISIBLE
        binding.tier3Section.visibility = if (currentTier3.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun buildDisplayList(notifications: List<PaperNotification>): List<NotificationItem> {
        val grouped = LinkedHashMap<String, MutableList<PaperNotification>>()
        for (notif in notifications.sortedByDescending { it.timestamp }) {
            val key = groupKey(notif)
            grouped.getOrPut(key) { mutableListOf() }.add(notif)
        }

        val result = mutableListOf<NotificationItem>()
        for ((key, notifs) in grouped) {
            if (notifs.size == 1) {
                result.add(NotificationItem.Single(notifs.first()))
            } else {
                val header = NotificationItem.GroupHeader(
                    groupKey = key,
                    packageName = notifs.first().packageName,
                    appLabel = notifs.first().appLabel,
                    notifications = notifs,
                    isExpanded = expandedGroups.contains(key)
                )
                result.add(header)
                if (header.isExpanded) {
                    notifs.forEachIndexed { index, notif ->
                        result.add(NotificationItem.GroupChild(notif, key, isFirst = index == 0))
                }
            }
        }
        return result
    }

    private fun groupKey(notif: PaperNotification): String = when (notif.packageName) {
        in MESSAGING_PACKAGES, in CALL_PACKAGES -> "${notif.packageName}:${notif.title}"
        else -> notif.packageName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
