package com.saveflow.downloader.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saveflow.downloader.data.repository.DownloadRepository
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.ui.components.CustomDialog
import com.saveflow.downloader.ui.components.CustomTopBar
import com.saveflow.downloader.ui.components.VideoPlayer
import com.saveflow.downloader.utils.FileActions
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/** Full-screen viewer for a finished download: pinch-zoom image or ExoPlayer video. */
@Composable
fun ViewerScreen(
    id: Long,
    repository: DownloadRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val flow = remember(id) { repository.observe(id) }
    val record by flow.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val r = record
        if (r?.savedUri != null) {
            if (r.type == MediaType.IMAGE) ZoomableImage(r.savedUri) else VideoPlayer(uri = r.savedUri)
        }
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            CustomTopBar(
                title = r?.fileName ?: "",
                onBack = onBack,
                actions = {
                    if (r != null) {
                        IconButton(onClick = { FileActions.open(context, r.savedUri, r.mimeType) }) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open with")
                        }
                        IconButton(onClick = { FileActions.share(context, r.savedUri, r.mimeType) }) {
                            Icon(Icons.Rounded.Share, "Share")
                        }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
                }
            )
        }
    }

    if (confirmDelete && record != null) {
        CustomDialog(
            title = "Delete file?",
            message = "It will be removed from your gallery and history.",
            confirmText = "Delete", destructive = true,
            onConfirm = {
                val r = record ?: return@CustomDialog
                confirmDelete = false
                scope.launch { repository.deleteCompleted(r); onBack() }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun ZoomableImage(model: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) { offsetX = 0f; offsetY = 0f } else { offsetX += pan.x; offsetY += pan.y }
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
    )
}
