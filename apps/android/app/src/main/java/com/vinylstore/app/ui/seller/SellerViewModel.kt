package com.vinylstore.app.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.SellerDetail
import com.vinylstore.app.data.repository.SellerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SellerUiState(
    val seller: SellerDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SellerViewModel(
    private val sellerRepository: SellerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerUiState())
    val uiState: StateFlow<SellerUiState> = _uiState.asStateFlow()

    private var currentId: Int = -1

    fun loadSeller(id: Int) {
        if (currentId == id && _uiState.value.seller != null) return
        currentId = id

        viewModelScope.launch {
            _uiState.value = SellerUiState(isLoading = true)
            try {
                val seller = sellerRepository.getSellerById(id)
                _uiState.value = SellerUiState(seller = seller)
            } catch (e: Exception) {
                _uiState.value = SellerUiState(error = e.message ?: "加载卖家信息失败")
            }
        }
    }

    class Factory(private val sellerRepository: SellerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SellerViewModel::class.java)) {
                return SellerViewModel(sellerRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
