package com.barryzeha.kmusic.common

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.barryzeha.kmusic.MainApp
import com.barryzeha.kmusic.data.PlaybackManagerListener
import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/****
* Project KMusic
* Created by Barry Zea H. on 4/05/25.
* Copyright (c)  All rights reserved.
***/

interface PlayerState{
    val player: BassManager
    val currentMediaItem: SongEntity?
    val mediaMetadata: MediaMetadata?
    val isPlaylistPopulated: MutableStateFlow<Boolean>
    val repeatMode:Int

    val isShuffleMode: Boolean
    var mediaItemIndex:Int
    val currentPosition: Long
    @get:Player.State
    val playbackState: Int

    val isPlaying: Boolean
    var isDraggingProgressSlider: Boolean
    fun registerListener()
    fun unregisterListener()
    fun setupRepeatAndShuffleMode()
    fun startTrackingPlaybackPosition(context: Context)

}
internal class PlayerStateImpl(): PlayerState{

    override var player: BassManager by mutableStateOf(playerInstance)
        set
    override var currentMediaItem: SongEntity? by mutableStateOf(player.currentMediaItem)
        private set
    override var mediaMetadata: MediaMetadata? by mutableStateOf(null)
        private set
    override var isPlaylistPopulated: MutableStateFlow<Boolean> = MutableStateFlow(player.populatePlaylistFinished)
        set
    override var repeatMode: Int by mutableIntStateOf(player.repeatMode)
        private set
    override var isShuffleMode: Boolean by mutableStateOf(player.shuffleMode)
        private set
    override var mediaItemIndex: Int by mutableStateOf(player.currentIndexOfSong)
        set
    override var playbackState: Int by mutableIntStateOf(player.getPlaybackState())
        private set
    override var isPlaying: Boolean by mutableStateOf(player.isPlaying)
        private set
    override var isDraggingProgressSlider: Boolean by mutableStateOf(false)
        set
    override var currentPosition: Long by mutableStateOf(player.currentPosition)
        private set

    private val listener = object: PlaybackManagerListener{
        override fun currentMediaItem(mediaItem: SongEntity) {
            this@PlayerStateImpl.currentMediaItem = mediaItem
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            this@PlayerStateImpl.mediaMetadata = mediaMetadata
        }

        override fun onPlaylistHasPopulated(isPopulated: Boolean) {
            this@PlayerStateImpl.isPlaylistPopulated.value=isPopulated
        }

        override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
            this@PlayerStateImpl.playbackState = playbackState
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerStateImpl.isPlaying = isPlaying
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            this@PlayerStateImpl.repeatMode = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            this@PlayerStateImpl.isShuffleMode = shuffleModeEnabled
        }
    }
    init{
        setupRepeatAndShuffleMode()
    }
    override fun registerListener() {
        player.addListener(listener)
    }

    override fun unregisterListener() {
        player.removeListener(listener)
    }

    override fun setupRepeatAndShuffleMode() {
        player.shuffleMode = MainApp.mPrefs?.isShuffleMode!!
        player.repeatMode = MainApp.mPrefs?.repeatMode!!
    }

    override fun startTrackingPlaybackPosition(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // To synchronize the new slider scroll position with the view so that there are no erratic changes when moving the thumb and scrolling is smoother
            val frameTime = (1f / context.display.refreshRate).toDouble().milliseconds

            while (isActive) {
                withContext(Dispatchers.Main) {
                    val currentPosition = player.currentPosition
                    if (!isDraggingProgressSlider) {
                        this@PlayerStateImpl.currentPosition = currentPosition
                    }
                }
                delay(frameTime)
            }
        }
    }

    companion object{
        private var instance: PlayerStateImpl?=null
        private lateinit var playerInstance: BassManager
        fun getInstance(player: BassManager): PlayerState?{
            playerInstance=player
            if(instance==null){
                instance= PlayerStateImpl()
            }
            return instance
        }
    }
}