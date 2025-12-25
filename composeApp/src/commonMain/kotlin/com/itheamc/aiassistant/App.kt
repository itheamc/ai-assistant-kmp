package com.itheamc.aiassistant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.itheamc.aiassistant.core.di.koinConfig
import com.itheamc.aiassistant.ui.RootLayout
import com.itheamc.aiassistant.ui.local_providers.LocalNavController
import com.itheamc.aiassistant.ui.theme.AiAssistantAppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinMultiplatformApplication
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App(
    onKoinConfigured: () -> Unit = {},
) {
    KoinMultiplatformApplication(
        config = koinConfig
    ) {
        AiAssistantApp(
            onKoinConfigured = onKoinConfigured,
        )
    }
}

@Composable
private fun AiAssistantApp(
    onKoinConfigured: () -> Unit = {},
) {

    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        onKoinConfigured.invoke()
    }

    CompositionLocalProvider(
        LocalNavController provides navController,
    ) {
        AiAssistantAppTheme {
            RootLayout(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}