package com.paperweight.launcher.ui.applibrary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperweight.launcher.data.model.AppInfo
import com.paperweight.launcher.data.repository.AppRepository
import com.paperweight.launcher.databinding.FragmentAppLibraryBinding
import kotlinx.coroutines.launch

class AppLibraryFragment : Fragment() {

    private var _binding: FragmentAppLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private lateinit var adapter: AppLibraryAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = AppRepository(requireContext())

        adapter = AppLibraryAdapter { app ->
            appRepository.launchApp(app.packageName)
        }

        binding.appListRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AppLibraryFragment.adapter
        }

        setupSearch()
        loadApps()
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
    }

    private fun loadApps() {
        lifecycleScope.launch {
            allApps = appRepository.getAllApps()
            adapter.submitList(allApps)
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