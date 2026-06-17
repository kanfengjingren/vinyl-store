package com.vinylstore.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String? = null,
    val icon: ImageVector? = null
) {
    // ── 底部导航 Tab ──
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Catalog : Screen("catalog", "分类", Icons.Default.Search)
    object Cart : Screen("cart", "购物车", Icons.Default.ShoppingCart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)

    // ── 子页面 ──
    object AlbumDetail : Screen("album_detail/{slug}") {
        fun createRoute(slug: String) = "album_detail/$slug"
    }
    object ArtistDetail : Screen("artist/{slug}") {
        fun createRoute(slug: String) = "artist/$slug"
    }
    object SellerDetail : Screen("seller/{id}") {
        fun createRoute(id: Int) = "seller/$id"
    }
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Search : Screen("search")
    object NewArrivals : Screen("new_arrivals")
    object Checkout : Screen("checkout")
    object Orders : Screen("orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Int) = "order_detail/$orderId"
    }
    object PublicProfile : Screen("user/{userId}") {
        fun createRoute(userId: Int) = "user/$userId"
    }
    object Messages : Screen("messages", "消息", Icons.Default.Chat)
    object Chat : Screen("chat/{partnerId}") {
        fun createRoute(partnerId: Int) = "chat/$partnerId"
    }
    object Friends : Screen("friends")
    object Settings : Screen("settings")

    companion object {
        val bottomTabs = listOf(Home, Catalog, Cart, Messages, Profile)

        /** routes that require authentication */
        val protectedRoutes = setOf(
            Cart.route, Profile.route, Checkout.route,
            Orders.route, OrderDetail.route, Messages.route,
            Chat.route, Friends.route, Settings.route
        )

        /** routes that only guests can access */
        val guestOnlyRoutes = setOf(Login.route, Register.route, ForgotPassword.route)
    }
}
