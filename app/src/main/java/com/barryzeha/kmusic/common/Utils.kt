package com.barryzeha.kmusic.common

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.Normalizer2
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.barryzeha.kmusic.data.SongEntity
import org.apache.commons.io.FilenameUtils
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.String
import kotlin.math.min

/****
 * Project KMusic
 * Created by Barry Zea H. on 20/04/25.
 * Copyright (c)  All rights reserved.
 ***/

fun checkPermissions(context:Context, permissionsList: List<String>, granted:(Boolean, List<Pair<String,Boolean>>)-> Unit){
    val permissionsGranted: MutableList<Pair<String, Boolean>> = mutableListOf()
    var grantedCount =0

    permissionsList.forEach {permission->
        if(ContextCompat.checkSelfPermission(context,permission) == PackageManager.PERMISSION_GRANTED){
            permissionsGranted.add(Pair(permission, true))
            grantedCount++
        }else{
            permissionsGranted.add(Pair(permission,false))
            granted((grantedCount == permissionsList.size), permissionsGranted)
            return
        }
    }
    granted((grantedCount == permissionsList.size), permissionsGranted)

}
private val contentResolverColumns =
    arrayOf(
        Media._ID,
        Media.DATA,
        Media.DATE_ADDED,
        Media.DATE_MODIFIED,
        Media.TITLE,
        Media.ARTIST,
        Media.ALBUM,
        Media.ALBUM_ARTIST,
        Media.GENRE,
        Media.YEAR,
        Media.TRACK,
        Media.DISC_NUMBER,
        Media.DURATION,
        Media.SIZE,
        Media.BITRATE,
        Media.ALBUM_ID
    )
fun scanTracks(context: Context): List<SongEntity>?{
    if(ContextCompat.checkSelfPermission(context,READ_PERMISSION) == PackageManager.PERMISSION_DENIED) return null

    val libraryVersion = MediaStore.getVersion(context)
    val query = context.contentResolver.query(
        Media.EXTERNAL_CONTENT_URI,
        contentResolverColumns,
        "${Media.IS_MUSIC} AND NOT ${Media.IS_DRM} AND NOT ${Media.IS_TRASHED}",
        null,
        "${Media._ID} ASC"
    )
    val tracks = mutableListOf<SongEntity>()
    query?.use{cursor->
        val ci = contentResolverColumns.associateWith {cursor.getColumnIndexOrThrow(it)  }
        while(cursor.moveToNext()){
            val id=cursor.getLong(ci[Media._ID]!!)
            val trackVersion = cursor.getLong(ci[Media.DATE_MODIFIED]!!)

            val path = cursor.getString(ci[Media.DATA]!!)
                .trimAndNormalize()
                .let{FilenameUtils.normalize(it)}
                .let{ FilenameUtils.separatorsToUnix(it) }
            val fileName = FilenameUtils.getName(path)
            var title = cursor.getStringOrNull(ci[Media.TITLE]!!)?.trimAndNormalize()
            var artist = cursor.getStringOrNull(ci[Media.ARTIST]!!)?.trimAndNormalize()
            var album = cursor.getStringOrNull(ci[Media.ALBUM]!!)?.trimAndNormalize()
            var albumArtist = cursor.getStringOrNull(ci[Media.ALBUM_ARTIST]!!)?.trimAndNormalize()
            var genre = cursor.getStringOrNull(ci[Media.GENRE]!!)?.trimAndNormalize()
            var year = cursor.getIntOrNull(ci[Media.YEAR]!!)
            var duration = cursor.getIntOrNull(ci[Media.DURATION]!!)
            var bitrate = cursor.getLongOrNull(ci[Media.BITRATE]!!)
            var size = cursor.getLongOrNull(ci[Media.SIZE]!!)
            var albumId = try{cursor.getLongOrNull(ci[Media.ALBUM_ID]!!)}catch(e:Exception){ -1}

            title = title?.takeIf { it.isNotEmpty() }?.trimAndNormalize()
            artist = artist?.takeIf { it.isNotEmpty() }?.trimAndNormalize()
            album = album?.takeIf { it.isNotEmpty() }?.trimAndNormalize()
            albumArtist = albumArtist?.takeIf { it.isNotEmpty() }?.trimAndNormalize()
            genre = genre?.takeIf { it.isNotEmpty() }?.trimAndNormalize()

            val song = SongEntity(
                idSong = id,
                title = title.toString(),
                artist = artist.toString(),
                album = album.toString(),
                albumArtist = albumArtist.toString(),
                genre = genre.toString(),
                year = year.toString(),
                duration = duration?.toLong()?:0,
                bitrate = bitrate?:0,
                pathFile = path,
                albumId=albumId!!,
                size = size!!

            )
            tracks.add(song)
        }

    }
    return tracks
}
private val cachedScreenSize = AtomicInteger(0)
@RequiresApi(Build.VERSION_CODES.R)
fun loadArtwork(context: Context, id: Long, sizeLimit: Int? = null): Bitmap? {
    try {
        val thumbnailSize = sizeLimit
            ?: cachedScreenSize.get().takeIf { it > 0 }
            ?: run {
                val screenSize = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
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
        return bitmap
    } catch (ex: Exception) {
        return null
    }
}

fun formatCurrentDuration(value: Long):String{
    val minutes = TimeUnit.MILLISECONDS.toMinutes(value)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(value) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d",minutes, seconds)
}