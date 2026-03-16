package com.paperweight.launcher.ui.minimalistgrid

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.data.repository.AppRepository
import com.paperweight.launcher.databinding.FragmentMinimalistAppGridBinding
import com.paperweight.launcher.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FocusCategory(val name: String, val packages: List<String>)

private val FOCUS_CATEGORIES = listOf(
    FocusCategory("Communication", listOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.whatsapp",
        "org.thoughtcrime.securesms",
        "com.facebook.orca",
    )),
    FocusCategory("Entertainment", listOf(
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.google.android.apps.photos",
        "com.spotify.music",
        "com.netflix.mediaclient",
    )),
    FocusCategory("Tools", listOf(
        "com.google.android.apps.maps",
        "com.android.chrome",
        "org.chromium.chrome",
        "com.google.android.calculator",
        "com.google.android.deskclock",
    )),
    FocusCategory("Work", listOf(
        "com.google.android.calendar",
        "com.google.android.apps.docs",
        "com.google.android.gm",
        "com.microsoft.teams",
        "com.slack",
        "com.notion.id",
    ))
)

class MinimalistAppGridFragment : Fragment() {

    private var _binding: FragmentMinimalistAppGridBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private lateinit var adapter: MinimalistAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinimalistAppGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = AppRepository(requireContext())

        adapter = MinimalistAppAdapter(
            onAppClick = { app -> appRepository.launchApp(app.packageName) },
            onMoreClick = { (requireActivity() as? MainActivity)?.navigateTo(4) }
        )

        val gridManager = GridLayoutManager(context, 2)
        gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = when (adapter.getItemViewType(position)) {
                MinimalistAppAdapter.VIEW_TYPE_APP -> 1
                else                              -> 2
            }
        }

        binding.minimalistRecycler.layoutManager = gridManager
        binding.minimalistRecycler.adapter = adapter

        loadApps()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            adapter.submitItems(buildItemList())
        }
    }

    private suspend fun buildItemList(): List<MinimalistItem> = withContext(Dispatchers.IO) {
        val pm = requireContext().packageManager
        val result = mutableListOf<MinimalistItem>()
        var isFirst = true

        for (category in FOCUS_CATEGORIES) {
            val installedApps = category.packages.mapNotNull { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(pkg),
                        isCoreApp = false
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }

            if (installedApps.isEmpty()) continue

            if (!isFirst) result.add(MinimalistItem.Separator)
            result.add(MinimalistItem.Header(category.name))
            installedApps.forEach { result.add(MinimalistItem.App(it)) }
            isFirst = false
        }

        if (result.isNotEmpty()) result.add(MinimalistItem.MoreApps)
        result
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
