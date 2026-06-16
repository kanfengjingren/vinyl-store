package com.vinylstore.app.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.vinylstore.app.BuildConfig
import com.vinylstore.app.bridge.WebViewBridge
import com.vinylstore.app.data.model.resolveCoverUrl
import com.vinylstore.app.data.repository.AlbumRepository
import com.vinylstore.app.local.TokenStorage
import com.vinylstore.app.ui.theme.*
import com.vinylstore.app.ui.native.album.AlbumDetailScreen
import com.vinylstore.app.ui.native.home.HomeScreen
import com.vinylstore.app.ui.native.home.HomeViewModel
import kotlinx.coroutines.channels.Channel

/**
 * Web 页面路由 → Vue Router path 映射
 */
private val tabRoutes = mapOf(
    1 to "/catalog",
    2 to "/cart",
    3 to "/profile"
)

data class PlaybackState(
    val trackTitle: String,
    val artistName: String,
    val coverUrl: String?,
    val gradient: String?,
    val isPlaying: Boolean = true,
    val currentSeconds: Float = 0f,
    val duration: Float = 0f
)

@Composable
fun AppNavigation(
    tokenStorage: TokenStorage,
    albumRepository: AlbumRepository
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Tab 切换控制（WebView 内部可通过 bridge 切到首页）
    var homeTabIndex by remember { mutableIntStateOf(0) }

    // Channel: Bridge(非UI线程) → Compose 主线程
    val navChannel = remember { Channel<Pair<String, Map<String, String>>>(Channel.UNLIMITED) }
    // Channel: WebView 播放状态 → Compose
    val playbackChannel = remember { Channel<PlaybackState?>(Channel.UNLIMITED) }

    val bridge = remember {
        WebViewBridge(
            tokenStorage = tokenStorage,
            onNavigateNative = { path, params ->
                navChannel.trySend(path to params)
            },
            onPlaybackChanged = { title, artist, cover, gradient, playing, sec, dur ->
                playbackChannel.trySend(PlaybackState(title, artist, cover, gradient, playing, sec, dur))
            },
            onPlaybackCleared = {
                playbackChannel.trySend(null)
            }
        )
    }

    // 共享 WebView
    val sharedWebView = remember {
        createSharedWebView(context, bridge, tokenStorage)
    }

    // 统一的播放状态（全局 MiniPlayerBar 数据源）
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }

    // 消费 channel 事件
    LaunchedEffect(Unit) {
        while (true) {
            // 优先消费播放状态（高优先级）
            val pb = playbackChannel.tryReceive()
            if (pb.isSuccess) {
                playbackState = pb.getOrNull()
                continue
            }
            // 再消费导航事件
            val nav = navChannel.tryReceive()
            if (nav.isSuccess) {
                val (path, params) = nav.getOrNull() ?: continue
                when (path) {
                    "album_detail" -> {
                        params["slug"]?.let { slug ->
                            navController.navigate("album_detail/$slug")
                        }
                    }
                    "go_home" -> homeTabIndex = 0
                    "go_cart" -> homeTabIndex = 2
                }
                continue
            }
            // 没有事件，等待一小段时间
            kotlinx.coroutines.delay(50)
        }
    }

    // 最外层 Box：NavHost + 全局 MiniPlayerBar
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(bottom = if (playbackState != null) 56.dp else 0.dp)
        ) {
            composable("main") {
                MainTabScreen(
                    navController = navController,
                    webView = sharedWebView,
                    tokenStorage = tokenStorage,
                    albumRepository = albumRepository,
                    externalTabIndex = homeTabIndex,
                    onExternalTabConsumed = { homeTabIndex = -1 }
                )
            }

            composable(
                route = "album_detail/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                AlbumDetailScreen(
                    slug = slug,
                    albumsRepository = albumRepository,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { albumJson ->
                        injectAddToCart(sharedWebView, albumJson)
                    },
                    onPlayTrack = { trackJson, artistName, albumJson, trackListJson ->
                        // 更新全局播放状态
                        try {
                            val gson = com.google.gson.Gson()
                            val album = gson.fromJson(albumJson, com.vinylstore.app.data.model.Album::class.java)
                            val track = gson.fromJson(trackJson, com.vinylstore.app.data.model.Track::class.java)
                            playbackState = PlaybackState(
                                trackTitle = track.title,
                                artistName = artistName,
                                coverUrl = album.coverUrl,
                                gradient = album.gradient,
                                isPlaying = true
                            )
                        } catch (_: Exception) {}
                        injectPlayTrack(sharedWebView, trackJson, artistName, albumJson, trackListJson)
                    }
                )
            }
        }

        // 全局 MiniPlayerBar — 覆盖在所有页面之上
        playbackState?.let { state ->
            MiniPlayerBar(
                state = state,
                onTap = {
                    // 跳回 main 页面，切到 catalog tab 后打开全屏播放器
                    try { navController.popBackStack("main", false) } catch (_: Exception) {}
                    homeTabIndex = 1
                    sharedWebView.evaluateJavascript(
                        "window.openFullPlayer && window.openFullPlayer()",
                        null
                    )
                },
                onPlayPause = {
                    sharedWebView.evaluateJavascript(
                        "window.togglePlayback && window.togglePlayback()",
                        null
                    )
                },
                onClose = {
                    playbackState = null
                    // 通知 WebView 停止播放
                    sharedWebView.evaluateJavascript(
                        "window.stopPlayback && window.stopPlayback()",
                        null
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 创建共享 WebView，所有 Web 页面共享同一个 JS 上下文。
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createSharedWebView(
    context: android.content.Context,
    bridge: WebViewBridge,
    tokenStorage: TokenStorage
): WebView {
    return WebView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = settings.userAgentString + " VinylStoreAndroid"
        }

        addJavascriptInterface(bridge, "AndroidBridge")

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectAuthToken(view, tokenStorage)
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean = false
        }

        webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                // Title handled in MainTabScreen
            }
        }

        // 初始化加载目录页
        loadUrl(BuildConfig.WEB_BASE_URL + "/catalog")
    }
}

