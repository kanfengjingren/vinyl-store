package com.vinylstore.app.ui.native.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Category
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val featured: Album? = null,
    val categories: List<Category> = emptyList(),
    val categoryAlbums: Map<String, List<Album>> = emptyMap(),
    val latestAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "starting loadHomeData")
                Log.d(TAG, "API_BASE_URL: ${com.vinylstore.app.BuildConfig.API_BASE_URL}")
                val featured = albumRepository.getFeatured()
                Log.d(TAG, "featured loaded: $featured")
                val categories = albumRepository.getCategories()
                Log.d(TAG, "categories loaded: ${categories.size}")
                val latest = albumRepository.getAlbums(
                    page = 1,
                    limit = 12,
                    sort = "createdAt",
                    order = "desc"
                )
                Log.d(TAG, "latest loaded: ${latest.size}")

                val categoryAlbumMap = mutableMapOf<String, List<Album>>()
                for (cat in categories.take(4)) {
                    val catAlbums = try {
                        albumRepository.getAlbums(
                            page = 1, limit = 6, category = cat.slug
                        )
                    } catch (_: Exception) {
                        emptyList()
                    }
                    categoryAlbumMap[cat.slug] = catAlbums
                }

                _uiState.value = HomeUiState(
                    featured = featured,
                    categories = categories,
                    categoryAlbums = categoryAlbumMap,
                    latestAlbums = latest,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadHomeData failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    class Factory(private val albumRepository: AlbumRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(albumRepository) as T
        }
    }
}
