package com.vinylstore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vinylstore.app.ui.navigation.AppNavigation
import com.vinylstore.app.ui.theme.VinylStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinylStoreTheme {
                AppNavigation()
            }
        }
    }
}
