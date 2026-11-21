package com.saveflow.downloader.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface DownloaderApi {
    /**
     * @Streaming: the backend may answer with the media bytes themselves. We only need the headers to
     * identify the media, so the body must not be buffered into memory.
     */
    @Streaming
    @GET(ApiConfig.DOWNLOAD_PATH)
    suspend fun download(
        @Query(ApiConfig.PARAM_URL) url: String,
        @Query(ApiConfig.PARAM_QUALITY) quality: String
    ): Response<ResponseBody>
}
