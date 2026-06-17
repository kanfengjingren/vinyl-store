package com.vinylstore.app.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinylstore.app.VinylApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    onChatClick: (Int) -> Unit,
    onLoginRequired: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: MessagesViewModel = viewModel(
        factory = MessagesViewModel.Factory(
            app.chatRepository, app.friendRepository, app.userRepository, app.chatSocketManager
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    // Socket 生命周期
    LaunchedEffect(Unit) {
        viewModel.connectSocket()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.disconnectSocket() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("我的好友") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        FriendsSection(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            friends = uiState.friends,
            pendingRequests = uiState.pendingRequests,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            searchLoading = uiState.searchLoading,
            actionLoading = uiState.actionLoading,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onSendFriendRequest = { viewModel.sendFriendRequest(it) },
            onAcceptRequest = { viewModel.acceptFriendRequest(it) },
            onRejectRequest = { viewModel.rejectFriendRequest(it) },
            onUserClick = onUserClick,
            onChatClick = onChatClick
        )
    }
}
