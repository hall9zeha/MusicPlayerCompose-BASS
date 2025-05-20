package com.barryzeha.kmusic.common

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.barryzeha.kmusic.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
    private var factory: ListenableFuture<MediaController>? = null
    var controller = mutableStateOf<MediaController?>(null)
    private var _state = MutableStateFlow<PlayerState?>(null)
    val state = _state.asStateFlow()

    init {
        initialize()
    }
    fun initialize(){
        if (factory == null || factory?.isDone == true) {
                factory = MediaController.Builder(
                    appContext,
                    SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
                ).buildAsync()
        }
        factory?.addListener(
            {
                controller.value = factory?.let { if (it.isDone) it.get() else null }
                _state.value = PlayerStateImpl.getInstance(controller.value!!)!!
            }, MoreExecutors.directExecutor()
        )

    }
    internal fun release(){
        factory?.let{
            //TODO If we release the media controller on each recomposition, we will not be able to interact with the service.
           MediaController.releaseFuture(it)
           controller.value = null
        }
       factory = null
    }
    companion object{
        @Volatile
        private var instance: MediaControllerUtil? = null

        fun getInstance(context: Context): MediaControllerUtil{

            return instance?:synchronized(this) {
                instance?: MediaControllerUtil(context).also { instance = it }
            }
        }
    }
}