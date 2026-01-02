package com.itheamc.aiassistant.ui.features.onboarding.views.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itheamc.aiassistant.core.utils.navigate
import com.itheamc.aiassistant.ui.common.AiAssistantAppButton
import com.itheamc.aiassistant.ui.common.AiAssistantAppLogo
import com.itheamc.aiassistant.ui.local_providers.LocalNavController
import com.itheamc.aiassistant.ui.navigation.AiAssistantRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {

    val navController = LocalNavController.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    AiAssistantAppLogo(
                        modifier = Modifier.size(64.dp),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                    )
                }
                
                Text(
                    text = "Welcome to AI Assistant",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Experience the power of Generative AI directly on your device. Fast, secure, and completely private.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No internet connection required for chat.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                AiAssistantAppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Get Started",
                    onClick = {
                        AiAssistantRoute.AiChat.navigate(
                            navController,
                            popUpTo = AiAssistantRoute.Onboarding.name,
                        )
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                AiAssistantAppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Try Agentic Features",
                    secondary = true,
                    onClick = {
                        AiAssistantRoute.AgentShowcase.navigate(
                            navController,
                            popUpTo = AiAssistantRoute.Onboarding.name,
                        )
                    },
                )
            }
        }
    }
}