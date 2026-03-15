package com.paperweight.launcher.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.paperweight.launcher.databinding.ActivityMainBinding
import com.paperweight.launcher.ui.appgrid.AppGridFragment
import com.paperweight.launcher.ui.applibrary.AppLibraryFragment
import com.paperweight.launcher.ui.home.HomeFragment
import com.paperweight.launcher.ui.notifications.NotificationDigestFragment
import com.paperweight.launcher.ui.minimalistgrid.MinimalistAppGridFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    fun navigateTo(position: Int) {
        binding.viewPager.setCurrentItem(position, true)
    }

    companion object {
        const val HOME_PAGE_INDEX = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem != HOME_PAGE_INDEX) {
                    binding.viewPager.setCurrentItem(HOME_PAGE_INDEX, true)
                }
            }
        })
        checkNotificationPermission()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = LauncherPagerAdapter(this)
        binding.viewPager.setCurrentItem(HOME_PAGE_INDEX, false)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
            }
        })

        binding.viewPager.offscreenPageLimit = 2
    }

    private fun updatePageIndicator(position: Int) {
        val labels = listOf("Notifications", "Home","Focus", "Apps", "Library")

        binding.pageLabel.text = labels.getOrNull(position) ?: ""
    }

    private fun checkNotificationPermission() {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val isEnabled = enabledListeners?.contains(packageName) == true

        if (!isEnabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    inner class LauncherPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount() = 5

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> NotificationDigestFragment()
            1 -> HomeFragment()
            2 -> MinimalistAppGridFragment()
            3 -> AppGridFragment()
            4 -> AppLibraryFragment()
            else -> HomeFragment()
        }
    }
}
