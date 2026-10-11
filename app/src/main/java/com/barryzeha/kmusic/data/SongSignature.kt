package com.barryzeha.kmusic.data

import kotlinx.serialization.Serializable

/****
 * Project KMusic
 * Created by Barry Zea H. on 4/10/26.
 * Copyright (c)  All rights reserved.
 ***/

@Serializable
data class SongSignature(
    val id:Long,
    val dateAdded:Long,
    val dateModified:Long,
    val songCount:Int
    )
