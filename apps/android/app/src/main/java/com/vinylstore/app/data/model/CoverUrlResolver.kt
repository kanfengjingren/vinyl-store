package com.vinylstore.app.data.model

import com.vinylstore.app.BuildConfig

private val IMG_BASE: String by lazy {
    BuildConfig.API_BASE_URL
        .replace("/api$".toRegex(), "")
        .trimEnd('/')
}

fun resolveCoverUrl(coverUrl: String?): String? {
    if (coverUrl.isNullOrBlank()) return null
    return if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
        coverUrl
    } else {
        "$IMG_BASE/${coverUrl.trimStart('/')}"
    }
}
