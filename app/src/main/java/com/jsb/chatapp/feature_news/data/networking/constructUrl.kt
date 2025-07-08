package com.jsb.chatapp.feature_news.data.networking

import com.jsb.chatapp.BuildConfig //Unresolved reference: BuildConfig

fun constructUrl(url: String): String {
    return when {
        url.contains(BuildConfig.BASE_URL) -> url
        url.startsWith("/") -> BuildConfig.BASE_URL + url.drop(1)
        else -> BuildConfig.BASE_URL + url
    }
}