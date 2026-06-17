package com.vinylstore.app.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.CartItem
import com.vinylstore.app.data.repository.AuthRepository
import com.vinylstore.app.data.repository.CartRepository
import com.vinylstore.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val items: List<CartItem> = emptyList(),
    val total: Int = 0,
    val balance: Int = 0,
    val shippingAddress: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false
)

class CheckoutViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val cart = cartRepository.getCart()
                var balance = 0
                var address = ""
                try {
                    val profile = authRepository.getProfile()
                    balance = profile.balance
                    address = profile.address ?: ""
                } catch (_: Exception) { }

                _uiState.value = CheckoutUiState(
                    items = cart.items,
                    total = cart.total,
                    balance = balance,
                    shippingAddress = address,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单信息失败"
                )
            }
        }
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(shippingAddress = address)
    }

    fun confirmCheckout() {
        val state = _uiState.value
        if (state.isSubmitting || state.submitted) return

        val address = state.shippingAddress.trim()
        if (address.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请先填写收货地址")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                orderRepository.checkout(address)
                // Refresh cart to clear items
                cartRepository.getCart()
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "下单失败，请重试"
                )
            }
        }
    }

    class Factory(
        private val cartRepository: CartRepository,
        private val orderRepository: OrderRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
                return CheckoutViewModel(cartRepository, orderRepository, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
