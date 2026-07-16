package com.example.safehome.ui.device

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safehome.databinding.ActivityDeviceHistoryBinding
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.launch

class DeviceHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceHistoryBinding
    private lateinit var viewModel: DeviceHistoryViewModel
    private lateinit var historyAdapter: DeviceHistoryAdapter
    private var deviceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = intent.getIntExtra("deviceId", -1)
        if (deviceId == -1) {
            finish()
            return
        }

        viewModel = createViewModel()
        setupRecyclerView()
        setupViews()
        observeUiState()

        viewModel.loadHistory()
    }

    private fun createViewModel(): DeviceHistoryViewModel {
        val tokenManager = TokenManager(applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val repository = DeviceRepository(deviceApi)
        val factory = DeviceHistoryViewModelFactory(repository, deviceId)
        return ViewModelProvider(this, factory)[DeviceHistoryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        historyAdapter = DeviceHistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = historyAdapter
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadHistory()
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.loadPreviousPage()
        }

        binding.btnNext.setOnClickListener {
            viewModel.loadNextPage()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateLoadingState(state.isLoading)
                updateErrorState(state.errorMessage)
                updateEmptyState(state.isEmpty)
                updateContentState(state.history, state.pagination, state.currentPage)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateErrorState(errorMessage: String?) {
        binding.layoutError.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (errorMessage != null) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateContentState(
        history: List<com.example.safehome.data.remote.HistoryRecordDto>,
        pagination: com.example.safehome.data.remote.PaginationDto?,
        currentPage: Int
    ) {
        historyAdapter.submitList(history)

        if (pagination != null && pagination.totalPages > 1) {
            binding.layoutPagination.visibility = View.VISIBLE
            binding.txtPageInfo.text = "Trang $currentPage / ${pagination.totalPages}"
            
            binding.btnPrevious.isEnabled = currentPage > 1
            binding.btnNext.isEnabled = currentPage < pagination.totalPages
        } else {
            binding.layoutPagination.visibility = View.GONE
        }
    }
}
