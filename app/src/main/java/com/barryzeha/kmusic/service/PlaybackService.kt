package com.barryzeha.kmusic.service

import android.content.Intent
import android.media.audiofx.AudioEffect

import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.AudioAttributes
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


/****
 * Project KMusic
 * Created by Barry Zea H. on 24/04/25.
 * Copyright (c)  All rights reserved.
 ***/

class PlaybackService:MediaSessionService(){
    private var mediaSession: MediaSession?=null
    private var player: ExoPlayer? = null
    private val serviceScope: CoroutineScope = MainScope()
    override fun onCreate() {
        super.onCreate()
        setupPlayer()

    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer(){
        // Set Media Audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        // Building a Exoplayer instance
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes,true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                trackSelectionParameters=trackSelectionParameters.buildUpon()
                    .setAudioOffloadPreferences(
                        TrackSelectionParameters.AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(AUDIO_OFFLOAD_MODE_ENABLED).build()
                    ).build()
            }

        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply{
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player?.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        mediaSession?.run{
            player.release()
            release()
            mediaSession = null
            serviceScope.cancel()
        }
        super.onDestroy()
    }
}