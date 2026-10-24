package com.barryzeha.kmusic.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.barryzeha.kmusic.R
import com.barryzeha.kmusic.common.util.loadArtwork
import com.barryzeha.kmusic.playback.PlayerState
import com.barryzeha.kmusic.ui.theme.Typography

/****
 * Project KMusic
 * Created by Barry Zea H. on 27/04/25.
 * Copyright (c)  All rights reserved.
 ***/


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MiniPlayerView(modifier:Modifier = Modifier, playerState: PlayerState?){
    val context = LocalContext.current

    Card(modifier = modifier,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProgressLine(modifier, playerState!!)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentMediaItem = playerState?.currentMediaItem
                if (currentMediaItem != null) {
                   MiniPlayerCoverArt(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp),
                        currentMediaItem.idSong.toString()
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    ) {
                        Text(
                            modifier=Modifier.basicMarquee(),
                            text = currentMediaItem.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier=Modifier.basicMarquee(),
                            text = currentMediaItem.artist,
                            maxLines = 1,
                            style = Typography.labelSmall,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PlayPauseButton(
                    modifier = Modifier.size(40.dp),
                    isPlaying = playerState?.isPlaying!!,
                    isBuffering = (playerState?.playbackState == Player.STATE_BUFFERING)
                ) {
                    with(playerState.player) {
                        playWhenReady = !playWhenReady
                    }
                }
                NextButton() {
                    with(playerState.player){
                        seekToNextMediaItem()
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MiniPlayerCoverArt(modifier: Modifier, idSong: String){
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null,key1=idSong.toLong()) {
        value = loadArtwork(context,idSong.toLong())
    }
    Card(modifier = modifier,
        shape = RoundedCornerShape(4.dp)
        ) {

        bitmap?.let {bitmap->
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bitmap.asImageBitmap(),
                contentScale = ContentScale.Crop,
                contentDescription = "Cover album art"
            )
        }?:run{
            Image(
                modifier = Modifier.fillMaxSize(),
                painter= painterResource(R.drawable.ic_launcher_foreground),
                contentScale = ContentScale.Crop,
                contentDescription = "Cover album art"
            )
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier,
                    isPlaying:Boolean,
                    isBuffering:Boolean,
                    iconTint: Color= MaterialTheme.colorScheme.onSurface,
                    onPlayPauseClick:()-> Unit
                    ){
    if(isBuffering){
        PlayerRoundedProgressIndicator(
            modifier=modifier,
            progressTint = iconTint
        )
    }
    else {
        IconButton(
            modifier = modifier,
            onClick = { onPlayPauseClick() }
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(.8f),
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = iconTint
            )
        }
    }
}
@Composable
fun NextButton(modifier: Modifier = Modifier,iconTint: Color= MaterialTheme.colorScheme.onSurface,onNextClick:()->Unit){
    IconButton(
        modifier = modifier,
        onClick = { onNextClick() }
    ) {
        Icon(
            modifier=Modifier.fillMaxSize(.6f),
            imageVector= Icons.Filled.SkipNext,
            contentDescription = "Next",
            tint=iconTint
        )
    }
}
@Composable
fun ProgressLine(modifier: Modifier, player: PlayerState){
    val context = LocalContext.current
    var currentPos = remember { mutableLongStateOf(player.currentPosition) }
    val duration = remember {mutableLongStateOf(player.currentMediaItem?.duration?:0L)}

    LaunchedEffect(player) {
        player.startTrackingPlaybackPosition(context)
        duration.longValue = player.player.getDuration()
    }
    LaunchedEffect(player.isPlaying) {
        duration.longValue = player.player.getDuration()
        snapshotFlow { player.currentPosition }
            .collect { pos ->
                currentPos.longValue = pos
            }
    }
    LaunchedEffect(player.currentMediaItem?.idSong) {
        duration.longValue = player.player.getDuration()
    }
    if(player.player.currentMediaItem!=null) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { currentPos.longValue.toFloat() / duration.longValue.toFloat() }
        )
    }

}
@Composable
fun PlayerRoundedProgressIndicator(
    modifier: Modifier = Modifier,
    progressTint:Color = MaterialTheme.colorScheme.onSurface
){
    Box(
        modifier = modifier
    ){
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize(.8f)
                .align(Alignment.Center),
            color = progressTint,
            trackColor = Color.Transparent
        )
    }

}

@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CompactPlayerPreview(){
    MiniPlayerView(Modifier, null)
}