package com.saveflow.downloader.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.ui.components.CustomTopBar
import com.saveflow.downloader.ui.components.DownloadButton
import com.saveflow.downloader.ui.components.EmptyStateView
import com.saveflow.downloader.ui.components.MediaGrid
import com.saveflow.downloader.ui.components.PlatformIcon
import com.saveflow.downloader.ui.components.QualitySelector
import com.saveflow.downloader.ui.components.VideoPlayer
import com.saveflow.downloader.utils.ClipboardUtils
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.Formatters
import com.saveflow.downloader.utils.rememberDownloadGate
import kotlinx.coroutines.launch

@Composable
fun PreviewScreen(
    vm: PreviewViewModel,
    onBack: () -> Unit,
    onViewDownloads: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val info = state.info

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    fun queue(assets: List<MediaAsset>) {
        scope.launch {
            val n = vm.download(assets)
            val result = snackbar.showSnackbar(
                if (n == 1) "Download started" else "$n downloads started",
                actionLabel = "View"
            )
            if (result == SnackbarResult.ActionPerformed) onViewDownloads()
        }
    }

    val gate = rememberDownloadGate(onStorageDenied = {
        FileActions.toast(context, "Storage permission is needed to save files on this Android version")
    })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CustomTopBar(title = "Preview", subtitle = info?.platform?.label, onBack = onBack) }
    ) { padding ->
        if (info == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(Icons.Rounded.BrokenImage, "Nothing to preview", "Go back and enter a link first.")
            }
            return@Scaffold
        }

        val items = info.items
        val selectedAssets = items.filter { it.id in state.selected }

        Column(Modifier.padding(padding).fillMaxSize()) {
            if (items.size == 1) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    SinglePreview(items.first())
                    Spacer(Modifier.height(16.dp))
                    Details(items.first(), info.platform, info.title)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${items.size} items • ${state.selected.size} selected",
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)
                    )
                    val allSelected = state.selected.size == items.size
                    TextButton(onClick = { vm.selectAll(!allSelected) }) {
                        Text(if (allSelected) "Clear" else "Select all")
                    }
                }
                MediaGrid(
                    items = items,
                    selectedIds = state.selected,
                    onToggle = vm::toggle,
                    onDownload = { asset -> gate { queue(listOf(asset)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quality", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                        QualitySelector(
                            options = info.availableQualities,
                            selected = info.quality,
                            onSelect = vm::setQuality,
                            loading = state.switchingQuality
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (items.size == 1) {
                        DownloadButton("Download", onClick = { gate { queue(items) } })
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { gate { queue(selectedAssets) } },
                                enabled = selectedAssets.isNotEmpty(),
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Selected (${selectedAssets.size})") }
                            DownloadButton(
                                "Download all",
                                onClick = { gate { queue(items) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { FileActions.shareText(context, info.sourceUrl) }) {
                            Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Share link")
                        }
                        TextButton(onClick = {
                            ClipboardUtils.copy(context, "Link", info.sourceUrl)
                            FileActions.toast(context, "Link copied")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Copy link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinglePreview(asset: MediaAsset) {
    var playing by rememberSaveable(asset.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val ratio = if (asset.width != null && asset.height != null && asset.height > 0)
        (asset.width.toFloat() / asset.height).coerceIn(0.6f, 1.8f) else 16f / 10f

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(shape)
            .background(Color.Black)
    ) {
        when {
            asset.type == MediaType.IMAGE -> AsyncImage(
                asset.downloadUrl, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
            )
            playing -> VideoPlayer(uri = asset.downloadUrl)
            else -> {
                asset.thumbnailUrl?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f))
                        .clickable { playing = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, "Play preview", tint = Color.Black, modifier = Modifier.size(40.dp))
                }
                Text(
                    "Tap to preview",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                )
            }
        }
    }
}

/** Shows only the facts the API actually returned. */
@Composable
private fun Details(asset: MediaAsset, platform: com.saveflow.downloader.domain.models.Platform, title: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlatformIcon(platform, size = 36.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title ?: if (asset.type == MediaType.VIDEO) "Video" else "Image", style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(platform.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val chips = buildList {
        add(if (asset.type == MediaType.VIDEO) "Video" else "Image")
        if (asset.width != null && asset.height != null) add("${asset.width}×${asset.height}")
        asset.durationMs?.let { add(Formatters.duration(it)) }
        asset.sizeBytes?.let { add(Formatters.bytes(it)) }
    }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}
