package com.vinylstore.app.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Artist
import com.vinylstore.app.data.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ArtistViewModel(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private var currentSlug: String = ""

    fun loadArtist(slug: String) {
        if (currentSlug == slug && _uiState.value.artist != null) return
        currentSlug = slug

        viewModelScope.launch {
            _uiState.value = ArtistUiState(isLoading = true)
            try {
                val artist = artistRepository.getArtistBySlug(slug)
                _uiState.value = ArtistUiState(artist = artist)
            } catch (e: Exception) {
                _uiState.value = ArtistUiState(error = e.message ?: "加载艺人信息失败")
            }
        }
    }

    class Factory(private val artistRepository: ArtistRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ArtistViewModel::class.java)) {
                return ArtistViewModel(artistRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
