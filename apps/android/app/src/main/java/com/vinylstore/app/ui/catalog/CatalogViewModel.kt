package com.vinylstore.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Category
import com.vinylstore.app.data.model.ColorOption
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val albums: List<Album> = emptyList(),
    val categories: List<Category> = emptyList(),
    val colors: List<ColorOption> = emptyList(),
    val selectedCategory: String? = null,
    val selectedColor: String? = null,
    val selectedCountry: String? = null,
    val sort: String? = null,
    val order: String = "desc",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CatalogViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(page: Int = 1) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                // 并行加载分类、颜色和专辑
                val categories = albumRepository.getCategories()
                val colors = albumRepository.getColors()
                val res = albumRepository.getAlbums(
                    page = page,
                    limit = 20,
                    category = state.selectedCategory,
                    color = state.selectedColor,
                    country = state.selectedCountry,
                    sort = state.sort,
                    order = state.order
                )
                _uiState.value = _uiState.value.copy(
                    albums = res.data,
                    categories = categories,
                    colors = colors,
                    currentPage = res.pagination?.page ?: 1,
                    totalPages = res.pagination?.totalPages ?: 1,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun setCategory(slug: String?) {
        val new = if (_uiState.value.selectedCategory == slug) null else slug
        _uiState.value = _uiState.value.copy(selectedCategory = new)
        load(1)
    }

    fun setColor(color: String?) {
        val new = if (_uiState.value.selectedColor == color) null else color
        _uiState.value = _uiState.value.copy(selectedColor = new?.lowercase())
        load(1)
    }

    fun setCountry(country: String?) {
        _uiState.value = _uiState.value.copy(selectedCountry = country)
        load(1)
    }

    fun setSort(sort: String?, order: String) {
        _uiState.value = _uiState.value.copy(sort = sort, order = order)
        load(1)
    }

    fun setPage(page: Int) { load(page) }

    class Factory(private val albumRepository: AlbumRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                return CatalogViewModel(albumRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
