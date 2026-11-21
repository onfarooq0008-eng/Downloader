package com.saveflow.downloader.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.ui.components.CustomTopBar
import com.saveflow.downloader.ui.components.DownloadProgressCard
import com.saveflow.downloader.ui.components.EmptyStateView
import com.saveflow.downloader.ui.history.HistoryContent

@Composable
fun DownloadsScreen(
    vm: DownloadsViewModel,
    onOpenRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val active by vm.active.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    val speeds by vm.speeds.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize()) {
        CustomTopBar(title = "Downloads")
        SegmentedTabs(
            labels = listOf(if (active.isEmpty()) "Active" else "Active (${active.size})", "History"),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        if (tab == 0) {
            if (active.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        icon = Icons.Rounded.CloudDownload,
                        title = "No active downloads",
                        message = "Downloads in progress, paused or failed appear here."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    items(active, key = { it.id }) { record ->
                        DownloadProgressCard(
                            record = record,
                            speedBytesPerSecond = speeds[record.id] ?: 0L,
                            onPause = { vm.pause(record.id) },
                            onResume = { vm.resume(record.id) },
                            onCancel = { vm.cancel(record.id) },
                            onRetry = { vm.retry(record.id) }
                        )
                    }
                }
            }
        } else {
            HistoryContent(
                records = completed,
                stats = stats,
                onOpen = onOpenRecord,
                onDelete = vm::delete
            )
        }
    }
}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
