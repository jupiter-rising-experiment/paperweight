package com.paperweight.launcher.ui.appgrid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.paperweight.launcher.data.repository.AppRepository
import com.paperweight.launcher.databinding.FragmentAppGridBinding
import kotlinx.coroutines.launch

class AppGridFragment : Fragment() {

    private var _binding: FragmentAppGridBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private lateinit var adapter: AppGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appRepository = AppRepository(requireContext())

        adapter = AppGridAdapter { app ->
            appRepository.launchApp(app.packageName)
        }

        binding.appGridRecycler.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = this@AppGridFragment.adapter
        }

        loadApps()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = appRepository.getAllApps()
            adapter.submitList(apps)
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