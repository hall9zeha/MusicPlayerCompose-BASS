package com.barryzeha.kmusic.service

import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.barryzeha.kmusic.R
import com.barryzeha.kmusic.common.BassManager
import com.barryzeha.kmusic.common.MediaControllerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel


/****
 * Project KMusic
 * Created by Barry Zea H. on 24/04/25.
 * Copyright (c)  All rights reserved.
 ***/
private const val MEDIA_SESSION_ID="mediaSessionBass"
private const val NOTIFICATION_ID = 12345
class PlaybackService: MediaSessionService(){
    private var mediaSession: android.media.session.MediaSession?=null
    private lateinit var notificationManager: NotificationManager
    private  var mediaPlayerNotify: Notification?=null
    private  lateinit var mediaStyle: MediaStyle
    private var playBackState:PlaybackState? = null
    private var mediaMetadata:MediaMetadata? = null
    private var songRunnable: Runnable = Runnable {  }
    private var songHandler: Handler = Handler(Looper.getMainLooper())
    private var mediaController: MediaControllerUtil?=null
    private var bassManager: BassManager?=null
    private var player: ExoPlayer? = null
    private val serviceScope: CoroutineScope = MainScope()
    private var currentSongId=-1L

    override fun onCreate() {
        super.onCreate()
        mediaController = MediaControllerUtil.getInstance(this)
        setupPlayer()


    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        TODO("Not yet implemented")
    }


    @OptIn(UnstableApi::class)
    private fun setupPlayer(){
        mediaSession=android.media.session.MediaSession(this,MEDIA_SESSION_ID)
        mediaStyle = MediaStyle().setMediaSession(mediaSession?.sessionToken)
        createNotification()
        initLoop()

    }
    private fun initLoop(){
        songRunnable = Runnable {
            updateNotify()
            songHandler.postDelayed(songRunnable,500)
        }
        songHandler.post(songRunnable)
    }
    private fun createNotification(): Notification {
        mediaController?.let{
            bassManager=it.bassManager
            val playerState= it.state.value
            val song=playerState?.currentMediaItem
            song?.let{song->
                playBackState = PlaybackState.Builder()
                    .setState(if(playerState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        playerState.currentPosition,
                        1f
                        )
                    .setActions(
                        PlaybackState.ACTION_SEEK_TO
                                or PlaybackState.ACTION_PLAY
                                or PlaybackState.ACTION_PAUSE
                                or PlaybackState.ACTION_SKIP_TO_NEXT
                                or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackState.ACTION_STOP
                    ).build()

                mediaMetadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                    //.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getBitmap(this,newState.songPath, isForNotify = true))
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                    .build()
                mediaSession?.setPlaybackState(playBackState)
                mediaSession?.setMetadata(mediaMetadata)
            }

        }

        val channelId = "playback_channel"

        // Crear el canal de notificación si es necesario

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Playback Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val mediaStyle=MediaStyle()
            .setMediaSession(mediaSession?.sessionToken!!)
            .setShowActionsInCompactView(0,1,2)

        val notificationBuilder = Notification.Builder(this, channelId)
            /*.setContentTitle("Reproduciendo música")
            .setContentText("Tu música está sonando...")*/
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Cambia esto por un icono adecuado
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true) // Esto hará que la notificación sea persistente

        mediaPlayerNotify = notificationBuilder.build()
        startForeground(NOTIFICATION_ID, mediaPlayerNotify)
        notificationManager.notify(NOTIFICATION_ID,mediaPlayerNotify)

        return mediaPlayerNotify!!
    }
    private fun updateNotify(){
        mediaController?.let{
            val playerState= it.state.value
            val song=playerState?.currentMediaItem
            song?.let { song ->
                val updatePlaybackState = playBackState?.let {
                    PlaybackState.Builder(it)
                        .setState(
                            if (playerState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                            playerState.currentPosition,
                            1f
                        )
                        .build()
                }
                // Actualizamos el progreso y estado de reproducción de la canción
                mediaSession?.setPlaybackState(updatePlaybackState)
                if (currentSongId.toLong() != song.idSong) {// Comparamos los ids para saber si ha cambiado la canción
                    val updateMediaMetadata = MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                        //.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getBitmap(this,newState.songPath, isForNotify = true))
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                        .build()

                    // Para android >=12
                    mediaSession?.setMetadata(updateMediaMetadata)
                    // Reemplazamos temporalmente el nuevo id para la comparación
                    currentSongId = song.idSong

                    // Para android <=10
                    val channelId = "playback_channel"
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        mediaPlayerNotify = Notification.Builder(this, channelId)
                            /*.setContentTitle("Reproduciendo música")
                            .setContentText("Tu música está sonando...")*/
                            .setStyle(mediaStyle)
                            .setSmallIcon(R.drawable.ic_launcher_foreground) // Cambia esto por un icono adecuado
                            .setPriority(Notification.PRIORITY_LOW)
                            .setOngoing(true)
                            .build()
                    // Esto hará que la notificación sea persistente

                    }
                    notificationManager.notify(
                        NOTIFICATION_ID,mediaPlayerNotify
                    )
                }
            }

        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        mediaSession?.run{
            player?.release()
            release()
            mediaSession = null
            serviceScope.cancel()
        }
        super.onDestroy()
    }
}