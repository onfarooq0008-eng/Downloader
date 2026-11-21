package com.saveflow.downloader.data.api

import com.saveflow.downloader.domain.models.Platform

/**
 * Single place to adapt the app to your backend.
 *
 * VERIFIED contract (from the sample request): GET {BASE_URL}{DOWNLOAD_PATH}?url=<encoded>&quality=hd
 * answers with the media bytes directly (Content-Type: video/mp4). That case needs no field mapping.
 *
 * UNVERIFIED: if the API ever answers with JSON (e.g. several media items, thumbnails), the key lists
 * below tell the parser where to look. They are candidate names, not a documented contract — edit them
 * to match your real JSON. Unknown keys are simply ignored; nothing is invented.
 */
object ApiConfig {
    const val BASE_URL = "https://modern-rebekkah-usman62627-a7dc2c5c.apps.mistral.run/"
    const val DOWNLOAD_PATH = "api/download"
    const val PARAM_URL = "url"
    const val PARAM_QUALITY = "quality"

    /** Qualities the backend is known to accept. Only "hd" is confirmed. Add "sd"/"original" if your API supports them. */
    val knownQualities: List<String> = listOf("hd")
    const val DEFAULT_QUALITY = "hd"

    /** Platforms advertised on the home screen. Only Instagram is confirmed by the sample request. */
    val enabledPlatforms: List<Platform> = listOf(Platform.INSTAGRAM)

    /** Leave empty to show the built-in privacy summary; set a URL to open your hosted policy instead. */
    const val PRIVACY_POLICY_URL = ""

    // Timeouts (seconds)
    const val CONNECT_TIMEOUT = 15L
    const val RESOLVE_TIMEOUT = 60L   // whole resolve call, the backend may need time to process
    const val READ_TIMEOUT = 45L
    const val DOWNLOAD_READ_TIMEOUT = 30L

    // ---- JSON response mapping (only used when the response is not raw media) ----
    val itemListKeys = listOf("media", "medias", "items", "results", "data", "files")
    val wrapperKeys = listOf("data", "result", "response")
    val urlKeys = listOf("url", "download_url", "downloadUrl", "media_url", "video_url", "image_url", "src", "link")
    val thumbnailKeys = listOf("thumbnail", "thumbnail_url", "thumb", "cover", "preview", "poster")
    val titleKeys = listOf("title", "caption", "description", "name")
    val typeKeys = listOf("type", "media_type", "kind", "mime", "mime_type")
    val widthKeys = listOf("width")
    val heightKeys = listOf("height")
    val durationKeys = listOf("duration", "duration_ms")
    val sizeKeys = listOf("size", "file_size", "filesize", "content_length")
    val fileNameKeys = listOf("filename", "file_name")
    val qualityListKeys = listOf("qualities", "available_qualities")
    val errorKeys = listOf("error", "message", "detail")

    /** Set to false if the API reports duration in milliseconds. */
    const val DURATION_IS_SECONDS = true
}
