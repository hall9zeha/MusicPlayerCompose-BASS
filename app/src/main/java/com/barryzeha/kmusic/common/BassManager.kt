package com.barryzeha.kmusic.common

/****
 * Project KMusic
 * Created by Barry Zea H. on 20/05/25.
 * Copyright (c)  All rights reserved.
 ***/


import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaMetadata
import com.barryzeha.kmusic.MainApp
import com.barryzeha.kmusic.data.BassRepeatMode
import com.barryzeha.kmusic.data.PlaybackManagerListener
import com.barryzeha.kmusic.data.SongEntity
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_INFO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer

private const val SAMPLE44 = 44100
private const val SAMPLE48 = 48000
private const val SAMPLE96 = 96000
private const val SAMPLE192 = 192000
private const val TAG = "BASS-MANAGER"
private var mainChannel:Int?=0
private val handler = Handler(Looper.getMainLooper())
private val aBLoopHandler = Handler(Looper.getMainLooper())
private var checkRunnable: Runnable? = null
private var updateTimer: Timer? = null
private var idSong:Long?=null

// For A-B looper
private var startAbLoopPosition:Long=0
private var endAbLopPosition:Long=0
private var listeners: MutableList<PlaybackManagerListener> = mutableListOf()

private var _playlist: MutableList<SongEntity> = mutableListOf()
val playlist: List<SongEntity> get() =_playlist

open class BassManager {

    private var instance: BassManager? = null

    var isPlaying: Boolean = false
    // Por defecto el modo desactivado
    private var _repeatMode: Int = BassRepeatMode.REPEAT_MODE_OFF.value
    private var _shuffleMode:Boolean = false

    var shuffleMode: Boolean
        get() = _shuffleMode
        set(value){saveShuffleMode(value)}

    var repeatMode:Int
        get()=_repeatMode
        set(value){saveRepeatMode(value)}

    var currentPosition: Long=0
        set
    var currentIndexOfSong:Int=0
        set
    var playWhenReady: Boolean
        get() = isPlaying
        set(value){isPlaying = playPause(value)}
    var currentMediaItem: SongEntity?=null
        set
    var populatePlaylistFinished: Boolean=false
        set
    //var currentMediaData: MediaMetadata?=getCurrentMediaData()


