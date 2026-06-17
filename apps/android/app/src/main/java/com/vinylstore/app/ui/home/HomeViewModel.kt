package com.vinylstore.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Category
import com.vinylstore.app.data.model.HotAlbum
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val featured: Album? = null,
    val hotAlbums: List<HotAlbum> = emptyList(),
    val recommendations: List<Album> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryAlbums: Map<String, List<Album>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadHomeData() }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val featured = albumRepository.getFeatured()
                val hot = albumRepository.getHotAlbums(10)
                val categories = albumRepository.getCategories()

                // 并行加载每分类的专辑
                val categoryAlbums = mutableMapOf<String, List<Album>>()
                categories.take(5).map { cat ->
                    async {
                        try {
                            val res = albumRepository.getAlbums(
                                category = cat.slug, limit = 10, sort = "createdAt", order = "desc"
                            )
                            cat.slug to res.data
                        } catch (_: Exception) {
                            cat.slug to emptyList<Album>()
                        }
                    }
                }.forEach { deferred ->
                    val (slug, albums) = deferred.await()
                    categoryAlbums[slug] = albums
                }

                // 推荐（可选，登录用户才有）
                val recs = try {
                    albumRepository.getRecommendations(10)
                } catch (_: Exception) { emptyList() }

                _uiState.value = HomeUiState(
                    featured = featured,
                    hotAlbums = hot,
                    recommendations = recs,
                    categories = categories,
                    categoryAlbums = categoryAlbums,
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

    class Factory(private val albumRepository: AlbumRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(albumRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
