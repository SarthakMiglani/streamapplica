package com.example.streamapp

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.streamapp.screens.VideoPlayerPage

object AppRoutes {
    const val WELCOME = "welcome"
    const val VIDEO_PLAYER = "videoPlayer"
    const val VIDEO_PLAYER_WITH_URL = "videoPlayer/{streamUrl}"

    fun createVideoPlayerRoute(streamUrl: String): String {

        val encodedUrl = Uri.encode(streamUrl)
        return "videoPlayer/$encodedUrl"
    }
}



@Composable
fun AppNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onEnterPip: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.WELCOME,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(AppRoutes.WELCOME) {
            WelcomePage(
                navController = navController
            )
        }

        composable(AppRoutes.VIDEO_PLAYER_WITH_URL) { backStackEntry ->
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            VideoPlayerPage(
                streamUrl = streamUrl,
                onEnterPip = onEnterPip
            )
        }
    }
}
