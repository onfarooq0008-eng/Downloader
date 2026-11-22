package com.saveflow.downloader.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saveflow.downloader.IncomingLink
import com.saveflow.downloader.SaveFlowApp
import com.saveflow.downloader.domain.models.AppSettings
import com.saveflow.downloader.ui.browser.BrowserScreen
import com.saveflow.downloader.ui.browser.BrowserState
import com.saveflow.downloader.ui.components.AnalyzingDialog
import com.saveflow.downloader.ui.components.CustomBottomNavigation
import com.saveflow.downloader.ui.components.NavItem
import com.saveflow.downloader.ui.downloads.DownloadsScreen
import com.saveflow.downloader.ui.downloads.DownloadsViewModel
import com.saveflow.downloader.ui.home.HomeEvent
import com.saveflow.downloader.ui.home.HomeScreen
import com.saveflow.downloader.ui.home.HomeViewModel
import com.saveflow.downloader.ui.settings.SettingsScreen
import com.saveflow.downloader.ui.settings.SettingsViewModel
import com.saveflow.downloader.ui.theme.SaveFlowTheme
import com.saveflow.downloader.ui.viewer.ViewerScreen
import com.saveflow.downloader.utils.FileActions
import com.saveflow.downloader.utils.rememberDownloadGate
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val BROWSER = "browser"
    const val SETTINGS = "settings"
    const val VIEWER = "viewer/{id}"
    fun viewer(id: Long) = "viewer/$id"
}

private val navItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    NavItem(Routes.DOWNLOADS, "Downloads", Icons.Outlined.Download, Icons.Rounded.Download),
    NavItem(Routes.BROWSER, "Browser", Icons.Outlined.Language, Icons.Rounded.Language),
    NavItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings)
)

@Composable
fun SaveFlowRoot() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SaveFlowApp).container }
    val factory = remember { ViewModelFactory(container) }
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

    SaveFlowTheme(settings.theme) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        // Activity-scoped so tab state (typed link, history filter, WebView page) is kept across tabs.
        val homeVm: HomeViewModel = viewModel(factory = factory)
        val downloadsVm: DownloadsViewModel = viewModel(factory = factory)
        val settingsVm: SettingsViewModel = viewModel(factory = factory)
        val homeState by homeVm.state.collectAsStateWithLifecycle()
        val browser = remember { BrowserState(context) }
        DisposableEffect(Unit) { onDispose { browser.destroy() } }

        fun goTab(target: String) {
            nav.navigate(target) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val gate = rememberDownloadGate(onStorageDenied = {
            FileActions.toast(context, "Storage permission is needed to save files on this Android version")
        })
        val currentGate by rememberUpdatedState(gate)

        LaunchedEffect(Unit) {
            homeVm.events.collect { event ->
                when (event) {
                    // Link resolved: ask for notification/storage permission if needed, then download.
                    is HomeEvent.StartDownload -> currentGate { homeVm.enqueue(event.info) }
                    is HomeEvent.Started -> scope.launch {
                        val result = snackbar.showSnackbar(
                            if (event.count == 1) "Download started" else "${event.count} downloads started",
                            actionLabel = "View"
                        )
                        if (result == SnackbarResult.ActionPerformed) goTab(Routes.DOWNLOADS)
                    }
                    is HomeEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
                }
            }
        }

        val showBar = route in navItems.map { it.route }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showBar) CustomBottomNavigation(navItems, route, onSelect = { if (it != route) goTab(it) })
            }
        ) { padding ->
            // Bottom padding only: each screen draws its own top bar / status-bar inset.
            val content = Modifier.padding(PaddingValues(bottom = padding.calculateBottomPadding()))
            NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize()) {
                composable(Routes.HOME) {
                    Box(content) {
                        HomeScreen(
                            vm = homeVm,
                            onSeeAll = { goTab(Routes.DOWNLOADS) },
                            onOpenRecord = { nav.navigate(Routes.viewer(it.id)) }
                        )
                    }
                }
                composable(Routes.DOWNLOADS) {
                    Box(content) {
                        DownloadsScreen(vm = downloadsVm, onOpenRecord = { nav.navigate(Routes.viewer(it.id)) })
                    }
                }
                composable(Routes.BROWSER) {
                    Box(content) {
                        BrowserScreen(
                            state = browser,
                            isActive = route == Routes.BROWSER,
                            onDownload = { pageUrl, directUrl ->
                                container.session.incomingLink.value = IncomingLink(pageUrl, directUrl)
                            }
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    Box(content) { SettingsScreen(vm = settingsVm) }
                }
                composable(
                    Routes.VIEWER,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    ViewerScreen(
                        id = entry.arguments?.getLong("id") ?: -1L,
                        repository = container.downloadRepository,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }

        // Shown over whatever screen is open (home, browser, or a freshly shared link).
        if (homeState.loading) AnalyzingDialog(onCancel = homeVm::cancel)
    }
}
