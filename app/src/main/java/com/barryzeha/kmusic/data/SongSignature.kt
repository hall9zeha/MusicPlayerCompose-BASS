package com.barryzeha.kmusic.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/****
 * Project KMusic
 * Created by Barry Zea H. on 4/10/26.
 * Copyright (c)  All rights reserved.
 ***/

@Keep
@Serializable
data class SongSignature(
    val id:Long,
    val dateAdded:Long,
    val dateModified:Long,
    val songCount:Int
    )
