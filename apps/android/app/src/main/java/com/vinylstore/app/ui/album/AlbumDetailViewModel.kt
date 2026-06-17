package com.vinylstore.app.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.model.Album
import com.vinylstore.app.data.model.Comment
import com.vinylstore.app.data.model.CommentReply
import com.vinylstore.app.data.repository.AlbumRepository
import com.vinylstore.app.data.repository.CartRepository
import com.vinylstore.app.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorited: Boolean = false,
    val avgScore: Float = 0f,
    val ratingCount: Int = 0,
    val userRating: Int = 0,
    val hoverStar: Int = 0,
    val isRatingSubmitting: Boolean = false,
    val isAddingToCart: Boolean = false,
    val cartMessage: String? = null,
    // ── 评论 ──
    val comments: List<Comment> = emptyList(),
    val commentsPage: Int = 1,
    val commentsTotalPages: Int = 1,
    val commentsTotal: Int = 0,
    val commentsLoading: Boolean = false,
    val commentsLoadingMore: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val replyTarget: Int? = null,                              // 正在回复的评论 ID
    val expandedReplies: Map<Int, List<CommentReply>> = emptyMap(), // commentId → 已加载的回复列表
    val highlightCommentId: Int? = null                         // 从导航参数来的高亮评论
)

class AlbumDetailViewModel(
    private val albumRepository: AlbumRepository,
    private val cartRepository: CartRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private var currentSlug: String = ""

    fun loadAlbum(slug: String) {
        if (currentSlug == slug && _uiState.value.album != null) return
        currentSlug = slug
        commentsLoaded = false
        isSubmitting = false

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null,
                comments = emptyList(), commentsPage = 1, commentsTotalPages = 1, commentsTotal = 0
            )
            try {
                val album = albumRepository.getAlbumBySlug(slug)

                // 并行加载评分和收藏状态
                var avgScore = 0f
                var ratingCount = 0
                var userRating = 0
                var isFavorited = false

                try {
                    val rating = albumRepository.getAlbumRating(album.id)
                    avgScore = rating.average ?: 0f
                    ratingCount = rating.count
                    userRating = rating.userScore ?: 0
                } catch (_: Exception) {}

                try {
                    isFavorited = albumRepository.isFavorited(album.id)
                } catch (_: Exception) {}

                _uiState.value = _uiState.value.copy(
                    album = album,
                    isLoading = false,
                    avgScore = avgScore,
                    ratingCount = ratingCount,
                    userRating = userRating,
                    isFavorited = isFavorited
                )

                // 加载评论（同一协程内，避免新协程竞态）
                loadCommentsSync(1)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载专辑失败"
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // 评分 & 收藏
    // ═══════════════════════════════════════════

    fun setHoverStar(star: Int) {
        _uiState.value = _uiState.value.copy(hoverStar = star)
    }

    fun toggleFavorite() {
        val album = _uiState.value.album ?: return
        viewModelScope.launch {
            try {
                val result = albumRepository.toggleFavorite(album.id)
                _uiState.value = _uiState.value.copy(isFavorited = result)
            } catch (_: Exception) {}
        }
    }

    fun rateAlbum(score: Int) {
        val album = _uiState.value.album ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRatingSubmitting = true)
            try {
                albumRepository.rateAlbum(album.id, score)
                val rating = albumRepository.getAlbumRating(album.id)
                _uiState.value = _uiState.value.copy(
                    userRating = score,
                    avgScore = rating.average ?: 0f,
                    ratingCount = rating.count,
                    isRatingSubmitting = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isRatingSubmitting = false)
            }
        }
    }

    fun addToCart() {
        val album = _uiState.value.album ?: return
        if (album.stock <= 0) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToCart = true, cartMessage = null)
            try {
                cartRepository.addToCart(album.id, 1)
                _uiState.value = _uiState.value.copy(
                    isAddingToCart = false,
                    cartMessage = "已加入购物车"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingToCart = false,
                    cartMessage = e.message ?: "添加失败"
                )
            }
        }
    }

    fun clearCartMessage() {
        _uiState.value = _uiState.value.copy(cartMessage = null)
    }

    // ═══════════════════════════════════════════
    // 评论
    // ═══════════════════════════════════════════

    private var commentsLoaded = false

    /** 同步加载评论（在调用方的协程内执行，不开新协程） */
    private suspend fun loadCommentsSync(page: Int = 1) {
        val albumId = _uiState.value.album?.id ?: return
        if (page == 1 && commentsLoaded && _uiState.value.comments.isNotEmpty()) return
        _uiState.value = _uiState.value.copy(
            commentsLoading = page == 1,
            commentsLoadingMore = page > 1
        )
        try {
            val response = commentRepository.getComments(albumId, page, 10)
            _uiState.value = _uiState.value.copy(
                comments = if (page == 1) (response.data ?: emptyList())
                           else _uiState.value.comments + (response.data ?: emptyList()),
                commentsPage = page,
                commentsTotalPages = response.pagination?.totalPages ?: 1,
                commentsTotal = response.pagination?.total ?: 0,
                commentsLoading = false,
                commentsLoadingMore = false
            )
            if (page == 1) commentsLoaded = true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                commentsLoading = false,
                commentsLoadingMore = false
            )
        }
    }

    /** 公开的分页加载评论（开新协程，用于 UI 分页按钮） */
    fun loadComments(page: Int = 1) {
        val albumId = _uiState.value.album?.id ?: return
        // 防止并发重复加载
        if (page == 1 && commentsLoaded && _uiState.value.comments.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                commentsLoading = page == 1,
                commentsLoadingMore = page > 1
            )
            try {
                val response = commentRepository.getComments(albumId, page, 10)
                _uiState.value = _uiState.value.copy(
                    comments = if (page == 1) response.data ?: emptyList()
                               else _uiState.value.comments + (response.data ?: emptyList()),
                    commentsPage = page,
                    commentsTotalPages = response.pagination?.totalPages ?: 1,
                    commentsTotal = response.pagination?.total ?: 0,
                    commentsLoading = false,
                    commentsLoadingMore = false
                )
                if (page == 1) commentsLoaded = true
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    commentsLoading = false,
                    commentsLoadingMore = false
                )
            }
        }
    }

    fun loadMoreComments() {
        val nextPage = _uiState.value.commentsPage + 1
        if (nextPage > _uiState.value.commentsTotalPages) return
        loadComments(nextPage)
    }

    private var isSubmitting = false

    fun submitComment(content: String, parentId: Int?) {
        val albumId = _uiState.value.album?.id ?: return
        if (content.isBlank()) return
        // 防重入
        if (isSubmitting) return
        isSubmitting = true

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            try {
                commentRepository.createComment(albumId, content, parentId)
                commentsLoaded = false
                _uiState.value = _uiState.value.copy(
                    isSubmittingComment = false,
                    replyTarget = null
                )
                isSubmitting = false
                // 重新加载评论（同一协程内）
                loadCommentsSync(1)
            } catch (e: Exception) {
                isSubmitting = false
                _uiState.value = _uiState.value.copy(
                    isSubmittingComment = false,
                    cartMessage = e.message ?: "评论失败"
                )
            }
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            try {
                commentRepository.deleteComment(commentId)
                loadComments(_uiState.value.commentsPage)
            } catch (_: Exception) {}
        }
    }

    fun loadReplies(commentId: Int) {
        if (_uiState.value.expandedReplies.containsKey(commentId)) return
        viewModelScope.launch {
            try {
                val replies = commentRepository.getReplies(commentId)
                val newMap = _uiState.value.expandedReplies.toMutableMap()
                newMap[commentId] = replies
                _uiState.value = _uiState.value.copy(expandedReplies = newMap)
            } catch (_: Exception) {}
        }
    }

    fun setReplyTarget(commentId: Int?) {
        _uiState.value = _uiState.value.copy(replyTarget = commentId)
    }

    fun setHighlightCommentId(commentId: Int) {
        _uiState.value = _uiState.value.copy(highlightCommentId = commentId)
    }

    class Factory(
        private val albumRepository: AlbumRepository,
        private val cartRepository: CartRepository,
        private val commentRepository: CommentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumDetailViewModel::class.java)) {
                return AlbumDetailViewModel(albumRepository, cartRepository, commentRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
