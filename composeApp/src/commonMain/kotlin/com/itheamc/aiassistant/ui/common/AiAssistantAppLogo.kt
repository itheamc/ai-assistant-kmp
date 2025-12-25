package com.itheamc.aiassistant.ui.common

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AiAssistantAppLogo(
    modifier: Modifier = Modifier
        .size(60.dp),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
    if (sharedTransitionScope != null && animatedContentScope != null) {
        with(sharedTransitionScope) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .sharedElement(
                        sharedTransitionScope.rememberSharedContentState(key = "app_logo"),
                        animatedVisibilityScope = animatedContentScope
                    ).then(modifier)
            )
        }
    } else {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.then(modifier)
        )
    }
}