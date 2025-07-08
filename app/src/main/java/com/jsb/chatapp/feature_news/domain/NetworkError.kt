package com.jsb.chatapp.feature_news.domain

import com.jsb.chatapp.feature_news.domain.Error

enum class NetworkError: Error {
    REQUEST_TIMEOUT,
    TOO_MANY_REQUESTS,
    NO_INTERNET,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN,
}