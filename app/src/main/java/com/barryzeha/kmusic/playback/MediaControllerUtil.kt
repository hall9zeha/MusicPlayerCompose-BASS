package com.barryzeha.kmusic.playback

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Stable
import com.barryzeha.kmusic.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/****
* Project KMusic
* Created by Barry Zea H. on 4/05/25.
* Copyright (c)  All rights reserved.
***/


@Stable
class MediaControllerUtil internal constructor(context: Context){
    private val appContext = context.applicationContext
    var bassManager: BassManager? = null
    private var _state = MutableStateFlow<PlayerState?>(null)
    val state = _state.asStateFlow()

    fun initialize(){
        if (bassManager == null) {
            val serviceIntent = Intent(appContext, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }

            val manager = BassManager()
            bassManager = manager.getInstance(object : BassManager.PlaybackManager {
                override fun onFinishPlayback() {
                   // _state.value = PlayerStateImpl.STOPPED
                }
            })
            bassManager?.let {
                _state.value = PlayerStateImpl.getInstance(it)
            }
        }
    }


    internal fun release(){
        bassManager?.releasePlayback()
        bassManager = null
        _state.value = null
    }
    companion object{
        @Volatile
        private var instance: MediaControllerUtil? = null

        fun getInstance(context: Context): MediaControllerUtil{

            return instance?: MediaControllerUtil(context).also { instance = it }

        }
    }
}