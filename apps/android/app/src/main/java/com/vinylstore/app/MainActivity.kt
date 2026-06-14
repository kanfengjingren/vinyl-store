package com.vinylstore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vinylstore.app.ui.navigation.AppNavigation
import com.vinylstore.app.ui.theme.VinylStoreTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VinylApp

        setContent {
            VinylStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        tokenStorage = app.tokenStorage,
                        albumRepository = app.albumRepository
                    )
                }
            }
        }
    }
}
