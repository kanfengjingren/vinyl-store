package com.vinylstore.app.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Order
import com.vinylstore.app.data.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionError: String? = null,
    val countdown: String = ""
)

class OrderDetailViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun loadOrder(orderId: Int) {
        stopCountdown()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, actionError = null)
            try {
                val order = orderRepository.getOrderById(orderId)
                _uiState.value = _uiState.value.copy(order = order, isLoading = false)
                startCountdown()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单失败"
                )
            }
        }
    }

    private fun startCountdown() {
        val order = _uiState.value.order ?: return
        if (order.status != "PENDING" || order.expiresAt == null) return

        countdownJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val expiresMs = java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
                    ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        .parse(order.expiresAt)?.time ?: break

                    val left = expiresMs - System.currentTimeMillis()
                    if (left <= 0) {
                        _uiState.value = _uiState.value.copy(countdown = "订单已超时")
                        break
                    }
                    val m = left / 60000
                    val s = (left % 60000) / 1000
                    _uiState.value = _uiState.value.copy(countdown = "${m}分${s}秒后自动关闭")
                } catch (_: Exception) { break }
                delay(1000)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    fun cancelOrder() {
        val order = _uiState.value.order ?: return
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(order.id)
                loadOrder(order.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionError = e.message ?: "取消订单失败"
                )
            }
        }
    }

    fun payOrder() {
        val order = _uiState.value.order ?: return
        viewModelScope.launch {
            try {
                orderRepository.payOrder(order.id)
                loadOrder(order.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionError = e.message ?: "付款失败"
                )
            }
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
    }

    class Factory(private val orderRepository: OrderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderDetailViewModel::class.java)) {
                return OrderDetailViewModel(orderRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
