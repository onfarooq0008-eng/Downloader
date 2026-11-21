package com.saveflow.downloader.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.DownloadStats
import com.saveflow.downloader.domain.models.MediaType
import com.saveflow.downloader.ui.components.CustomDialog
import com.saveflow.downloader.ui.components.DownloadHistoryItem
import com.saveflow.downloader.ui.components.EmptyStateView
import com.saveflow.downloader.ui.components.MediaThumb
import com.saveflow.downloader.ui.components.RecordActionsSheet
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.Formatters

private enum class HistoryFilter(val label: String) { ALL("All"), VIDEOS("Videos"), IMAGES("Images") }

@Composable
fun HistoryContent(
    records: List<DownloadRecord>,
    stats: DownloadStats,
    onOpen: (DownloadRecord) -> Unit,
    onDelete: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<DownloadRecord?>(null) }
    var deleteFor by remember { mutableStateOf<DownloadRecord?>(null) }

    val visible = when (filter) {
        HistoryFilter.ALL -> records
        HistoryFilter.VIDEOS -> records.filter { it.type == MediaType.VIDEO }
        HistoryFilter.IMAGES -> records.filter { it.type == MediaType.IMAGE }
    }

    Column(modifier.fillMaxSize()) {
        if (records.isNotEmpty()) {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${stats.total}", "Files")
                    Stat(Formatters.bytes(stats.totalBytes), "Used")
                    Stat("${stats.videos}", "Videos")
                    Stat("${stats.images}", "Images")
                }
            }
            Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                    }
                }
                IconButton(onClick = { grid = !grid }) {
                    Icon(if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView, contentDescription = "Toggle layout")
                }
            }
        }

        when {
            records.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    Icons.Rounded.History, "No history yet",
                    "Files you download will be listed here and stay available after you close the app."
                )
            }
            visible.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(Icons.Rounded.History, "Nothing in this filter", "Try another filter.")
            }
            grid -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible, key = { it.id }) { r ->
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).clickable { onOpen(r) }) {
                        androidx.compose.foundation.layout.BoxWithConstraints {
                            MediaThumb(r, maxWidth)
                        }
                    }
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { r ->
                    DownloadHistoryItem(record = r, onClick = { onOpen(r) }, onMore = { menuFor = r })
                }
            }
        }
    }

    menuFor?.let { r ->
        RecordActionsSheet(
            record = r,
            onOpen = { FileActions.open(context, r.savedUri, r.mimeType) },
            onShare = { FileActions.share(context, r.savedUri, r.mimeType) },
            onDelete = { deleteFor = r },
            onDismiss = { menuFor = null }
        )
    }
    deleteFor?.let { r ->
        CustomDialog(
            title = "Delete file?",
            message = "\"${r.fileName}\" will be removed from your gallery and from this list.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { onDelete(r); deleteFor = null },
            onDismiss = { deleteFor = null }
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
