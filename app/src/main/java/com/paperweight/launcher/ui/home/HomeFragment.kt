package com.paperweight.launcher.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            PaperNotificationService.notifications.collect { notifications ->
                val tier1Count = notifications.count {
                    it.tier == com.paperweight.launcher.data.model.NotificationTier.TIER_1
                }
                val tier2Count = notifications.count {
                    it.tier == com.paperweight.launcher.data.model.NotificationTier.TIER_2
                }

                val parts = mutableListOf<String>()
                if (tier1Count > 0) parts.add("$tier1Count important")
                if (tier2Count > 0) parts.add("$tier2Count update${if (tier2Count > 1) "s" else ""}")

                binding.notificationBadge.text = parts.joinToString(" · ")
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