package com.itheamc.aiassistant.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AiAssistantNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: AiAssistantRoute = AiAssistantRoute.Splash
) {

    SharedTransitionLayout(
        modifier = modifier
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination.name
        ) {

            composable(
                route = AiAssistantRoute.Splash.name,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(200)) }
            ) {

            }

            composable(
                route = AiAssistantRoute.Onboarding.name,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(200)) }
            ) {

            }

            composable(
                route = AiAssistantRoute.AiChat.name,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(200)) }
            ) {

            }
        }
    }
}
