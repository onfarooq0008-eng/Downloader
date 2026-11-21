package com.saveflow.downloader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache

class SaveFlowApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Start the engine eagerly so downloads interrupted by a process kill are marked as paused.
        container.engine
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) } // thumbnails for saved videos
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
            .crossfade(true)
            .build()
}
