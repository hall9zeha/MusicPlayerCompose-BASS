package com.barryzeha.kmusic.common.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
        val bitmap =  try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

               context.contentResolver.loadThumbnail(
                    ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id),
                    Size(thumbnailSize, thumbnailSize),
                    null,
                )

            }else{
                decodeAlbumArtToLegacyVersions(context,ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id),sizeLimit?:256)
            }
        } catch (ex: Exception) {
            null
        }
        bitmap?.let { artworkCache.put(id, it) }
        bitmap
    }
private fun decodeAlbumArtToLegacyVersions(context:Context, uri: Uri?, sizeLimit: Int=256 ):Bitmap?{
 var bitmap:Bitmap? = null
    if(uri !=null){
        val retriever = MediaMetadataRetriever()
        try{
            retriever.setDataSource(context,uri)
            val picture = retriever.embeddedPicture
            if(picture !=null && picture.isNotEmpty()){
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(picture,0,picture.size,opts)
                opts.inSampleSize = calculateInSampleSize(opts,sizeLimit,sizeLimit)
                opts.inJustDecodeBounds = false
                bitmap = BitmapFactory.decodeByteArray(picture,0,picture.size,opts)
            }
        }catch(e:Exception){}
        finally {
            try{
                retriever.release()
            }catch(_:Exception){}
        }
    }
    return bitmap
}
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}