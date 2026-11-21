package com.saveflow.downloader.data.api

import android.webkit.MimeTypeMap
import com.saveflow.downloader.data.api.ApiException.Kind
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaInfo
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.Platform
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

/**
 * Turns the backend response into [MediaInfo].
 *  1. Raw media (a video, image or octet-stream content type): the API URL itself is the download URL.
 *  2. JSON: items are located via the key lists in [ApiConfig].
 */
class ResponseParser {

    fun parse(response: Response<ResponseBody>, sourceUrl: String, quality: String): MediaInfo {
        val platform = Platform.detect(sourceUrl)

        if (!response.isSuccessful) {
            val text = runCatching { response.errorBody()?.use { it.string() } }.getOrNull()
            val message = text?.let { extractError(it) } ?: "The server returned an error (HTTP ${response.code()})."
            throw ApiException(Kind.HTTP, message)
        }

        val body = response.body() ?: throw ApiException(Kind.EMPTY, "The server returned an empty response.")
        val mime = body.contentType()?.let { "${it.type}/${it.subtype}".lowercase() }

        body.use {
            val isDirect = mime != null &&
                (mime.startsWith("video/") || mime.startsWith("image/") || mime.endsWith("octet-stream"))
            return if (isDirect) {
                direct(response, body, mime!!, sourceUrl, platform, quality)
            } else {
                parseJson(it.string(), sourceUrl, platform, quality)
            }
        }
    }

    private fun direct(
        response: Response<ResponseBody>,
        body: ResponseBody,
        mime: String,
        sourceUrl: String,
        platform: Platform,
        quality: String
    ): MediaInfo {
        val fileName = response.headers()["Content-Disposition"]?.let(::fileNameFromDisposition)
        val extFromName = fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotEmpty() }
        val isImage = mime.startsWith("image/") ||
            (mime.endsWith("octet-stream") && extFromName in IMAGE_EXTENSIONS)
        val type = if (isImage) MediaType.IMAGE else MediaType.VIDEO
        val realMime = if (mime.endsWith("octet-stream")) {
            extFromName?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                ?: if (isImage) "image/jpeg" else "video/mp4"
        } else mime
        val ext = extFromName ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(realMime)
        val length = body.contentLength().takeIf { it > 0 }
        val url = ApiClient.buildDownloadUrl(sourceUrl, quality)

