package com.paperweight.launcher.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperweight.launcher.data.model.NotificationTier
import com.paperweight.launcher.databinding.FragmentNotificationDigestBinding
import com.paperweight.launcher.service.PaperNotificationService
import kotlinx.coroutines.launch

class NotificationDigestFragment : Fragment() {

    private var _binding: FragmentNotificationDigestBinding? = null
    private val binding get() = _binding!!

    private lateinit var tier1Adapter: NotificationAdapter
    private lateinit var tier2Adapter: NotificationAdapter
    private lateinit var tier3Adapter: NotificationAdapter

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
        PaperNotificationService.seedDummyData()

    }

    private fun setupTierRecyclers() {
        tier1Adapter = NotificationAdapter()
        tier2Adapter = NotificationAdapter()
        tier3Adapter = NotificationAdapter()

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
        lifecycleScope.launch {
            PaperNotificationService.notifications.collect { notifications ->
                val tier1 = notifications.filter { it.tier == NotificationTier.TIER_1 }
                val tier2 = notifications.filter { it.tier == NotificationTier.TIER_2 }
                val tier3 = notifications.filter { it.tier == NotificationTier.TIER_3 }

                tier1Adapter.submitList(tier1)
                tier2Adapter.submitList(tier2)
                tier3Adapter.submitList(tier3)

                binding.tier1Section.visibility = if (tier1.isEmpty()) View.GONE else View.VISIBLE
                binding.tier2Section.visibility = if (tier2.isEmpty()) View.GONE else View.VISIBLE
                binding.tier3Section.visibility = if (tier3.isEmpty()) View.GONE else View.VISIBLE

                binding.emptyState.visibility =
                    if (notifications.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}