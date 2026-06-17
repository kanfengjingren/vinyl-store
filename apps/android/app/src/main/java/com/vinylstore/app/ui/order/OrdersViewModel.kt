package com.vinylstore.app.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Order
import com.vinylstore.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OrderTab(val label: String, val key: String) {
    ALL("全部", "all"),
    PENDING("待付款", "PENDING"),
    RECEIVING("待收货", "receiving"),
    COMPLETED("已完成", "DELIVERED")
}

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val selectedTab: OrderTab = OrderTab.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionError: String? = null
) {
    val filteredOrders: List<Order>
        get() = when (selectedTab) {
            OrderTab.ALL -> orders
            OrderTab.PENDING -> orders.filter { it.status == "PENDING" }
            OrderTab.RECEIVING -> orders.filter { it.status == "PAID" || it.status == "SHIPPED" }
            OrderTab.COMPLETED -> orders.filter { it.status == "DELIVERED" }
        }

    fun countForTab(tab: OrderTab): Int = when (tab) {
        OrderTab.ALL -> orders.size
        OrderTab.PENDING -> orders.count { it.status == "PENDING" }
        OrderTab.RECEIVING -> orders.count { it.status == "PAID" || it.status == "SHIPPED" }
        OrderTab.COMPLETED -> orders.count { it.status == "DELIVERED" }
    }
}

class OrdersViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, actionError = null)
            try {
                val orders = orderRepository.getOrders()
                _uiState.value = _uiState.value.copy(orders = orders, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载订单失败"
                )
            }
        }
    }

    fun setTab(tab: OrderTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun cancelOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionError = e.message ?: "取消订单失败"
                )
            }
        }
    }

    fun payOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                orderRepository.payOrder(orderId)
                loadOrders()
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

    class Factory(private val orderRepository: OrderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrdersViewModel::class.java)) {
                return OrdersViewModel(orderRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
