package com.itheamc.aiassistant.ui.features.splash.views.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itheamc.aiassistant.core.utils.navigate
import com.itheamc.aiassistant.ui.common.AiAssistantAppLogo
import com.itheamc.aiassistant.ui.local_providers.LocalNavController
import com.itheamc.aiassistant.ui.navigation.AiAssistantRoute
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {

    val navController = LocalNavController.current

    // Start animation on composition
    LaunchedEffect(Unit) {
        delay((1500..2000).random().toLong())

        AiAssistantRoute.Onboarding.navigate(
            navController,
            popUpTo = AiAssistantRoute.Splash.name,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiAssistantAppLogo(
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AI Assistant",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Your Private AI Companion",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}