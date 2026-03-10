package com.paperweight.launcher.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperweight.launcher.data.repository.AppRepository
import com.paperweight.launcher.databinding.FragmentHomeBinding
import com.paperweight.launcher.service.PaperNotificationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private lateinit var coreAppAdapter: CoreAppAdapter

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 60_000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = AppRepository(requireContext())

        setupClock()
        setupCoreApps()
        observeNotifications()
    }

    private fun setupClock() {
        updateClock()
        clockHandler.post(clockRunnable)
    }

    private fun updateClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val now = Date()
        binding.clockView.text = timeFormat.format(now)
        binding.dateView.text = dateFormat.format(now)
    }

    private fun setupCoreApps() {
        coreAppAdapter = CoreAppAdapter { app ->
            appRepository.launchApp(app.packageName)
        }

        binding.coreAppsRecycler.apply {
            layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = coreAppAdapter
        }

        lifecycleScope.launch {
            appRepository.seedDefaultsIfEmpty()
            val coreApps = appRepository.getCoreApps()
            coreAppAdapter.submitList(coreApps)
        }
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            PaperNotificationService.notifications.collect { notifications ->
                val tier2Count = notifications.count {
                    it.tier == com.paperweight.launcher.data.model.NotificationTier.TIER_2
                }
                val tier3Count = notifications.count {
                    it.tier == com.paperweight.launcher.data.model.NotificationTier.TIER_3
                }

                binding.notificationBadge.text = when {
                    tier2Count == 0 && tier3Count == 0 -> ""
                    tier2Count > 0 && tier3Count == 0 ->
                        "$tier2Count notification${if (tier2Count > 1) "s" else ""}"
                    tier2Count == 0 ->
                        "$tier3Count digest item${if (tier3Count > 1) "s" else ""}"
                    else ->
                        "$tier2Count notification${if (tier2Count > 1) "s" else ""} · $tier3Count digest"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val coreApps = appRepository.getCoreApps()
            coreAppAdapter.submitList(coreApps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockHandler.removeCallbacks(clockRunnable)
        _binding = null
    }
}