package com.barryzeha.kmusic.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.barryzeha.kmusic.common.MyPreferences
import com.barryzeha.kmusic.common.librarySignature
import com.barryzeha.kmusic.common.loadCache
import com.barryzeha.kmusic.common.saveCache
import com.barryzeha.kmusic.common.scanTracks
import com.barryzeha.kmusic.common.serializeSignature
import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/****
 * Project KMusic
 * Created by Barry Zea H. on 29/09/26.
 * Copyright (c)  All rights reserved.
 ***/

class SongRepositoryImpl internal constructor(private val context: Context, private val prefs: MyPreferences): SongRepository {

    companion object{
        @Volatile
        private var instance: SongRepositoryImpl?=null

        fun getInstance(context: Context): SongRepository{
            val appContext = context.applicationContext
            val prefs = MyPreferences(appContext)
            return instance?:synchronized(this){
                instance ?: SongRepositoryImpl(appContext, prefs).also {
                    instance = it
                }
            }
        }
    }

    private var _songs: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    override val songs: StateFlow<List<SongEntity>> get() = _songs

    override suspend fun loadSongs(){
        val readCache = loadCache(context)
        if(!readCache.isNullOrEmpty()){
            _songs.value = readCache
        } else {
            val scanSongs = scanTracks(context) ?: emptyList()
            _songs.value = scanSongs
            val signature=librarySignature(context)
            signature?.let{sign-> prefs.librarySign = serializeSignature(sign)}
            saveCache(context, scanSongs)
        }
        syncLibraryTracks()
    }

    override suspend fun syncLibraryTracks() = withContext(Dispatchers.IO){
        val currentLibrarySignature = librarySignature(context)
        val previousSignature = prefs.librarySign
        val currentSignature = currentLibrarySignature?.let{serializeSignature(it)}
        if(previousSignature.isNotEmpty() && currentSignature !=null){

            if(currentSignature != previousSignature){
                val scanSongs = scanTracks(context) ?: emptyList()
                _songs.value = scanSongs
                prefs.librarySign = currentSignature
                saveCache(context, scanSongs)
            }
        }

    }
    override suspend fun forceSyncLibrary() {
        try{
            val scanSongs = scanTracks(context) ?: emptyList()
            _songs.value = scanSongs
            val currentSignature = librarySignature(context)?.let{serializeSignature(it)}
            currentSignature?.let{signature->
                prefs.librarySign = signature
            }
            saveCache(context, scanSongs)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }
}
