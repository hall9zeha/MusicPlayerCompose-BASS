package com.barryzeha.kmusic.ui.screens

import android.annotation.SuppressLint

import android.os.Build

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.barryzeha.kmusic.MainApp
import com.barryzeha.kmusic.playback.BassManager
import com.barryzeha.kmusic.common.playMediaById
import com.barryzeha.kmusic.data.SongEntity
import com.barryzeha.kmusic.ui.components.Scrollbar
import com.barryzeha.kmusic.ui.components.SongItem
import com.barryzeha.kmusic.ui.viewmodel.MainViewModel

/****
 * Project KMusic
 * Created by Barry Zea H. on 18/04/25.
 * Copyright (c)  All rights reserved.
 ***/

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayListScreen(mediaController: BassManager?, mainViewModel: MainViewModel = viewModel(), navController: NavHostController ){
    val songsList by mainViewModel.songsList.collectAsStateWithLifecycle()
    val songsFiltered by mainViewModel.filteredSongs.collectAsStateWithLifecycle()
    val isSearch by remember{mainViewModel.isSearch}.collectAsStateWithLifecycle()
    val searchIsActive  = rememberSaveable {mutableStateOf(false)}

    val permissionsGranted by mainViewModel.permissionsGranted.collectAsState()
    val listState by mainViewModel.lazyListState.collectAsState()
    val loadLibrary by remember {mainViewModel.loadLibrary}.collectAsStateWithLifecycle()


    //Menu expanded
    var menuExpanded by rememberSaveable {mutableStateOf(false) }

    //val lazyListState = remember { listState}
    val lazyListState = rememberLazyListState(0)
    val textFieldState  = remember { TextFieldState() }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                if (searchIsActive.value) {
                    SimpleSearchBar(
                        textFieldState,
                        {},
                        Modifier.weight(1f),
                        mainViewModel,
                        searchIsActive
                    )
                }
                if(loadLibrary){
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF6200EE),
                        trackColor = Color.LightGray
                    )
                }
                if(!searchIsActive.value) {
                    Text(text="Library",
                        modifier=Modifier.weight(1f).padding(start=8.dp),
                        textAlign = TextAlign.Start,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Box(contentAlignment = Alignment.CenterEnd) {
                        IconButton(
                            onClick = {
                                searchIsActive.value = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                    Box(contentAlignment = Alignment.CenterEnd) {
                        IconButton(
                            onClick = {
                                menuExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded=false
                            }
                        ) {
                            DropdownMenuItem(
                                text={Text("Reload library")},
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload library"
                                    )
                                },
                                onClick = {
                                    menuExpanded=false
                                    mainViewModel.resyncLibrary()
                                }
                            )
                        }

                    }
                }
            }},
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
    mediaController: BassManager?,
    songsList: List<SongEntity>,
    modifier: Modifier,
    lazyListState: LazyListState
){
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.padding(bottom = 65.dp),
        state = lazyListState
    ) {
        itemsIndexed(songsList,key={
            _,song->song.idSong
        }) { index,song->
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
    mainViewModel: MainViewModel,
    searchIsActive: MutableState<Boolean>
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
                        //if (textFieldState.text.isNotEmpty()) {
                            IconButton(onClick = {
                                textFieldState.edit {replace(0, length, "")}
                                mainViewModel.setIsSearch(false)
                                expanded = false
                                focusManager.clearFocus()
                                searchIsActive.value= false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        //}
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
    SimpleSearchBar(
        TextFieldState(),
        {},
        mainViewModel = viewModel (),
        searchIsActive = mutableStateOf(false)
    )

}
