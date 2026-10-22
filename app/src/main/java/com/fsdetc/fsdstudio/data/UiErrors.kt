package com.fsdetc.fsdstudio.data

import androidx.annotation.StringRes
import com.fsdetc.fsdstudio.R
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Typed HTTP failure so the UI can map status codes to human messages. */
class HttpException(val code: Int) : IOException("HTTP $code")

/** The small set of error kinds the UI knows how to talk about. */
enum class ErrorKind(@StringRes val messageRes: Int) {
    OFFLINE(R.string.error_offline),
    TIMEOUT(R.string.error_timeout),
    SERVER_BUSY(R.string.error_server_busy),
    NOT_FOUND(R.string.error_player_not_found),
    GENERIC(R.string.error_generic),
}

/** Map any failure to a human, localised message kind. Never show raw exception text. */
fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is PlayerNotFoundException -> ErrorKind.NOT_FOUND
    is HttpException -> when {
        code == 429 || code in 500..599 -> ErrorKind.SERVER_BUSY
        code == 404 -> ErrorKind.NOT_FOUND
        else -> ErrorKind.GENERIC
    }
    is SocketTimeoutException -> ErrorKind.TIMEOUT
    is UnknownHostException, is ConnectException -> ErrorKind.OFFLINE
    else -> ErrorKind.GENERIC
}
