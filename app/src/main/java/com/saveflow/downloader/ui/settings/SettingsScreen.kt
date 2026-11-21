package com.saveflow.downloader.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saveflow.downloader.data.api.ApiConfig
import com.saveflow.downloader.domain.models.ThemeMode
import com.saveflow.downloader.ui.components.CustomDialog
import com.saveflow.downloader.ui.components.CustomTopBar
import com.saveflow.downloader.ui.components.QualitySelector
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.Formatters

private enum class Dialog { FOLDER, CLEAR_CACHE, CLEAR_HISTORY, PRIVACY, ABOUT }

@Composable
fun SettingsScreen(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val cache by vm.cacheBytes.collectAsStateWithLifecycle()
    val free by vm.freeBytes.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    Column(modifier.fillMaxSize()) {
        CustomTopBar(title = "Settings")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Downloads") {
                SettingRow(Icons.Rounded.HighQuality, "Default quality", "Used for new downloads") {
                    QualitySelector(ApiConfig.knownQualities, settings.quality, vm::setQuality)
                }
                SettingRow(
                    Icons.Rounded.Folder, "Download location",
                    "Movies/${settings.folder} and Pictures/${settings.folder}",
                    onClick = { dialog = Dialog.FOLDER }
                )
                SettingRow(Icons.Rounded.ContentPaste, "Clipboard detection", "Suggest copied links on the home screen") {
                    Switch(settings.clipboardDetection, vm::setClipboard)
                }
            }

            Section("Appearance") {
                SettingRow(Icons.Rounded.DarkMode, "Theme", null) {}
                Row(
                    Modifier.padding(start = 56.dp, bottom = 14.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        androidx.compose.material3.FilterChip(
                            selected = settings.theme == mode,
                            onClick = { vm.setTheme(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Section("Storage") {
                SettingRow(
                    Icons.Rounded.Storage, "Downloaded media",
                    "${stats.total} files • ${Formatters.bytes(stats.totalBytes)} • ${Formatters.bytes(free)} free on device"
                )
                SettingRow(
                    Icons.Rounded.CleaningServices, "Clear cache", Formatters.bytes(cache),
                    onClick = { dialog = Dialog.CLEAR_CACHE }
                )
                SettingRow(
                    Icons.Rounded.DeleteSweep, "Clear download history",
                    "Removes the list only; saved files stay in your gallery",
                    onClick = { dialog = Dialog.CLEAR_HISTORY }
                )
            }

            Section("About") {
                SettingRow(Icons.Rounded.PrivacyTip, "Privacy policy", null, onClick = {
                    if (ApiConfig.PRIVACY_POLICY_URL.isNotBlank()) FileActions.openUrl(context, ApiConfig.PRIVACY_POLICY_URL)
                    else dialog = Dialog.PRIVACY
                })
                SettingRow(Icons.Rounded.Info, "About SaveFlow", "Version ${vm.versionName}", onClick = { dialog = Dialog.ABOUT })
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    when (dialog) {
        Dialog.FOLDER -> {
            var text by remember { mutableStateOf(settings.folder) }
            CustomDialog(
                title = "Download folder",
                message = "Files are saved to Movies and Pictures inside this folder.",
                confirmText = "Save",
                onConfirm = { vm.setFolder(text); dialog = null },
                onDismiss = { dialog = null }
            ) {
                OutlinedTextField(text, { text = it }, singleLine = true, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Dialog.CLEAR_CACHE -> CustomDialog(
            title = "Clear cache?", message = "Temporary files (${Formatters.bytes(cache)}) will be deleted.",
            confirmText = "Clear", onConfirm = { vm.clearCache(); dialog = null }, onDismiss = { dialog = null }
        )
        Dialog.CLEAR_HISTORY -> CustomDialog(
            title = "Clear history?",
            message = "All entries will be removed from the list. Files already saved to your gallery are not deleted.",
            confirmText = "Clear", destructive = true,
            onConfirm = { vm.clearHistory(); dialog = null }, onDismiss = { dialog = null }
        )
        Dialog.PRIVACY -> CustomDialog(
            title = "Privacy",
            message = "SaveFlow stores your download history and settings only on this device. " +
                "When you download a link, that link and the chosen quality are sent to the downloader server " +
                "to fetch the media. The clipboard is read only to suggest a link and is never uploaded automatically.",
            onDismiss = { dialog = null }
        )
        Dialog.ABOUT -> CustomDialog(
            title = "SaveFlow",
            message = "Version ${vm.versionName}\nSave videos and photos from supported links. " +
                "Only download content you have the right to save.",
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) { Column(Modifier.fillMaxWidth()) { content() } }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}
