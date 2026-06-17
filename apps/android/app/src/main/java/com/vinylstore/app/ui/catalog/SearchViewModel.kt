package com.vinylstore.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.AlbumSuggestion
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val suggestions: List<AlbumSuggestion> = emptyList(),
    val results: List<Album> = emptyList(),
    val isSearching: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        // 防抖 300ms
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(300)
            if (query.isNotBlank()) {
                try {
                    val s = albumRepository.getSuggestions(query)
                    _uiState.value = _uiState.value.copy(suggestions = s)
                } catch (_: Exception) {}
            } else {
                _uiState.value = _uiState.value.copy(suggestions = emptyList())
            }
        }
    }

    fun search(page: Int = 1) {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isSearching = true, error = null)
            try {
                val res = albumRepository.getAlbums(page = page, limit = 20, search = q, sort = "createdAt", order = "desc")
                _uiState.value = _uiState.value.copy(
                    results = res.data,
                    currentPage = res.pagination?.page ?: 1,
                    totalPages = res.pagination?.totalPages ?: 1,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = e.message ?: "搜索失败"
                )
            }
        }
    }

    fun setPage(page: Int) { search(page) }

    fun clearSuggestions() {
        _uiState.value = _uiState.value.copy(suggestions = emptyList())
    }

    class Factory(private val albumRepository: AlbumRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(albumRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
