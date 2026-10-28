package com.barryzeha.kmusic.service

import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.barryzeha.kmusic.MainActivity
import com.barryzeha.kmusic.R
import com.barryzeha.kmusic.common.util.loadArtwork
import com.barryzeha.kmusic.playback.BassManager
import com.barryzeha.kmusic.playback.MediaControllerUtil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


/****
 * Project KMusic
 * Created by Barry Zea H. on 24/04/25.
 * Copyright (c)  All rights reserved.
 ***/
private const val MEDIA_SESSION_ID="mediaSessionBass"
private const val NOTIFICATION_ID = 12345
// Podemos heredar de "Service" y no necesariamente de MediaSessionService porque en este caso no usaremos Exoplayer ni media3
// ya que estaremos usando la librería BASS y solo necesitamos las notificaciones multimedia.
class PlaybackService: MediaSessionService(), MediaControllerUtil.PlaybackControllerState{

    private companion object {
        const val ACTION_PREVIOUS = "com.barryzeha.kmusic.PREVIOUS"
        const val ACTION_PLAY = "com.barryzeha.kmusic.PLAY"
        const val ACTION_PAUSE = "com.barryzeha.kmusic.PAUSE"
        const val ACTION_NEXT = "com.barryzeha.kmusic.NEXT"
    }
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
        mediaController = MediaControllerUtil.getInstance(this, this)
        setupPlayer()
    }

    override fun playbackStateChanged() {
        updateNotify()
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
        mediaSession?.setCallback(mediaSessionCallback())

    }

    private fun initLoop(){
        songRunnable = Runnable {
            updateNotify()
            songHandler.postDelayed(songRunnable,500)
        }
        songHandler.post(songRunnable)
    }
    private fun stopLoop(){
        songHandler.removeCallbacks(songRunnable)
    }
    private fun startLoop(){
        songHandler.post(songRunnable)
    }
    private fun createNotification(): Notification {
        val isPlaying = mediaController?.state?.value?.isPlaying == true
        mediaController?.let{

            bassManager=it.bassManager
            val playerState= it.state.value
            val song=playerState?.currentMediaItem
            playerState?.let {
                playBackState = PlaybackState.Builder()
                    .setState(
                        if (playerState!!.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
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
            }
            song?.let{song->
                mediaMetadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
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
            //.setShowActionsInCompactView(0,1,2)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            123,
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationBuilder= if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.Q) {
             Notification.Builder(this, channelId)
                .addAction(R.drawable.ic_prev, "Previous", actionPendingIntent(ACTION_PREVIOUS))
                .addAction(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (isPlaying) "Pause" else "Play",
                    actionPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
                )
                .addAction(R.drawable.ic_next, "Next", actionPendingIntent(ACTION_NEXT))
                .setStyle(mediaStyle)
                .setContentIntent(mainPendingIntent)
                .setSmallIcon(R.drawable.kmusic_icon_placeholder)
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true)
        }else{
            Notification.Builder(this, channelId)
                .setStyle(mediaStyle)
                .setContentIntent(mainPendingIntent)
                .setSmallIcon(R.drawable.kmusic_icon_placeholder)
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true)
        }
        mediaPlayerNotify = notificationBuilder.build()
        startForeground(NOTIFICATION_ID, mediaPlayerNotify)
        notificationManager.notify(NOTIFICATION_ID,mediaPlayerNotify)

        return mediaPlayerNotify!!
    }
    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
                        .setActions(
                            PlaybackState.ACTION_SEEK_TO
                                    or PlaybackState.ACTION_PLAY
                                    or PlaybackState.ACTION_PAUSE
                                    or PlaybackState.ACTION_SKIP_TO_NEXT
                                    or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                                    or PlaybackState.ACTION_STOP
                        )
                        .build()
                }
                // Actualizamos el progreso y estado de reproducción de la canción
                mediaSession?.setPlaybackState(updatePlaybackState)
                serviceScope.launch {
                    val artwork = loadArtwork(this@PlaybackService, song.idSong, 96)

                    if (currentSongId.toLong() != song.idSong) {// Comparamos los ids para saber si ha cambiado la canción

                            val updateMediaMetadata = MediaMetadata.Builder()
                                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                                .putBitmap(
                                    MediaMetadata.METADATA_KEY_ALBUM_ART,
                                    artwork
                                )

                                .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                                .build()

                            // Para android >=12
                            mediaSession?.setMetadata(updateMediaMetadata)
                            // Reemplazamos temporalmente el nuevo id para la comparación
                            currentSongId = song.idSong
                            notificationManager.notify(
                                NOTIFICATION_ID, mediaPlayerNotify
                            )
                        }

                        val isPlaying = playerState.isPlaying
                    // Para android <=10
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                            val notification = Notification.Builder(this@PlaybackService, "playback_channel")
                                .addAction(
                                    R.drawable.ic_prev,
                                    "Previous",
                                    actionPendingIntent(ACTION_PREVIOUS)
                                )
                                .addAction(
                                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                                    if (isPlaying) "Pause" else "Play",
                                    actionPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
                                )
                                .addAction(
                                    R.drawable.ic_next,
                                    "Next",
                                    actionPendingIntent(ACTION_NEXT)
                                )
                                .setStyle(
                                    MediaStyle()
                                        .setMediaSession(mediaSession?.sessionToken)
                                        .setShowActionsInCompactView(0, 1, 2)
                                )
                                .setContentTitle(song.title)
                                .setContentText(song.artist)
                                .setSmallIcon(R.drawable.kmusic_icon_placeholder)
                                .setLargeIcon(artwork)
                                .setOngoing(true)
                                .build()
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PREVIOUS -> bassManager?.seekToPreviousMediaItem()
            ACTION_PLAY -> bassManager?.playWhenReady = true
            ACTION_PAUSE -> bassManager?.playWhenReady = false
            ACTION_NEXT -> bassManager?.seekToNextMediaItem()
        }

        return START_NOT_STICKY

    }
    private fun mediaSessionCallback(): android.media.session.MediaSession.Callback{
        return object: android.media.session.MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                bassManager?.playWhenReady = true
            }
            override fun onPause() {
                super.onPause()
                bassManager?.playWhenReady = !bassManager?.playWhenReady!!
            }
            override fun onSkipToNext() {
                super.onSkipToNext()
                stopLoop()
                bassManager?.seekToNextMediaItem()
                startLoop()
            }
            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                stopLoop()
                bassManager?.seekToPreviousMediaItem()
                startLoop()
            }
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                bassManager?.seekTo(pos)
            }
        }
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        mediaSession?.run{
            bassManager?.releasePlayback()
            release()
            mediaSession = null
            serviceScope.cancel()
        }
        super.onDestroy()
    }
}