package com.barryzeha.kmusic.ui.screens

import android.annotation.SuppressLint

import android.os.Build

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import com.barryzeha.kmusic.MainApp
import com.barryzeha.kmusic.common.playMediaById
import com.barryzeha.kmusic.common.updatePlaylist
import com.barryzeha.kmusic.data.SongEntity
import com.barryzeha.kmusic.data.toMediaItem
import com.barryzeha.kmusic.ui.components.MiniPlayerView
import com.barryzeha.kmusic.ui.components.Scrollbar
import com.barryzeha.kmusic.ui.components.SongItem
import com.barryzeha.kmusic.ui.navigation.Routes
import com.barryzeha.kmusic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/****
 * Project KMusic
 * Created by Barry Zea H. on 18/04/25.
 * Copyright (c)  All rights reserved.
 ***/

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayListScreen(mediaController: MediaController?, mainViewModel: MainViewModel = viewModel(), navController: NavHostController ){
    val songsList by mainViewModel.songsList.collectAsStateWithLifecycle()
    val songsFiltered by mainViewModel.filteredSongs.collectAsStateWithLifecycle()
    val isSearch by remember{mainViewModel.isSearch}.collectAsStateWithLifecycle()

    val lazyListState = remember{ LazyListState()}

    val textFieldState  = remember { TextFieldState() }
    LaunchedEffect(songsList.isNotEmpty()) {
        mediaController?.updatePlaylist(songsList.map { it.toMediaItem() })
    }
    LaunchedEffect(true){
        mainViewModel.scanSongs()
    }
    Scaffold(
        topBar = {SimpleSearchBar(textFieldState,{}, Modifier, mainViewModel)/*MyToolbar {  }*/},
        content = { padding ->
            Box(Modifier.padding(padding)) {
                Scrollbar(lazyListState, { (it + 1).toString() }, 8.dp) {
                    VerticalRecyclerView(
                        mediaController,
                        if (isSearch) songsFiltered else songsList,
                        Modifier,
                        lazyListState =lazyListState
                    )
                }
            }
        }
        )
}@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun VerticalRecyclerView(
    mediaController: MediaController?,
    songsList: List<SongEntity>,
    modifier: Modifier,
    lazyListState: LazyListState
){
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.padding(bottom = 65.dp),
        state = lazyListState
    ) {
        itemsIndexed(songsList) { index,song->
            SongItem(song) { song ->
                //mediaController?.playMediaAtIndex(index)
                mediaController?.playMediaById(song.idSong.toInt())
                MainApp.mPrefs?.currentIndexSaved = index
            }
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyToolbar(onClick: (String) -> Unit) {
    TopAppBar(title = { Text(text = "Library") },
        modifier = Modifier.background(Color.Transparent)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel
) {
    // Controls expansion state of the search bar
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(
        modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = false }
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(bottom = 8.dp, start = 4.dp, end=4.dp)
                .semantics { traversalIndex = 0f },
            inputField = {

                SearchBarDefaults.InputField(
                    modifier=Modifier.focusRequester(focusRequester),
                    query = textFieldState.text.toString(),
                    onQueryChange = { textFieldState.edit { replace(0, length, it) }
                                    if(textFieldState.text.isEmpty()) {
                                        mainViewModel.setIsSearch(false)
                                    } else {
                                        mainViewModel.setIsSearch(true)
                                        mainViewModel.filteredSong(it)
                                    }
                                    },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        expanded = false
                    },

                    expanded = false,
                    onExpandedChange = { expanded = false },
                    placeholder = { Text("Search") },
                    trailingIcon = {
                        if (textFieldState.text.isNotEmpty()) {
                            IconButton(onClick = {
                                textFieldState.edit {replace(0, length, "")}
                                mainViewModel.setIsSearch(false)
                                expanded = false
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )


            },

            expanded = expanded,
            onExpandedChange = { expanded = it }

        ) {
            // Display search results in a scrollable column
           
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview(){
    SimpleSearchBar(TextFieldState(), {},  mainViewModel = viewModel ())

}
