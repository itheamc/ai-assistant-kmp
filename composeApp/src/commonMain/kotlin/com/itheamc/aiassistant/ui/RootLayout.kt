package com.itheamc.aiassistant.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.itheamc.aiassistant.ui.local_providers.LocalNavController
import com.itheamc.aiassistant.ui.navigation.AiAssistantNavHost
import com.itheamc.aiassistant.ui.navigation.AiAssistantRoute

@Composable
fun RootLayout(modifier: Modifier = Modifier) {

    val navController = LocalNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedRoute by remember { mutableStateOf<AiAssistantRoute?>(null) }

    // Track current route for bottom navigation selection
    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            selectedRoute = AiAssistantRoute.fromStr(backStackEntry.destination.route)
        }
    }

    // Listen to app lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {}
                Lifecycle.Event.ON_START -> {}
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup observer when composable is disposed
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
    ) { _ ->
        AiAssistantNavHost(
            modifier = modifier
                .fillMaxSize(),
            navController = navController,
            startDestination = AiAssistantRoute.Splash
        )
    }
}