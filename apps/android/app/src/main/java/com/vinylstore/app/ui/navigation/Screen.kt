package com.vinylstore.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * App 导航路由定义。
 * 原生页面用 route path；WebView 页面用 WebViewScreen 承载。
 */
sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {

    // ── 底部导航 Tab ──────────────────────────────────
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Catalog : Screen("catalog", "分类", Icons.Default.List)
    data object Cart : Screen("cart", "购物车", Icons.Default.ShoppingCart)
    data object Profile : Screen("profile", "我的", Icons.Default.Person)

    // ── 原生页面 ───────────────────────────────────────
    data object AlbumDetail : Screen("album_detail/{slug}") {
        fun createRoute(slug: String) = "album_detail/$slug"
    }

    // ── WebView 通用页面 ───────────────────────────────
    data object WebPage : Screen("web/{path}") {
        fun createRoute(path: String) = "web/$path"
    }

    companion object {
        /** 底部导航 Tab 列表，按顺序排列 */
        val bottomTabs = listOf(Home, Catalog, Cart, Profile)

        /** WebView 页面路由（不含底部导航 Tab 中的 WebView 页面） */
        val webViewRouteMap = mapOf(
            "catalog" to "/catalog",
            "cart" to "/cart",
            "orders" to "/orders",
            "order_detail" to "/orders/{id}",
            "checkout" to "/checkout",
            "login" to "/login",
            "register" to "/register",
            "profile" to "/profile",
            "messages" to "/messages",
            "search" to "/search",
            "artist" to "/artists/{slug}",
            "seller" to "/seller/{id}"
        )
    }
}
