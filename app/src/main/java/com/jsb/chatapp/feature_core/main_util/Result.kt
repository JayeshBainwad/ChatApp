package com.jsb.chatapp.feature_core.main_util

sealed class Result<out T> {
    data class Success<out T>(val data: T): Result<T>()
    data class Error(val exception: Throwable): Result<Nothing>()
}
