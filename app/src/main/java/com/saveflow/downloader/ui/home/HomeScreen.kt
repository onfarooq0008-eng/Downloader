package com.saveflow.downloader.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saveflow.downloader.data.api.ApiConfig
import com.saveflow.downloader.domain.models.DownloadRecord
import com.saveflow.downloader.domain.models.Platform
import com.saveflow.downloader.ui.components.DownloadButton
import com.saveflow.downloader.ui.components.DownloadHistoryItem
import com.saveflow.downloader.ui.components.EmptyStateView
import com.saveflow.downloader.ui.components.LoadingAnimation
import com.saveflow.downloader.ui.components.PlatformIcon
import com.saveflow.downloader.ui.components.URLInputField
import com.saveflow.downloader.utils.ClipboardUtils
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.UrlUtils

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onSeeAll: () -> Unit,
    onOpenRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()

    // Android 10+ only lets the focused app read the clipboard, so wait for window focus.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) {
        if (focused) vm.checkClipboard(ClipboardUtils.readText(context))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        item { Header() }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    URLInputField(
                        value = state.input,
                        onValueChange = vm::onInput,
                        onPaste = { vm.pasteFromClipboard(ClipboardUtils.readText(context)) },
                        onClear = vm::clear,
                        onSubmit = { if (state.input.isNotBlank()) vm.analyze() },
                        isError = state.error != null,
                        enabled = !state.loading
                    )
                    AnimatedVisibility(visible = state.error != null) {
                        Text(
                            state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    DownloadButton(
                        text = "Download",
                        onClick = vm::analyze,
                        loading = state.loading,
                        enabled = state.input.isNotBlank()
                    )
                    AnimatedVisibility(visible = state.loading) {
                        LoadingAnimation(label = "Contacting server… this can take a few seconds")
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = state.clipSuggestion != null) {
                state.clipSuggestion?.let { url ->
                    Surface(
                        modifier = Modifier.padding(top = 14.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = vm::acceptSuggestion
                    ) {
                        Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentPaste, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Link copied — tap to paste", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    UrlUtils.displayHost(url) + "  •  " + url.removePrefix("https://").removePrefix("http://"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = vm::dismissSuggestion) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        item { SupportedPlatforms(ApiConfig.enabledPlatforms) }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent downloads", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (recent.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
            }
        }

        if (recent.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    EmptyStateView(
                        icon = Icons.Rounded.Inbox,
                        title = "Nothing downloaded yet",
                        message = "Paste a link above and your saved videos and photos will show up here."
                    )
                }
            }
        } else {
            items(recent, key = { it.id }) { record ->
                DownloadHistoryItem(
                    record = record,
                    onClick = { onOpenRecord(record) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowDownward, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("SaveFlow", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Save videos and photos from links",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SupportedPlatforms(platforms: List<Platform>) {
    if (platforms.isEmpty()) return
    Column(Modifier.padding(top = 24.dp)) {
        Text("Supported platforms", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            platforms.forEach { PlatformIcon(it, size = 48.dp, showLabel = true) }
        }
    }
}
