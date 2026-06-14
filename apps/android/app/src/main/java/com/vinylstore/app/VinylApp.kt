package com.vinylstore.app

import android.app.Application
import com.vinylstore.app.data.repository.AlbumRepository
import com.vinylstore.app.data.repository.AuthRepository
import com.vinylstore.app.local.TokenStorage

class VinylApp : Application() {

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var albumRepository: AlbumRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenStorage = TokenStorage(this)
        albumRepository = AlbumRepository(tokenStorage)
        authRepository = AuthRepository(tokenStorage)
    }
}