/**
 * 向共享 WebView 注入 JS 调 Vue 端的 addToCartFromNative()
 */
private fun injectAddToCart(webView: WebView, albumJson: String) {
    val escaped = albumJson
        .replace("\\", "\\\\")
        .replace("'", "\\'")
    webView.evaluateJavascript(
        "window.addToCartFromNative('$escaped')",
        null
    )
}

private fun injectPlayTrack(webView: WebView, trackJson: String, artistName: String, albumJson: String, trackListJson: String) {
    fun escape(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")
    val escapedTrack = escape(trackJson)
    val escapedArtist = escape(artistName)
    val escapedAlbum = escape(albumJson)
    val escapedTrackList = escape(trackListJson)
    webView.evaluateJavascript(
        "window.playTrackFromNative('$escapedTrack', '$escapedArtist', '$escapedAlbum', '$escapedTrackList')",
        null
    )
}

/**
 * 将 token 注入 WebView 的 localStorage
 */
private fun injectAuthToken(view: WebView?, tokenStorage: TokenStorage) {
    val nativeToken = tokenStorage.getTokenSync()
    view?.evaluateJavascript(
        """
        (function(){
            var t = localStorage.getItem('token');
            var nativeToken = '${nativeToken.replace("'", "\\'")}';
            if (nativeToken && t !== nativeToken) {
                localStorage.setItem('token', nativeToken);
            }
            if (t && typeof AndroidBridge !== 'undefined' && AndroidBridge.setToken) {
                AndroidBridge.setToken(t);
            }
        })();
        """.trimIndent(), null
    )
}

@Composable
fun MiniPlayerBar(
    state: PlaybackState,
    onTap: () -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        state.gradient?.let { parseMiniGradient(it) }
                            ?: Brush.horizontalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))
                    )
                    .clickable { onTap() },
                contentAlignment = Alignment.Center
            ) {
                if (!state.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = resolveCoverUrl(state.coverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("♪", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(10.dp))

            Surface(
                onClick = onPlayPause,
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(17.dp),
                color = Gold
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isPlaying) "⏸" else "▶",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTap() }
            ) {
                Text(
                    text = state.trackTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Text(
                    "✕",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }
    }
}

private fun parseMiniGradient(gradient: String): Brush? {
    try {
        val hexRegex = Regex("#[0-9a-fA-F]{6}")
        val colors = hexRegex.findAll(gradient).toList()
        if (colors.size >= 2) {
            val c1 = Color(android.graphics.Color.parseColor(colors[0].value))
            val c2 = Color(android.graphics.Color.parseColor(colors[1].value))
            return Brush.horizontalGradient(listOf(c1, c2))
        }
    } catch (_: Exception) {}
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MainTabScreen(
    navController: androidx.navigation.NavController,
    webView: WebView,
    tokenStorage: TokenStorage,
    albumRepository: AlbumRepository,
    externalTabIndex: Int = -1,
    onExternalTabConsumed: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentTitle by remember { mutableStateOf("幻觉贸易") }
    var currentPath by remember { mutableStateOf("") }
    val tabPaths = setOf("/catalog", "/cart", "/profile")

    // 响应外部（WebViewBridge）请求的 Tab 切换
    LaunchedEffect(externalTabIndex) {
        if (externalTabIndex >= 0) {
            selectedTab = externalTabIndex
            onExternalTabConsumed()
        }
    }
    val tabs = Screen.bottomTabs

    // 设置标题监听 + 文件选择器
    val fileChooserCallback = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileChooserCallback.value ?: return@rememberLauncherForActivityResult
        fileChooserCallback.value = null
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val uris = when {
                data.clipData != null -> {
                    val cd = data.clipData!!
                    Array(cd.itemCount) { cd.getItemAt(it).uri }
                }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
            callback.onReceiveValue(uris)
        } else {
            callback.onReceiveValue(null)
        }
    }

    // 通过 JS 查询 WebView 当前路径
    fun trackPath() {
        webView.evaluateJavascript("window.location.pathname") { result ->
            if (result != null) {
                val path = result.trim().removeSurrounding("\"")
                if (path.isNotEmpty()) currentPath = path
            }
        }
    }

    // 设置 WebChromeClient + WebViewClient
    LaunchedEffect(webView) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                currentTitle = title ?: ""
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback.value?.onReceiveValue(null)
                fileChooserCallback.value = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                }
                try {
                    filePickerLauncher.launch(intent)
                } catch (e: Exception) {
                    fileChooserCallback.value = null
                    filePathCallback?.onReceiveValue(null)
                    return false
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectAuthToken(view, tokenStorage)
                trackPath()
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean = false
        }
    }

    // 当 Tab 切换时，用 Vue Router history API 导航并追踪路径
    LaunchedEffect(selectedTab) {
        if (selectedTab in tabRoutes) {
            val path = tabRoutes[selectedTab]!!
            currentPath = path
            webView.evaluateJavascript(
                """
                (function() {
                    if (window.location.pathname !== '$path') {
                        window.history.pushState({}, '', '$path');
                        window.dispatchEvent(new PopStateEvent('popstate'));
                    }
                })();
                """.trimIndent(), null
            )
        }
    }

    Scaffold(
        topBar = {
            val showBack = selectedTab != 0 && currentPath.isNotEmpty() && currentPath !in tabPaths
            CenterAlignedTopAppBar(
                title = { Text(currentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = {
                            trackPath()
                            webView.evaluateJavascript("window.history.back()", null)
                            webView.postDelayed({ trackPath() }, 300)
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                tabs.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            screen.icon?.let {
                                Icon(it, contentDescription = screen.label)
                            }
                        },
                        label = { Text(screen.label ?: "") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tab 0: 原生首页
            if (selectedTab == 0) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(albumRepository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onAlbumClick = { slug ->
                        navController.navigate("album_detail/$slug")
                    },
                    onCategoryClick = { slug ->
                        selectedTab = 1
                        webView.evaluateJavascript(
                            "window.history.pushState({}, '', '/catalog?category=$slug'); window.dispatchEvent(new PopStateEvent('popstate'));",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 共享 WebView（Tab 1-3）
            if (selectedTab != 0) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
