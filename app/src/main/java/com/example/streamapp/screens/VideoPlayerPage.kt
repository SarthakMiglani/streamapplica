package com.example.streamapp.screens

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.streamapp.vlc.VlcPlayer

@Composable
fun VideoPlayerPage(
    streamUrl: String,
    modifier: Modifier = Modifier,
    onEnterPip: () -> Unit,
    isPiPMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vlcPlayer = remember { VlcPlayer(context) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var hasAttachedSurface by remember { mutableStateOf(false) }

    vlcPlayer.onError = { msg ->
        errorMessage = msg
        isLoading = false
    }
    vlcPlayer.onPlaying = {
        isLoading = false
    }


    LaunchedEffect(surfaceView) {
        if (!vlcPlayer.isInitialized) {
            vlcPlayer.initialize()
        }

        surfaceView?.let { surface ->
            if (!hasAttachedSurface) {
                vlcPlayer.attachSurface(surface)
                hasAttachedSurface = true
            }

            vlcPlayer.playStream(streamUrl)
            isLoading = true
        }
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> vlcPlayer.stopPlayback()
                Lifecycle.Event.ON_DESTROY -> vlcPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vlcPlayer.release()
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )


        errorMessage?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Show loading spinner while buffering
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }


        if (!isPiPMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Now playing: $streamUrl",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            var isRecording by remember { mutableStateOf(false) }

            // Buttons and other UI controls, only show them when NOT in PiP mode
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (!isRecording) {
                            isRecording = vlcPlayer.startRecording()
                        } else {
                            vlcPlayer.stopRecording()
                            isRecording = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else Color.Green
                    )
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }

            //  (PiP) button
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            onEnterPip()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Enter PiP Mode")
                }
            }
        }
    }
}
