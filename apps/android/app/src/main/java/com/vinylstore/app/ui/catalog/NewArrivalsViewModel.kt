package com.vinylstore.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NewArrivalsUiState(
    val albums: List<Album> = emptyList(),
    val dates: List<String> = emptyList(),    // 最近 7 天 YYYY-MM-DD
    val selectedDate: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NewArrivalsViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewArrivalsUiState())
    val uiState: StateFlow<NewArrivalsUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // 生成最近 7 天日期列表
        val cal = Calendar.getInstance()
        val dates = (0 until 7).map {
            cal.add(Calendar.DAY_OF_MONTH, if (it == 0) 0 else -1)
            dateFormat.format(cal.time)
        }
        _uiState.value = _uiState.value.copy(dates = dates, selectedDate = dates.first())
        load(dates.first())
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        load(date)
    }

    private fun load(date: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = albumRepository.getAlbums(
                    page = 1, limit = 50, date = date, sort = "createdAt", order = "desc"
                )
                _uiState.value = _uiState.value.copy(
                    albums = res.data, isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = e.message ?: "加载失败"
                )
            }
        }
    }

    class Factory(private val albumRepository: AlbumRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NewArrivalsViewModel::class.java)) {
                return NewArrivalsViewModel(albumRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
