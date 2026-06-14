package com.vinylstore.app.ui.native.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AlbumDetailViewModel(
    private val albumRepository: AlbumRepository,
    private val slug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbum()
    }

    fun loadAlbum() {
        viewModelScope.launch {
            _uiState.value = AlbumDetailUiState(isLoading = true)
            try {
                val album = albumRepository.getAlbumBySlug(slug)
                _uiState.value = AlbumDetailUiState(album = album)
            } catch (e: Exception) {
                _uiState.value = AlbumDetailUiState(
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    class Factory(
        private val albumRepository: AlbumRepository,
        private val slug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlbumDetailViewModel(albumRepository, slug) as T
        }
    }
}
