package com.saveflow.downloader.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * ExoPlayer is created only when this composable enters composition and released when it leaves,
 * so a preview costs no memory until the user taps play.
 */
@Composable
fun VideoPlayer(uri: String, modifier: Modifier = Modifier, autoPlay: Boolean = true) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = autoPlay
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
        update = { it.player = player }
    )
}
