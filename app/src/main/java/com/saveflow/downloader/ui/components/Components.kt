package com.saveflow.downloader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.DownloadStatus
import com.saveflow.downloader.domain.models.MediaAsset
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.utils.Formatters

data class NavItem(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
fun CustomTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun CustomBottomNavigation(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val pill by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tween(220), label = "navPill"
                )
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(220), label = "navTint"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item.route) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(pill)
                            .padding(horizontal = 22.dp, vertical = 6.dp)
                    ) {
                        Icon(if (selected) item.selectedIcon else item.icon, contentDescription = item.label, tint = tint)
                    }
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun URLInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Paste a link here") },
        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
                }
                TextButton(onClick = onPaste, enabled = enabled) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun DownloadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Rounded.Download
) {
    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Icon(icon, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        Text(if (loading) "Analyzing…" else text, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
    }
}

@Composable
fun PlatformIcon(
    platform: Platform,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showLabel: Boolean = false
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f))
                .background(Color(platform.brand)),
            contentAlignment = Alignment.Center
        ) {
            Text(platform.short, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(platform.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MediaPreviewCard(
    asset: MediaAsset,
    selected: Boolean,
    onToggle: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
    ) {
        val model = asset.thumbnailUrl ?: asset.downloadUrl.takeIf { asset.type == MediaType.IMAGE }
        if (model != null) {
            AsyncImage(model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                Icons.Rounded.PlayCircle, contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) Box(Modifier.fillMaxSize().border(3.dp, MaterialTheme.colorScheme.primary, shape))
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (selected) "Selected" else "Not selected",
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp)
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (asset.type == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Image,
                contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)
            )
            val label = asset.sizeBytes?.let { Formatters.bytes(it) } ?: if (asset.type == MediaType.VIDEO) "Video" else "Image"
            Spacer(Modifier.width(4.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        FilledIconButton(
            onClick = onDownload,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp)
        ) { Icon(Icons.Rounded.Download, contentDescription = "Download", modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun MediaGrid(
    items: List<MediaAsset>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onDownload: (MediaAsset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { asset ->
            MediaPreviewCard(
                asset = asset,
                selected = asset.id in selectedIds,
                onToggle = { onToggle(asset.id) },
                onDownload = { onDownload(asset) }
            )
        }
    }
}

@Composable
fun DownloadProgressCard(
    record: DownloadRecord,
    speedBytesPerSecond: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val failed = record.status == DownloadStatus.FAILED
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (record.type == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Image,
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        statusLine(record, speedBytesPerSecond),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                when (record.status) {
                    DownloadStatus.PAUSED -> IconButton(onClick = onResume) { Icon(Icons.Rounded.PlayArrow, "Resume") }
                    DownloadStatus.FAILED -> IconButton(onClick = onRetry) { Icon(Icons.Rounded.Replay, "Retry") }
                    else -> IconButton(onClick = onPause) { Icon(Icons.Rounded.Pause, "Pause") }
                }
                IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, "Cancel") }
            }
            if (!failed) {
                Spacer(Modifier.height(12.dp))
                val barModifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                if (record.progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { record.progress },
                        modifier = barModifier,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else if (record.status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(modifier = barModifier, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

private fun statusLine(r: DownloadRecord, speed: Long): String = when (r.status) {
    DownloadStatus.QUEUED -> "Waiting in queue"
    DownloadStatus.DOWNLOADING -> buildList {
        if (r.progress >= 0f) add("${(r.progress * 100).toInt()}%")
        if (speed > 0) add(Formatters.speed(speed))
        add(if (r.totalBytes > 0) "${Formatters.bytes(r.downloadedBytes)} / ${Formatters.bytes(r.totalBytes)}" else Formatters.bytes(r.downloadedBytes))
    }.joinToString(" • ")
    DownloadStatus.PAUSED -> "Paused • " + Formatters.bytes(r.downloadedBytes) +
        if (r.totalBytes > 0) " of ${Formatters.bytes(r.totalBytes)}" else ""
    DownloadStatus.FAILED -> "Failed • ${r.error ?: "Unknown error"}"
    DownloadStatus.COMPLETED -> "Completed"
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingAnimation(modifier: Modifier = Modifier, label: String? = null) {
    val transition = rememberInfiniteTransition(label = "dots")
    Column(modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val scale by transition.animateFloat(
                    initialValue = 0.5f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(500), RepeatMode.Reverse, initialStartOffset = StartOffset(i * 160)
                    ),
                    label = "dot$i"
                )
                Box(
                    Modifier
                        .size((12 * scale).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f + 0.6f * scale))
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QualitySelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { q ->
            FilterChip(
                selected = q.equals(selected, ignoreCase = true),
                onClick = { onSelect(q) },
                enabled = !loading,
                label = { Text(Formatters.qualityLabel(q)) },
                shape = RoundedCornerShape(12.dp)
            )
        }
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

@Composable
fun CustomDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancel",
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                content?.invoke()
            }
        },
        confirmButton = {
            if (confirmText != null && onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text(
                        confirmText, fontWeight = FontWeight.SemiBold,
                        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK", fontWeight = FontWeight.SemiBold) }
            }
        },
        dismissButton = {
            if (confirmText != null) TextButton(onClick = onDismiss) { Text(dismissText) }
        }
    )
}

@Composable
fun MediaThumb(record: DownloadRecord, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val model = record.savedUri ?: record.thumbnailUrl
        if (model != null) {
            AsyncImage(model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                if (record.type == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Image,
                contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (record.type == MediaType.VIDEO && model != null) {
            Icon(
                Icons.Rounded.PlayCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}

@Composable
fun DownloadHistoryItem(
    record: DownloadRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaThumb(record, 60.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    Formatters.bytes(record.sizeOnDisk) + " • " + if (record.type == MediaType.VIDEO) "Video" else "Image",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    Formatters.date(record.completedAt ?: record.createdAt),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onMore != null) {
                IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordActionsSheet(
    record: DownloadRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                record.fileName, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SheetAction(Icons.AutoMirrored.Rounded.OpenInNew, "Open", MaterialTheme.colorScheme.onSurface) { onDismiss(); onOpen() }
            SheetAction(Icons.Rounded.Share, "Share", MaterialTheme.colorScheme.onSurface) { onDismiss(); onShare() }
            SheetAction(Icons.Rounded.Delete, "Delete", MaterialTheme.colorScheme.error) { onDismiss(); onDelete() }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
