package com.saveflow.downloader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saveflow.downloader.AppContainer
import com.saveflow.downloader.ui.downloads.DownloadsViewModel
import com.saveflow.downloader.ui.home.HomeViewModel
import com.saveflow.downloader.ui.settings.SettingsViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(container)
        DownloadsViewModel::class.java -> DownloadsViewModel(container)
        SettingsViewModel::class.java -> SettingsViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
