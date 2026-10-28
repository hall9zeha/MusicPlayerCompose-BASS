package com.barryzeha.kmusic.playback

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
class MediaControllerUtil internal constructor(context: Context): BassManager.PlaybackManager{
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
            bassManager = manager.getInstance()
            bassManager?.registerPlaybackState(this)
            bassManager?.let {
                _state.value = PlayerStateImpl.getInstance(it)
            }
        }
    }

    override fun onFinishPlayback() {
    }

    override fun playbackStateChanged() {
        Log.e("PLAYBACK_CHANGED", "playbackStateChanged: UTIL " )
        playbackState?.playbackStateChanged()
    }

    internal fun release(){
        bassManager?.releasePlayback()
        bassManager?.unregisterPlaybackState()
        bassManager = null
        _state.value = null
    }
    companion object{
        @Volatile
        private var instance: MediaControllerUtil? = null
        private var playbackState: PlaybackControllerState?=null
        fun getInstance(context: Context, playbackState: PlaybackControllerState): MediaControllerUtil{
            this.playbackState = playbackState
            return instance?: MediaControllerUtil(context).also { instance = it }

        }
    }
    interface PlaybackControllerState{
        fun playbackStateChanged()
    }
}