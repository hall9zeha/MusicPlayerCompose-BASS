package com.barryzeha.kmusic.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import com.barryzeha.kmusic.MainApp
import com.barryzeha.kmusic.common.MediaControllerUtil
import com.barryzeha.kmusic.common.PlayerState
import com.barryzeha.kmusic.common.scanTracks
import com.barryzeha.kmusic.data.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/****
 * Project KMusic
 * Created by Barry Zea H. on 22/04/25.
 * Copyright (c)  All rights reserved.
 ***/

class MainViewModel(private val application: Application): AndroidViewModel(application) {
    private var _songsList: MutableStateFlow<List<SongEntity>> = MutableStateFlow(listOf())
    val songsList: StateFlow<List<SongEntity>> = _songsList
    
    // For search filter
    private var _filteredSongs: MutableStateFlow<List<SongEntity>> = MutableStateFlow(listOf())
    val filteredSongs: StateFlow<List<SongEntity>> = _filteredSongs
    // End region

    private var _mediaController: MediaControllerUtil = MediaControllerUtil.getInstance(MainApp.context!!)
    val mediaController: MediaControllerUtil get() =  _mediaController

    private var _controller: MutableState<MediaController?> = mutableStateOf(null)
    val controller: State<MediaController?> get() = _controller

    private var _playerScreenIsActive: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val playerScreenIsActive: StateFlow<Boolean> get() = _playerScreenIsActive

    private var _playerState: MutableLiveData<PlayerState?> = MutableLiveData(null)
    val playerState: LiveData<PlayerState?>  get() = _playerState

    private var _isSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSearch: StateFlow<Boolean> = _isSearch

    private var _hasInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val hasInitialized: StateFlow<Boolean> = _hasInitialized

    init {
        setUpController()
        setUpState()

    }
    fun scanSongs(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _songsList.value = scanTracks(application.applicationContext)!!
            }
        }
    }
    fun filteredSong(input: String){
        _filteredSongs.value = songsList.value.filter{song->
            song.title.contains(input, ignoreCase = true )||
                    song.artist.contains(input, ignoreCase = true) ||
                    song.album.contains(input, ignoreCase = true)
        }
    }

    fun setUpState(){
        viewModelScope.launch {
            _mediaController.state.collect { statePlayer ->
                _playerState.value = statePlayer
            }
        }
    }
    fun setUpController(){
        viewModelScope.launch {
            _controller = _mediaController.controller
        }
    }
    fun setIsSearch(isSearch:Boolean){
        _isSearch.value = isSearch
    }
    fun setHasInitialized(isInitialized: Boolean){
        _hasInitialized.value = isInitialized
    }

    override fun onCleared() {
        mediaController.release()
        super.onCleared()

    }
    fun setPlayerScreenVisibility(isVisible: Boolean){
        viewModelScope.launch {
            delay(300)
            _playerScreenIsActive.value = isVisible
        }
    }

}