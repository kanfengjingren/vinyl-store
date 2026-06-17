package com.vinylstore.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vinylstore.app.VinylApp
import com.vinylstore.app.ui.auth.ForgotPasswordScreen
import com.vinylstore.app.ui.auth.LoginScreen
import com.vinylstore.app.ui.auth.RegisterScreen
import com.vinylstore.app.ui.catalog.CatalogScreen
import com.vinylstore.app.ui.catalog.NewArrivalsScreen
import com.vinylstore.app.ui.catalog.SearchScreen
import com.vinylstore.app.ui.artist.ArtistScreen
import com.vinylstore.app.ui.components.FullPlayerOverlay
import com.vinylstore.app.ui.components.MiniPlayerBar
import com.vinylstore.app.ui.components.PlaceholderScreen
import com.vinylstore.app.ui.album.AlbumDetailScreen
import com.vinylstore.app.ui.cart.CartScreen
import com.vinylstore.app.ui.checkout.CheckoutScreen
import com.vinylstore.app.ui.home.HomeScreen
import com.vinylstore.app.ui.order.OrderDetailScreen
import com.vinylstore.app.ui.order.OrdersScreen
import com.vinylstore.app.ui.profile.ProfileScreen
import com.vinylstore.app.ui.seller.SellerScreen
import com.vinylstore.app.ui.social.ChatScreen
import com.vinylstore.app.ui.social.FriendsScreen
import com.vinylstore.app.ui.social.MessagesScreen
import com.vinylstore.app.ui.social.PublicProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val app = VinylApp.instance
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLoggedIn by app.authRepository.isLoggedInFlow.collectAsState(initial = false)

    // ── 导航守卫：未登录时跳转登录页 ──
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute in Screen.protectedRoutes && !isLoggedIn) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route)
            }
        }
        if (currentRoute != null && currentRoute in Screen.guestOnlyRoutes && isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val isTabScreen = currentRoute in Screen.bottomTabs.map { it.route }
    val playerState by app.playerManager.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            if (isTabScreen) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.Home.route -> "幻觉贸易"
                                Screen.Catalog.route -> "分类"
                                Screen.Cart.route -> "购物车"
                                Screen.Messages.route -> "消息"
                                Screen.Profile.route -> "我的"
                                else -> ""
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            Column {
                // Mini player above nav bar
                if (playerState.track != null) {
                    MiniPlayerBar(
                        playerManager = app.playerManager,
                        onExpand = { showFullPlayer = true }
                    )
                }
                if (isTabScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        Screen.bottomTabs.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ═══════════════════════════════════════
            // 底部 Tab
            // ═══════════════════════════════════════
            composable(Screen.Home.route) {
                HomeScreen(
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) },
                    onCategoryClick = { slug -> navController.navigate(Screen.Catalog.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) }
                )
            }
            composable(Screen.Catalog.route) {
                CatalogScreen(
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) }
                )
            }
            composable(Screen.Cart.route) {
                CartScreen(
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) },
                    onCheckoutClick = { navController.navigate(Screen.Checkout.route) },
                    onGoShopping = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) },
                    onOrdersClick = { navController.navigate(Screen.Orders.route) },
                    onLoginRequired = { navController.navigate(Screen.Login.route) },
                    onLoggedOut = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ═══════════════════════════════════════
            // 专辑详情（支持可选 commentId）
            // ═══════════════════════════════════════
            composable(
                route = "album_detail/{slug}?commentId={commentId}",
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("commentId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                val commentId = backStackEntry.arguments?.getInt("commentId")?.takeIf { it > 0 }
                AlbumDetailScreen(
                    slug = slug,
                    highlightCommentId = commentId,
                    onBack = { navController.popBackStack() },
                    onArtistClick = { artistSlug ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artistSlug))
                    },
                    onSellerClick = { sellerId ->
                        navController.navigate(Screen.SellerDetail.createRoute(sellerId))
                    },
                    onAlbumClick = { albumSlug ->
                        navController.navigate("album_detail/$albumSlug")
                    },
                    onCategoryClick = { _ ->
                        navController.navigate(Screen.Catalog.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onUserClick = { userId ->
                        navController.navigate(Screen.PublicProfile.createRoute(userId))
                    },
                    onLoginRequired = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }

            // ═══════════════════════════════════════
            // 认证（阶段 2） ✅
            // ═══════════════════════════════════════
            composable(Screen.Login.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════
            // 艺人 / 卖家 / 搜索 / 新品 / 结账 / 订单 / 公开主页 / 消息 / 好友 / 设置
            // ═══════════════════════════════════════
            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
                ArtistScreen(
                    slug = slug,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumSlug ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumSlug))
                    }
                )
            }
            composable(
                route = Screen.SellerDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                SellerScreen(
                    sellerId = id,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumSlug ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumSlug))
                    },
                    onChatClick = { partnerId ->
                        navController.navigate(Screen.Chat.createRoute(partnerId))
                    },
                    onLoginRequired = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) }
                )
            }
            composable(Screen.NewArrivals.route) {
                NewArrivalsScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) }
                )
            }
            composable(Screen.Checkout.route) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { goToOrders ->
                        if (goToOrders) {
                            navController.navigate(Screen.Orders.route) {
                                popUpTo(Screen.Home.route)
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    onAlbumClick = { slug -> navController.navigate(Screen.AlbumDetail.createRoute(slug)) }
                )
            }
            composable(Screen.Orders.route) {
                OrdersScreen(
                    onOrderClick = { orderId ->
                        navController.navigate(Screen.OrderDetail.createRoute(orderId))
                    },
                    onAlbumClick = { slug ->
                        navController.navigate(Screen.AlbumDetail.createRoute(slug))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
                OrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { slug ->
                        navController.navigate(Screen.AlbumDetail.createRoute(slug))
                    }
                )
            }
            composable(
                route = Screen.PublicProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
                PublicProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { slug -> navController.navigate("album_detail/$slug") },
                    onChatClick = { partnerId -> navController.navigate(Screen.Chat.createRoute(partnerId)) },
                    onLoginRequired = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Messages.route) {
                MessagesScreen(
                    onChatClick = { partnerId -> navController.navigate(Screen.Chat.createRoute(partnerId)) },
                    onUserClick = { userId -> navController.navigate(Screen.PublicProfile.createRoute(userId)) },
                    onFriendsClick = { navController.navigate(Screen.Friends.route) },
                    onLoginRequired = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("partnerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val partnerId = backStackEntry.arguments?.getInt("partnerId") ?: return@composable
                ChatScreen(
                    partnerId = partnerId,
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Screen.PublicProfile.createRoute(userId)) },
                    onAlbumClick = { slug, commentId ->
                        if (commentId != null) {
                            navController.navigate("album_detail/$slug?commentId=$commentId")
                        } else {
                            navController.navigate("album_detail/$slug")
                        }
                    }
                )
            }
            composable(Screen.Friends.route) {
                FriendsScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Screen.PublicProfile.createRoute(userId)) },
                    onChatClick = { partnerId -> navController.navigate(Screen.Chat.createRoute(partnerId)) },
                    onLoginRequired = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen("设置", "阶段 7 实现")
            }
        }
    }

        // ── 全屏播放器 ──
        FullPlayerOverlay(
            playerManager = app.playerManager,
            visible = showFullPlayer,
            onDismiss = { showFullPlayer = false }
        )
    } // Box
}

