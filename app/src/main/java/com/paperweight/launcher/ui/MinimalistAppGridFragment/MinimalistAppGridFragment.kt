package com.paperweight.launcher.ui.minimalistgrid

import android.content.pm.ApplicationInfo
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
        val apps = appRepository.getAllApps()
        val pm = requireContext().packageManager
        val categorised = mutableMapOf<String, MutableList<AppInfo>>()

        for (app in apps) {
            val cat = resolveCategory(pm, app.packageName) ?: continue
            categorised.getOrPut(cat) { mutableListOf() }.add(app)
        }

        val result = mutableListOf<MinimalistItem>()
        for ((category, appsInCat) in categorised.toSortedMap()) {
            result.add(MinimalistItem.Header(category))
            appsInCat.sortedBy { it.label.lowercase() }.forEach { result.add(MinimalistItem.App(it)) }
        }
        if (result.isNotEmpty()) result.add(MinimalistItem.More)
        result
    }

    private fun resolveCategory(pm: PackageManager, packageName: String): String? {
        val category = try {
            pm.getApplicationInfo(packageName, 0).category
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return when (category) {
            ApplicationInfo.CATEGORY_SOCIAL        -> "Communication"
            ApplicationInfo.CATEGORY_MAPS          -> "Navigation"
            ApplicationInfo.CATEGORY_AUDIO         -> "Audio"
            ApplicationInfo.CATEGORY_VIDEO         -> "Video"
            ApplicationInfo.CATEGORY_IMAGE         -> "Photos"
            ApplicationInfo.CATEGORY_GAME          -> "Games"
            ApplicationInfo.CATEGORY_NEWS          -> "News"
            ApplicationInfo.CATEGORY_PRODUCTIVITY  -> "Productivity"
            else                                   -> null
        }
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
