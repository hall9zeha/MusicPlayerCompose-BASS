package com.barryzeha.kmusic.ui.navigation

import com.barryzeha.kmusic.common.PlayerState

/****
 * Project KMusic
 * Created by Barry Zea H. on 10/05/25.
 * Copyright (c)  All rights reserved.
 ***/

sealed class Routes(val route: String) {
    object Playlist: Routes("playlist")
    object Player: Routes("player?playerStateArg={playerStateArg}"){
        fun createRoute(playerState: PlayerState) = "player?playerStateArg=$playerState"
    }
}