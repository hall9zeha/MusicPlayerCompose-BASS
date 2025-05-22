package com.barryzeha.kmusic.data

import androidx.media3.common.MediaMetadata

/****
 * Project KMusic
 * Created by Barry Zea H. on 20/05/25.
 * Copyright (c)  All rights reserved.
 ***/

interface PlaybackManagerListener {
    fun currentMediaItem(mediaItem: SongEntity)
    fun onMediaMetadataChanged(mediaMetadata: MediaMetadata)
    fun onPlaybackStateChanged(playbackState: Int)
    fun onPlaylistHasPopulated(isPopulated: Boolean)
    fun onIsPlayingChanged(isPlaying: Boolean)
    fun onRepeatModeChanged(repeatMode: Int)
    fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean)
}