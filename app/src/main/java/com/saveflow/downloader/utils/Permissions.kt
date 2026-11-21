package com.saveflow.downloader.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a gate: gate { startDownloads() } asks only for the permissions that the running Android
 * version actually needs (storage on 9 and below, notifications on 13+) and then runs the action.
 * Notifications are optional; storage is required where it is requested.
 */
@Composable
fun rememberDownloadGate(onStorageDenied: () -> Unit): (() -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val action = pending
        pending = null
        val storageDenied = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            result[Manifest.permission.WRITE_EXTERNAL_STORAGE] == false
        if (storageDenied) onStorageDenied() else action?.invoke()
    }

    return { action ->
        val needed = buildList {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            action()
        } else {
            pending = action
            launcher.launch(missing.toTypedArray())
        }
    }
}
