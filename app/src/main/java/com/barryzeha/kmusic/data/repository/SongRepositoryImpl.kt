package com.barryzeha.kmusic.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.barryzeha.kmusic.common.loadCache
import com.barryzeha.kmusic.common.saveCache
import com.barryzeha.kmusic.common.scanTracks
import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/****
 * Project KMusic
 * Created by Barry Zea H. on 29/09/26.
 * Copyright (c)  All rights reserved.
 ***/

class SongRepositoryImpl internal constructor(private val context: Context): SongRepository {

    companion object{
        @Volatile
        private var instance: SongRepositoryImpl?=null

        fun getInstance(context: Context): SongRepository{
            val appContext = context.applicationContext
            return instance?:synchronized(this){
                instance ?: SongRepositoryImpl(appContext).also {
                    instance = it
                }
            }
        }
    }

    private var _songs: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    override val songs: StateFlow<List<SongEntity>> get() = _songs

    override suspend fun loadSongs(){
        val readCache = loadCache(context)
        if(readCache !=null && readCache.isNotEmpty()){
            _songs.value = readCache
            Log.e("LOAD_SONG_CACHED", readCache.toString() )
        } else {
            val scanSongs = scanTracks(context) ?: emptyList()
            _songs.value = scanSongs
            Log.e("LOAD_SONG_SCAN", scanSongs.toString())
            saveCache(context, scanSongs)
        }
    }
}
