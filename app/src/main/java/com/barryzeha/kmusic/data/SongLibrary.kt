package com.barryzeha.kmusic.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

/****
 * Project KMusic
 * Created by Barry Zea H. on 27/09/26.
 * Copyright (c)  All rights reserved.
 ***/

@Keep
@Serializable
data class SongLibrary(val songs: List<SongEntity>)
