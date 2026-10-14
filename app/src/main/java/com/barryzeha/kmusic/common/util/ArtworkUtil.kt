package com.barryzeha.kmusic.common.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore.Audio.Media
import android.util.LruCache
import android.util.Size
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.get
import kotlin.math.min

/****
 * Project KMusic
 * Created by Barry Zea H. on 14/10/26.
 * Copyright (c)  All rights reserved.
 ***/

private val maxMemoryKb = (Runtime.getRuntime().maxMemory()/1024).toInt()
private val cacheSizeKb = (maxMemoryKb /16).coerceAtLeast(2048)
private val artworkCache = object:LruCache<Long, Bitmap>(cacheSizeKb){
    override fun sizeOf(key: Long?, value: Bitmap): Int {
        return value.byteCount /1024
    }
}

fun getCachedArtwork(key: Long): Bitmap? = synchronized(artworkCache){
    artworkCache.get(key)
}
private val cachedScreenSize = AtomicInteger(0)

@RequiresApi(Build.VERSION_CODES.R)
suspend fun loadArtwork(context: Context, id: Long, sizeLimit: Int? = null): Bitmap? =
    withContext(Dispatchers.IO) {
        artworkCache.get(id)?.let{return@withContext it}
        try {
            val thumbnailSize = sizeLimit
                ?: cachedScreenSize.get().takeIf { it > 0 }
                ?: run {
                    val screenSize =
                        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                            .maximumWindowMetrics
                            .bounds
                    val limit = min(screenSize.width(), screenSize.height()).coerceAtLeast(256)
                    cachedScreenSize.set(limit)
                    limit
                }

            val bitmap = context.contentResolver.loadThumbnail(
                ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id),
                Size(thumbnailSize, thumbnailSize),
                null,
            )
            bitmap?.let{artworkCache.put(id,it)}
            bitmap
        } catch (ex: Exception) {
            null
        }
    }
