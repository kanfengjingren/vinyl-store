package com.vinylstore.app.data.model

import com.vinylstore.app.BuildConfig

private val IMG_BASE by lazy {
    BuildConfig.API_BASE_URL
        .replace("/api$".toRegex(), "")
        .trimEnd('/')
}

fun resolveCoverUrl(coverUrl: String?): String? {
    if (coverUrl.isNullOrBlank()) return null
    if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) return coverUrl

    val path = coverUrl.trimStart('/')
    return "$IMG_BASE/$path"
}
