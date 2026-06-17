package com.vinylstore.app.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinylstore.app.VinylApp
import com.vinylstore.app.ui.components.ErrorState
import com.vinylstore.app.ui.components.LoadingState
import com.vinylstore.app.ui.home.AlbumCardSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewArrivalsScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val app = VinylApp.instance
    val viewModel: NewArrivalsViewModel = viewModel(
        factory = NewArrivalsViewModel.Factory(app.albumRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新品上架") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // ── 日期导航栏 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.dates.forEach { date ->
                    val selected = date == uiState.selectedDate
                    val label = date.takeLast(5) // MM-DD
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectDate(date) },
                        label = { Text(label) },
                        shape = MaterialTheme.shapes.extraSmall,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── 专辑网格 ──
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!, onRetry = { viewModel.selectDate(uiState.selectedDate ?: "") })
                uiState.albums.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这天没有新品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.albums, key = { it.id }) { album ->
                            AlbumCardSmall(album = album, onClick = { onAlbumClick(album.slug) })
                        }
                    }
                }
            }
        }
    }
}
