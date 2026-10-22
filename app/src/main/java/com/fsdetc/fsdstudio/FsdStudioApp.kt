package com.fsdetc.fsdstudio

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Application entry point. Owns the [AppContainer] (manual DI) and provides the
 * app-wide Coil [ImageLoader]: memory cache capped at 15% of the app's heap
 * budget and a 50 MB disk cache, backed by our OkHttp client (which itself
 * carries a 20 MB HTTP cache — render URLs are immutable, so both layers
 * mostly serve repeats for free).
 */
class FsdStudioApp : Application(), ImageLoaderFactory {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(container.skinRepository.httpClient)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
