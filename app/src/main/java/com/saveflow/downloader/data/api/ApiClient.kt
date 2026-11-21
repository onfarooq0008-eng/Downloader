package com.saveflow.downloader.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    /** Client for the resolve call (has an overall call timeout). */
    val resolveClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(ApiConfig.RESOLVE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Client for file transfers: no overall call timeout, only a per-read stall timeout. */
    val downloadClient: OkHttpClient by lazy {
        resolveClient.newBuilder()
            .callTimeout(0, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.DOWNLOAD_READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    val api: DownloaderApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(resolveClient)
            .build()
            .create(DownloaderApi::class.java)
    }

    /** Fully encoded API URL for a source link; used when the API itself streams the media. */
    fun buildDownloadUrl(sourceUrl: String, quality: String): String =
        ApiConfig.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(ApiConfig.DOWNLOAD_PATH)
            .addQueryParameter(ApiConfig.PARAM_URL, sourceUrl)
            .addQueryParameter(ApiConfig.PARAM_QUALITY, quality)
            .build()
            .toString()
}
