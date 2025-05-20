package com.barryzeha.kmusic

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.barryzeha.kmusic.common.checkPermissions
import com.barryzeha.kmusic.ui.components.MiniPlayerView
import com.barryzeha.kmusic.ui.navigation.Routes
import com.barryzeha.kmusic.ui.screens.PlayListScreen
import com.barryzeha.kmusic.ui.screens.PlayerScreen
import com.barryzeha.kmusic.ui.theme.KMusicTheme
import com.barryzeha.kmusic.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var launcherPermission: ActivityResultLauncher<String>
    private lateinit var navController:NavHostController

    private val permissionsList: MutableList<String> = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
        mutableListOf(
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )
    }else{
        mutableListOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityResultForPermission()

        setContent {
            LaunchedEffect(true) {
                initCheckPermissions()
            }
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current.lifecycle

            var mediaControllerInstance by remember{mutableStateOf(mainViewModel.mediaController)}
            val mediaController  by mainViewModel.controller
            val playerScreenIsActive by mainViewModel.playerScreenIsActive.collectAsState()
            val playerState by  mainViewModel.playerState.observeAsState()
            val hasInitialized by mainViewModel.hasInitialized.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver{_, event->
                    when(event){
                        Lifecycle.Event.ON_STOP->{
                            MainApp.mPrefs?.currentSongDuration = playerState?.currentPosition!!
                        }
                        Lifecycle.Event.ON_START->{
                            playerState?.registerListener()
                            mediaControllerInstance.initialize()
                        }
                        Lifecycle.Event.ON_DESTROY->{
                            // MediaController resources are released when the ViewModel is destroyed
                            //mediaControllerInstance.release()
                        }
                        else->{}
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(key1 = playerState) {
                mediaController?.run {
                    playerState?.registerListener()
                    // We load our track only the first time we start the application if we have saved one that we have played, otherwise we load index zero by default
                    if(!hasInitialized){
                        playerState?.let{player->
                            val newIndex = if(MainApp.mPrefs?.currentIndexSaved!! > -1) MainApp.mPrefs?.currentIndexSaved!! else 0
                            val currentProgressDuration= MainApp.mPrefs?.currentSongDuration
                            player.mediaItemIndex =newIndex
                            player.player.seekTo(newIndex,currentProgressDuration!!)
                            player.player.prepare()
                        }
                    }
                    mainViewModel.setHasInitialized(true)
                }
                onDispose {
                    playerState?.unregisterListener()
                }
            }

            KMusicTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    innerPadding.calculateTopPadding()
                    SetupNavigation(mediaController)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    ) {
                        if (playerState !=null && !playerScreenIsActive) {
                            MiniPlayerView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .align(Alignment.BottomCenter)
                                    .clickable {
                                        coroutineScope.launch {
                                            mainViewModel.setPlayerScreenVisibility(true)
                                            navController.navigate(Routes.Player.route)

                                        }
                                    },
                                playerState = playerState!!
                            )
                        }
                    }
                }
            }

        }
    }
    private fun activityResultForPermission(){
        launcherPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {isGranted->
              initCheckPermissions()
        }
    }
    private fun initCheckPermissions(){
        checkPermissions(this,permissionsList) {isAllGranted,permissions->
            if(!isAllGranted){
                permissions.forEach { (permission, isGranted)->
                    if(!isGranted) launcherPermission.launch(permission)
                }
            }else{
            }
        }
    }
    @Composable
    fun SetupNavigation(mediaController: MediaController?){
        navController = rememberNavController()
        NavHost(navController, startDestination=Routes.Playlist.route){
            composable(Routes.Playlist.route){
                PlayListScreen(mediaController, navController = navController)
            }
            composable(Routes.Player.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            400, easing = LinearEasing
                        )
                    )+ slideIntoContainer(
                        animationSpec = tween(400, easing = EaseIn),
                        towards = AnimatedContentTransitionScope.SlideDirection.Up
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            500, easing = LinearEasing
                        )
                    ) + slideOutOfContainer(
                        animationSpec = tween(500, easing = EaseOut),
                        towards = AnimatedContentTransitionScope.SlideDirection.Down
                    )
                }
                ){
                PlayerScreen(mainViewModel = mainViewModel, navController = navController)
            }
        }
    }
}

