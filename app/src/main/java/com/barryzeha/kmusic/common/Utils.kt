package com.barryzeha.kmusic.common

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.LruCache
import android.util.Size
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.barryzeha.kmusic.data.SongEntity
import com.barryzeha.kmusic.data.SongLibrary
import com.barryzeha.kmusic.data.SongSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FilenameUtils
import java.io.File
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
suspend fun loadCache(context: Context): List<SongEntity>? = withContext(Dispatchers.IO){
    val readCache = readJson(context,SONGS_FILE)?:return@withContext null
    val library = Json.decodeFromString<SongLibrary>(readCache)
    return@withContext library.songs
}
suspend fun saveCache(context: Context, songs: List<SongEntity>) = withContext(Dispatchers.IO){
    val songLibrary = Json.encodeToString<SongLibrary>(SongLibrary(songs))
    context.openFileOutput(SONGS_FILE, Context.MODE_PRIVATE).use {
        it.write(songLibrary.toByteArray())
    }
}
fun saveSongState(context:Context, song: SongEntity){
    val songStateJson = Json.encodeToString(song)
    context.openFileOutput(SONG_STATE_FILE,Context. MODE_PRIVATE ).use{
        it.write(songStateJson.toByteArray())
    }
}
suspend fun loadSongState(context:Context): SongEntity?{
    val readSongState = readJson(context,SONG_STATE_FILE)?:return null
    val songState = Json.decodeFromString<SongEntity>(readSongState)
    return songState
}
private suspend fun readJson(context:Context, fileName:String):String? = withContext(Dispatchers.IO){
    return@withContext try {
        val file = File(context.filesDir, fileName)
        if(!file.exists()){
            null
        }else{
            file.readText()
        }
    }catch(ex:Exception){
        ex.printStackTrace()
        null
    }
}
private val contentResolverColumns =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
    } else {
        arrayOf(
            Media._ID,
            Media.DATA,
            Media.DATE_ADDED,
            Media.DATE_MODIFIED,
            Media.TITLE,
            Media.ARTIST,
            Media.ALBUM,
            Media.ALBUM_ARTIST,
            Media.YEAR,
            Media.TRACK,
            Media.DURATION,
            Media.SIZE,
            Media.ALBUM_ID
        )
    }


fun scanTracks(context: Context): List<SongEntity>?{
    if(ContextCompat.checkSelfPermission(context,READ_PERMISSION) == PackageManager.PERMISSION_DENIED) return null

    val libraryVersion = MediaStore.getVersion(context)
    val query = context.contentResolver.query(
        Media.EXTERNAL_CONTENT_URI,
        contentResolverColumns,
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){"${Media.IS_MUSIC} AND NOT ${Media.IS_DRM} AND NOT ${Media.IS_TRASHED}"}else{"${Media.IS_MUSIC} AND NOT ${Media.IS_DRM}"},
        null,
        "${Media._ID} ASC"
    )
    val tracks = mutableListOf<SongEntity>()
    query?.use{cursor->
        val ci = contentResolverColumns.associateWith {cursor.getColumnIndexOrThrow(it)  }
        while(cursor.moveToNext()){
            val id=cursor.getLong(ci[Media._ID]!!)
            val trackVersion = cursor.getLong(ci[Media.DATE_MODIFIED]!!)
            var genre:String? = ""
            val path = cursor.getString(ci[Media.DATA]!!)
                .trimAndNormalize()
                .let{FilenameUtils.normalize(it)}
                .let{ FilenameUtils.separatorsToUnix(it) }
            val fileName = FilenameUtils.getName(path)
            var title = cursor.getStringOrNull(ci[Media.TITLE]!!)?.trimAndNormalize()
            var artist = cursor.getStringOrNull(ci[Media.ARTIST]!!)?.trimAndNormalize()
            var album = cursor.getStringOrNull(ci[Media.ALBUM]!!)?.trimAndNormalize()
            var albumArtist = cursor.getStringOrNull(ci[Media.ALBUM_ARTIST]!!)?.trimAndNormalize()
            genre = cursor.getStringOrNull(ci[Media.GENRE]!!)?.trimAndNormalize()
            var year = cursor.getIntOrNull(ci[Media.YEAR]!!)
            var duration = cursor.getIntOrNull(ci[Media.DURATION]!!)
            var bitrate = try{cursor.getLongOrNull(ci[Media.BITRATE]!!)}catch(e:Exception){-1}
            var size = cursor.getLongOrNull(ci[Media.SIZE]!!)
            var albumId = try{cursor.getLongOrNull(ci[Media.ALBUM_ID]!!)}catch(e:Exception){ -1}
            var dateAdded = cursor.getLongOrNull(ci[Media.DATE_ADDED]!!)
            var dateModified = cursor.getLongOrNull(ci[Media.DATE_MODIFIED]!!)

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
fun countSongs(context: Context):Int{
    val query = context.contentResolver.query(
        Media.EXTERNAL_CONTENT_URI,
        arrayOf(
            Media._ID,
            Media.DATE_ADDED,
            Media.DATE_MODIFIED),
        "${Media.IS_MUSIC} AND NOT ${Media.IS_DRM} AND NOT ${Media.IS_TRASHED}",
        null,
        null
    )
    val count = query?.count?:0
    query?.close()
    return count
}
fun librarySignature(context:Context): SongSignature?{
    return try{
        val query = context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                Media._ID,
                Media.DATE_ADDED,
                Media.DATE_MODIFIED
            ),
            "${Media.IS_MUSIC} AND NOT ${Media.IS_DRM} AND NOT ${Media.IS_TRASHED}",
            null,
            Media._ID + " DESC"

        )
        query?.use{
            if(!it.moveToFirst()){
                return null
            }
            val id = it.getColumnIndex(Media._ID)
            val dateAdded = it.getColumnIndex(Media.DATE_ADDED)
            val dateModified = it.getColumnIndexOrThrow(Media.DATE_MODIFIED)
            SongSignature(
                id=if(id>=0) it.getLong(id) else 0L,
                dateAdded = if(dateAdded >=0) it.getLong(dateAdded) else 0L,
                dateModified = if(dateModified >=0) it.getLong(dateModified) else 0L,
                songCount = it.count
            )
        }
    }
    catch(e:Exception){
        e.printStackTrace()
        null
    }
}
fun serializeSignature(signature: SongSignature):String{
    val signatureJson = Json.encodeToString(signature)
    return signatureJson
}
fun deserializeSignature(signature:String): SongSignature{
    val deserializeSign = Json.decodeFromString<SongSignature>(signature)
    return deserializeSign
}

fun formatCurrentDuration(value: Long):String{
    val minutes = TimeUnit.MILLISECONDS.toMinutes(value)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(value) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d",minutes, seconds)
}