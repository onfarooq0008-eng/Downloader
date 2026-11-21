package com.saveflow.downloader.data.api

import com.saveflow.downloader.data.api.ApiException.Kind
import com.saveflow.downloader.domain.models.MediaInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException

/** The only class that talks to the backend. Swap or edit it (plus ApiConfig) to change backends. */
class DownloadApiService(
    private val api: DownloaderApi = ApiClient.api,
    private val parser: ResponseParser = ResponseParser()
) {
    suspend fun resolve(sourceUrl: String, quality: String): MediaInfo = withContext(Dispatchers.IO) {
        try {
            val response = api.download(sourceUrl, quality)
            parser.parse(response, sourceUrl, quality)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: InterruptedIOException) {
            // Includes SocketTimeoutException and OkHttp call timeouts.
            throw ApiException(Kind.TIMEOUT, "The server took too long to respond. Please try again.", e)
        } catch (e: IOException) {
            throw ApiException(Kind.NETWORK, "Can't reach the server. Check your internet connection.", e)
        } catch (e: Exception) {
            throw ApiException(Kind.PARSE, "Something went wrong while reading the response.", e)
        }
    }
}
