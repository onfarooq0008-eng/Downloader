package com.saveflow.downloader.data.api

class ApiException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    enum class Kind { INVALID, NETWORK, TIMEOUT, HTTP, PARSE, EMPTY }
}
