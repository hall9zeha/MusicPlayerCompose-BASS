package com.barryzeha.kmusic.common

import android.icu.text.Normalizer2
import android.view.animation.PathInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.PathParser
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.barryzeha.kmusic.data.SongEntity

/****
 * Project KMusic
 * Created by Barry Zea H. on 22/04/25.
 * Copyright (c)  All rights reserved.
 ***/

fun String.trimAndNormalize(): String{
    return Normalizer2.getNFCInstance().normalize(this.trim())
}
internal val Player.currentMediaItems: List<MediaItem> get() {
    return List(mediaItemCount, ::getMediaItemAt)
}
fun Player.updatePlaylist(mediaItems: List<MediaItem>){
    val oldMediaItems = currentMediaItems.map{it.mediaId}.toSet()
    val itemsToAdd = mediaItems.filterNot { item->item.mediaId in oldMediaItems }
    addMediaItems(itemsToAdd)
}
fun BassManager.updatePlaylist(songsList: List<SongEntity>){
    val oldMediaItems = playlist.map{it.idSong}.toSet()
    val itemsToAdd = songsList.filterNot { item->item.idSong in oldMediaItems }
    setPlaylist(itemsToAdd)
}

fun BassManager.playMediaAtIndex(index:Int){
    val song = playlist[index]
    currentIndexOfSong = index
    streamCreateFile(song)
    channelPlay(0)
}
fun BassManager.playMediaById(id:Int){
    val song = playlist.first { id.toLong() == it.idSong }
    val position = playlist.indexOfFirst { id.toLong() == it.idSong  }
    currentIndexOfSong = position
    streamCreateFile(song)
    channelPlay(0)
}
fun BassManager.setMediaWithProgress(index:Int, progress: Long){
    try {
        val song = playlist[index]
        streamCreateFile(song)
        setChannelProgress(progress.toLong()) {  }
    }catch(e: Exception){
        e.printStackTrace()
    }

}
fun <T> emphasized(durationMillis: Int, delayMillis: Int = 0): TweenSpec<T> {
    return tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EmphasizedEasing(),
    )
}

@Immutable
class EmphasizedEasing : Easing {
    override fun transform(fraction: Float): Float {
        return emphasizedInterpolator.getInterpolation(fraction)
    }

    override fun equals(other: Any?): Boolean {
        return other is EmphasizedEasing
    }

    override fun hashCode(): Int {
        return 0
    }
}

private val emphasizedInterpolator =
    PathInterpolator(
        PathParser.createPathFromPathData(
            "M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1"
        )
    )
fun Color.contentColor(): Color {
    return if (luminance() < 0.5f) lerp(this, Color.White, 0.9f) else lerp(this, Color.Black, 0.4f)
}