package com.vinylstore.app.ui.navigation

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vinylstore.app.BuildConfig
import com.vinylstore.app.bridge.WebViewBridge
import com.vinylstore.app.data.repository.AlbumRepository
import com.vinylstore.app.local.TokenStorage
import com.vinylstore.app.ui.native.album.AlbumDetailScreen
import com.vinylstore.app.ui.native.home.HomeScreen
import com.vinylstore.app.ui.native.home.HomeViewModel

/**
 * Web 页面路由 → Vue Router path 映射
 */
private val tabRoutes = mapOf(
    1 to "/catalog",
    2 to "/cart",
    3 to "/profile"
)

@Composable
fun AppNavigation(
    tokenStorage: TokenStorage,
    albumRepository: AlbumRepository
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val bridge = remember {
        WebViewBridge(
            tokenStorage = tokenStorage,
            onNavigateNative = { path, params ->
                when (path) {
                    "album_detail" -> {
                        params["slug"]?.let { slug ->
                            navController.navigate("album_detail/$slug")
                        }
                    }
                }
            }
        )
    }

    // 共享 WebView（在 AppNavigation 层级创建，所有子页面可引用）
    val sharedWebView = remember {
        createSharedWebView(context, bridge, tokenStorage)
    }

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainTabScreen(
                navController = navController,
                webView = sharedWebView,
                tokenStorage = tokenStorage,
                albumRepository = albumRepository
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
                onPlayTrack = { trackJson, artistName ->
                    injectPlayTrack(sharedWebView, trackJson, artistName)
                }
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

private fun injectPlayTrack(webView: WebView, trackJson: String, artistName: String) {
    val escapedTrack = trackJson
        .replace("\\", "\\\\")
        .replace("'", "\\'")
    val escapedArtist = artistName
        .replace("\\", "\\\\")
        .replace("'", "\\'")
    webView.evaluateJavascript(
        "window.playTrackFromNative('$escapedTrack', '$escapedArtist')",
        null
    )
}

/**
 * 将 token 注入 WebView 的 localStorage
 */
private fun injectAuthToken(view: WebView?, tokenStorage: TokenStorage) {
    val token = tokenStorage.getTokenSync()
    if (token.isNotBlank()) {
        val escaped = token.replace("'", "\\'")
        view?.evaluateJavascript(
            """
            (function(){
                var t = localStorage.getItem('token');
                if (t !== '$escaped') { localStorage.setItem('token', '$escaped'); }
            })();
            """.trimIndent(), null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MainTabScreen(
    navController: androidx.navigation.NavController,
    webView: WebView,
    tokenStorage: TokenStorage,
    albumRepository: AlbumRepository
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentTitle by remember { mutableStateOf("幻觉贸易") }
    val tabs = Screen.bottomTabs

    // 设置标题监听（webView 实例由外层创建）
    LaunchedEffect(webView) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                currentTitle = title ?: ""
            }
        }
    }

    // 当 Tab 切换时，用 Vue Router history API 导航（不刷新整个 WebView）
    LaunchedEffect(selectedTab) {
        if (selectedTab in tabRoutes) {
            val path = tabRoutes[selectedTab]!!
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
            CenterAlignedTopAppBar(
                title = { Text(currentTitle) },
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
            // WebView 实例通过 remember 持久化，不会因 if 移除而销毁
            if (selectedTab != 0) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