    private   var playbackManager:PlaybackManager?=null
    init {
        BASS.BASS_Init(-1, 44100, 0)
    }
    fun getInstance(playbackManager: PlaybackManager):BassManager?{
        instance?.let{ return it}?:run{
            instance=BassManager()
            this.playbackManager = playbackManager
            if (!BASS.BASS_Init(-1, SAMPLE192, BASS.BASS_DEVICE_FREQ)) {
                Log.i(TAG, "Can't initialize device")
                Log.i(TAG, "init with sample " + SAMPLE96 + "Hz")
                if (!BASS.BASS_Init(-1, SAMPLE96, BASS.BASS_DEVICE_FREQ)) {
                    Log.i(TAG, "Can't initialize device")
                    Log.i(TAG, "init with sample " + SAMPLE48 + "Hz")
                    if (!BASS.BASS_Init(-1, SAMPLE48, BASS.BASS_DEVICE_FREQ)) {
                        Log.i(TAG, "Can't initialize device")
                        Log.i(TAG, "init with sample " + SAMPLE44 + "Hz")
                        if (!BASS.BASS_Init(-1, SAMPLE44, BASS.BASS_DEVICE_FREQ)) {
                            Log.i(TAG, "Can't initialize device")
                        }
                    }
                }
            }
            val info = BASS_INFO()
            if (BASS.BASS_GetInfo(info)) {
                Log.i(TAG, "Min Buffer :" + info.minbuf)
                Log.i(TAG, "Direct Sound Ver :" + info.dsver)
                Log.i(TAG, "Latency :" + info.latency)
                Log.i(TAG, "speakers :" + info.speakers)
                Log.i(TAG, "freq :" + info.freq)
            }
        }
        configure()
        val nativeDir = MainApp.context?.applicationInfo?.nativeLibraryDir
        val pluginsList = File(nativeDir).list { dir, name -> name.matches("libbass.+\\.so|libtags\\.so".toRegex()) }
        pluginsList?.forEach { plugin->
            BASS.BASS_PluginLoad(plugin,0)
        }
        return instance
    }
    private fun configure(){
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC, 3)
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_SRC_SAMPLE, 3)
    }
    fun startCheckingPlayback(){
        stopRunnable()
        checkRunnable = object:Runnable{
            override fun run() {
                if (BASS.BASS_ChannelIsActive(getActiveChannel()) == BASS.BASS_ACTIVE_PLAYING) {
                    currentPosition = getCurrentPositionInSeconds(getActiveChannel())
                }
                if (BASS.BASS_ChannelIsActive(getActiveChannel()) == BASS.BASS_ACTIVE_PAUSED){
                    currentPosition = getCurrentPositionInSeconds(getActiveChannel())
                }
                if (BASS.BASS_ChannelIsActive(getActiveChannel()) == BASS.BASS_ACTIVE_STOPPED) {
                    handleOnFinishPlaybackBehavior()

                }
                handler.postDelayed(this,500)
            }
        }
        handler.post(checkRunnable!!)
    }
    fun stopCheckingPlayback(){
        stopRunnable()
    }
    private fun stopRunnable(){
        checkRunnable?.let{
            handler.removeCallbacks(it)
            checkRunnable = null
        }
    }
    private fun handleOnFinishPlaybackBehavior(){
        if (currentIndexOfSong < playlist.size) {
            when (repeatMode) {
                BassRepeatMode.REPEAT_MODE_ONE.value -> {if (isPlaying) {repeatSong()}}
                else -> {if (isPlaying) seekToNextMediaItem()}
            }
            if(_shuffleMode && isPlaying){
                currentIndexOfSong = (playlist.indices).random()
                streamCreateFile(playlist[currentIndexOfSong])
                channelPlay(0)
            }
        } else {
            when (repeatMode) {
                BassRepeatMode.REPEAT_MODE_ALL.value -> {
                    currentIndexOfSong=0
                    streamCreateFile(playlist[0])
                    channelPlay(0)
                }
                BassRepeatMode.REPEAT_MODE_ONE.value -> {if (isPlaying) {repeatSong()}}
                else -> {
                    if (playlist.isNotEmpty()) streamCreateFile(playlist[0])
                    isPlaying = false
                    stopCheckingPlayback()
                }
            }
        }
    }
    fun setSongStateSaved(channel:Int, position:Long){
        mainChannel = channel
        val positionBytes = getCurrentPositionToBytes(position)
        BASS.BASS_ChannelSetPosition(channel, positionBytes, BASS.BASS_POS_BYTE)
    }
    fun streamCreateFile(song:SongEntity){
        // Cleaning a previous track if have anyone
        BASS.BASS_StreamFree(getActiveChannel())
        // Creating the new channel for playing
        isPlaying=false
        mainChannel = BASS.BASS_StreamCreateFile(song.pathFile, 0, 0, BASS.BASS_SAMPLE_FLOAT)
        currentIndexOfSong = playlist.indexOfFirst{it.idSong==song.idSong}
        // Guardamos el índice para obtener la los detalles de la pista cuando volvamos a abrir la app
        MainApp.mPrefs?.currentIndexSaved = currentIndexOfSong

        listeners.forEach {
            it.currentMediaItem(playlist[currentIndexOfSong])
            it.onMediaMetadataChanged(getCurrentMediaData()!!)
        }
    }
    fun playPause(play: Boolean): Boolean{
        if(play){
            channelPlay(currentPosition)
        }else{
            channelPause()
        }
        return play
    }
    fun channelPlay(currentSongProgress:Long): Boolean{
        BASS.BASS_ChannelSetAttribute(getActiveChannel(),BASS.BASS_ATTRIB_VOL,1F)
        // Convert the current position (in milliseconds) to bytes with  bassManager?.getCurrentPositionToBytes
        BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(currentSongProgress),BASS.BASS_POS_BYTE)
        BASS.BASS_ChannelPlay(getActiveChannel()!!, false)
        currentMediaItem = playlist[currentIndexOfSong]
        listeners.forEach {
            isPlaying=true
            it.currentMediaItem(currentMediaItem!!)
            it.onMediaMetadataChanged(getCurrentMediaData()!!)
            it.onIsPlayingChanged(true)
        }
        return true
    }
    fun channelPause(){
        BASS.BASS_ChannelPause(getActiveChannel())
        listeners.forEach {
           it.onIsPlayingChanged(false)
        }
    }
    fun seekTo(progress:Long){
        setChannelProgress(progress){
            currentPosition = it
        }
    }
    fun seekToNextMediaItem(){
        if(isPlaying) {
            if (currentIndexOfSong < playlist.size) {
                currentIndexOfSong = if(shuffleMode) (playlist.indices).random() else currentIndexOfSong++
            }else{
                currentIndexOfSong = 0
            }
            streamCreateFile(playlist[currentIndexOfSong])
            channelPlay(currentSongProgress = 0)
        }else{
            if (currentIndexOfSong < playlist.size) {
                currentIndexOfSong = if(shuffleMode) (playlist.indices).random() else currentIndexOfSong++

            }else{
                currentIndexOfSong =  0
            }
            streamCreateFile(playlist[currentIndexOfSong])
        }

    }
    fun seekToPreviousMediaItem(){
        if(isPlaying){
            if(currentIndexOfSong>0){
                currentIndexOfSong--
                streamCreateFile(playlist[currentIndexOfSong])
                channelPlay(currentSongProgress = 0)
            }
        }else{
            if(currentIndexOfSong>0){
                currentIndexOfSong--
                streamCreateFile(playlist[currentIndexOfSong])
            }
        }
    }

    fun fastForwardOrRewind(isForward:Boolean,currentProgress: (Long) -> Unit){
        val progressOnSeconds = getCurrentPositionInSeconds(getActiveChannel())
        val forwardProgress = if(isForward)progressOnSeconds + 2000 else progressOnSeconds - 2000
        setChannelProgress(forwardProgress){currentProgress(it)}
    }
    fun setChannelProgress(progress:Long, currentProgress:(Long)->Unit){
        val progressBytes = BASS.BASS_ChannelSeconds2Bytes(getActiveChannel(), progress / 1000.0)
         // Ajusta la posición del canal
        BASS.BASS_ChannelSetPosition(getActiveChannel(), progressBytes, BASS.BASS_POS_BYTE)
        currentProgress(progress)
    }

    fun setPlaylist(list: List<SongEntity>){
        _playlist.addAll(list)
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
                //populatePlaylistFinished = true
                listeners.forEach {
                    it.onPlaylistHasPopulated(true)
                }

        }
    }

    fun getDuration():Long{
        return getDuration(getActiveChannel())
    }


    fun getCurrentMediaData(): MediaMetadata?{
       val song=playlist[currentIndexOfSong]
       song?.let {
           return MediaMetadata.Builder()
               .setArtist(it.artist)
               .setTitle(it.title)
               .setAlbumTitle(it.album)
               .build()
       }?:run{
           return null
       }
    }

    fun getPlaybackState():Int{
        return 0
    }
    private fun saveRepeatMode(mode:Int){
        MainApp.mPrefs?.repeatMode=mode
        _repeatMode=mode
    }
    private fun saveShuffleMode(isShuffleMode:Boolean){
        MainApp.mPrefs?.isShuffleMode= isShuffleMode
        _shuffleMode = isShuffleMode
        listeners.forEach {
            it.onShuffleModeEnabledChanged(isShuffleMode)
        }
    }
    fun setActiveChannel(channel:Int){
        mainChannel=channel
    }

    fun repeatSong(){
        BASS.BASS_ChannelPlay(getActiveChannel(), true)
    }

    fun setAbLoopStar(){
        startAbLoopPosition = getCurrentPositionInSeconds(getActiveChannel())
    }
    fun setAbLoopEnd(){
        endAbLopPosition = getCurrentPositionInSeconds(getActiveChannel())
        startAbLoop()
    }
    private fun startAbLoop(){
        val currentPosition = getCurrentPositionInSeconds(getActiveChannel())
        if(currentPosition >= endAbLopPosition){
            BASS.BASS_ChannelSetPosition(getActiveChannel(),getCurrentPositionToBytes(startAbLoopPosition),BASS.BASS_POS_BYTE)
        }
        aBLoopHandler.postDelayed({
            startAbLoop()
        },500)
    }
    fun stopAbLoop() = aBLoopHandler.removeCallbacksAndMessages(null)
    fun getActiveChannel():Int{
        return mainChannel?:0
    }

    private fun getCurrentPositionToBytes(position: Long):Long{
        return if(mainChannel!=null)BASS.BASS_ChannelSeconds2Bytes(mainChannel!!, position / 1000.0)else 0L
    }
    fun getCurrentPositionInSeconds(channel: Int): Long {
        return if(getActiveChannel() !=0)BASS.BASS_ChannelBytes2Seconds(channel, getBytesPosition(channel)).toLong() * 1000 else 0
    }

    fun getDuration(channel: Int): Long {
        return if(getActiveChannel()!=0)BASS.BASS_ChannelBytes2Seconds(channel, getBytesTotal(channel)).toLong() * 1000 else 0
    }

    private fun getBytesPosition(channel:Int): Long {
        return BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
    }
    private fun getBytesTotal(channel: Int): Long {
        return BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE)
    }

    fun addListener(listener: PlaybackManagerListener){
        listeners.add(listener)
    }
    fun removeListener(listener: PlaybackManagerListener){
        listeners.remove(listener)
    }
    fun releasePlayback(){
        BASS.BASS_ChannelStop(getActiveChannel())
        BASS.BASS_PluginFree(0)
        BASS.BASS_Free()
        stopRunnable()
        instance=null
    }
    fun clearBassChannel() {
        mainChannel = null
        BASS.BASS_StreamFree(getActiveChannel())
        BASS.BASS_ChannelSetPosition( getActiveChannel(),getCurrentPositionToBytes(0),BASS.BASS_POS_BYTE)
    }
    interface PlaybackManager{
        fun onFinishPlayback()
    }
}
