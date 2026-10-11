package com.barryzeha.kmusic.data.repository

import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/****
 * Project KMusic
 * Created by Barry Zea H. on 29/09/26.
 * Copyright (c)  All rights reserved.
 ***/

interface SongRepository {
    val songs: StateFlow<List<SongEntity>>
    suspend fun loadSongs()
    suspend fun syncLibraryTracks()
}