        val asset = MediaAsset(
            id = "0",
            type = type,
            downloadUrl = url,
            thumbnailUrl = null,
            mimeType = realMime,
            extension = ext,
            fileNameHint = fileName,
            width = null,
            height = null,
            durationMs = null,
            sizeBytes = length
        )
        return MediaInfo(sourceUrl, platform, null, listOf(asset), listOf(quality), quality)
    }

    private fun parseJson(text: String, sourceUrl: String, platform: Platform, quality: String): MediaInfo {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw ApiException(Kind.EMPTY, "The server returned an empty response.")

        val root: Any = try {
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
        } catch (e: JSONException) {
            throw ApiException(Kind.PARSE, "The server sent a response this app can't read.", e)
        }

        val objects = collectItems(root)
        val assets = objects.mapIndexedNotNull { index, o -> toAsset(o, index) }
        if (assets.isEmpty()) {
            val message = (root as? JSONObject)?.let { firstString(it, ApiConfig.errorKeys) }
                ?: "No downloadable media was found for this link."
            throw ApiException(Kind.EMPTY, message)
        }

        val rootObj = root as? JSONObject
        val title = rootObj?.let { firstString(it, ApiConfig.titleKeys) }
        val qualities = rootObj?.let { readQualities(it) }.orEmpty().ifEmpty { listOf(quality) }
        return MediaInfo(sourceUrl, platform, title, assets, qualities, quality)
    }

    private fun collectItems(node: Any?): List<JSONObject> = when (node) {
        is JSONArray -> (0 until node.length()).mapNotNull { i ->
            when (val v = node.opt(i)) {
                is JSONObject -> v
                is String -> JSONObject().put(ApiConfig.urlKeys.first(), v)
                else -> null
            }
        }
        is JSONObject -> {
            ApiConfig.itemListKeys.firstNotNullOfOrNull { key ->
                node.optJSONArray(key)?.takeIf { it.length() > 0 }?.let { collectItems(it) }
            } ?: ApiConfig.wrapperKeys.firstNotNullOfOrNull { key ->
                node.optJSONObject(key)?.let { collectItems(it) }?.takeIf { it.isNotEmpty() }
            } ?: if (firstString(node, ApiConfig.urlKeys) != null) listOf(node) else emptyList()
        }
        else -> emptyList()
    }

    private fun toAsset(o: JSONObject, index: Int): MediaAsset? {
        val url = firstString(o, ApiConfig.urlKeys) ?: return null
        val typeHint = firstString(o, ApiConfig.typeKeys)?.lowercase()
        val urlExt = url.substringBefore('?').substringAfterLast('.', "").lowercase()
        val type = when {
            typeHint != null && ("image" in typeHint || typeHint == "photo") -> MediaType.IMAGE
            typeHint != null && "video" in typeHint -> MediaType.VIDEO
            urlExt in IMAGE_EXTENSIONS -> MediaType.IMAGE
            else -> MediaType.VIDEO
        }
        val ext = urlExt.takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
            ?: if (type == MediaType.IMAGE) "jpg" else "mp4"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4"
        val duration = firstDouble(o, ApiConfig.durationKeys)?.let {
            if (ApiConfig.DURATION_IS_SECONDS) (it * 1000).toLong() else it.toLong()
        }
        return MediaAsset(
            id = index.toString(),
            type = type,
            downloadUrl = url,
            thumbnailUrl = firstString(o, ApiConfig.thumbnailKeys),
            mimeType = mime,
            extension = ext,
            fileNameHint = firstString(o, ApiConfig.fileNameKeys),
            width = firstDouble(o, ApiConfig.widthKeys)?.toInt(),
            height = firstDouble(o, ApiConfig.heightKeys)?.toInt(),
            durationMs = duration,
            sizeBytes = firstDouble(o, ApiConfig.sizeKeys)?.toLong()?.takeIf { it > 0 }
        )
    }

    private fun readQualities(o: JSONObject): List<String> =
        ApiConfig.qualityListKeys.firstNotNullOfOrNull { key ->
            o.optJSONArray(key)?.let { arr ->
                (0 until arr.length()).mapNotNull { i -> (arr.opt(i) as? String)?.takeIf { it.isNotBlank() } }
            }?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    private fun extractError(text: String): String? {
        val t = text.trim()
        if (t.startsWith("{")) {
            runCatching { JSONObject(t) }.getOrNull()?.let { o ->
                firstString(o, ApiConfig.errorKeys)?.let { return it }
            }
        }
        return t.takeIf { it.isNotEmpty() && it.length < 200 && !it.startsWith("<") }
    }

    private fun fileNameFromDisposition(header: String): String? {
        val star = Regex("filename\\*=(?:UTF-8'')?([^;]+)", RegexOption.IGNORE_CASE).find(header)?.groupValues?.get(1)
        val plain = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(header)?.groupValues?.get(1)
        return (star?.let { runCatching { java.net.URLDecoder.decode(it.trim(), "UTF-8") }.getOrNull() } ?: plain)
            ?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun firstString(o: JSONObject, keys: List<String>): String? =
        keys.firstNotNullOfOrNull { k ->
            if (o.isNull(k)) null else o.optString(k, "").takeIf { it.isNotBlank() && it != "null" }
        }

    private fun firstDouble(o: JSONObject, keys: List<String>): Double? =
        keys.firstNotNullOfOrNull { k ->
            if (!o.has(k) || o.isNull(k)) null else o.optDouble(k, Double.NaN).takeIf { !it.isNaN() }
        }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
    }
}
