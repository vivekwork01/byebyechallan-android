package com.byebyechallan.app.data.repository

/**
 * Wraps the outcome of any repository call so screens can handle
 * loading/success/error states uniformly without try/catch everywhere.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
