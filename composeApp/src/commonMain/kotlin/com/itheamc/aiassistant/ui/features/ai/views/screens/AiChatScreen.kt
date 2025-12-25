package com.itheamc.aiassistant.ui.features.ai.views.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itheamc.aiassistant.ui.common.AiAssistantAppLogo
import com.itheamc.aiassistant.ui.features.ai.models.ChatMessage
import com.itheamc.aiassistant.ui.features.ai.viewmodels.AiChatViewModel
import com.itheamc.aiassistant.ui.features.ai.views.composables.ChatBubble
import com.itheamc.aiassistant.ui.features.ai.views.composables.ChatInputBar
import com.itheamc.aiassistant.ui.local_providers.LocalNavController
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {


    val viewModel = koinViewModel<AiChatViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val inlineContentId = "logo"

                    Text(
                        text = buildAnnotatedString {
                            append("AI Assistant ")
                            appendInlineContent(inlineContentId, "[logo]")
                        },
                        inlineContent = mapOf(
                            inlineContentId to InlineTextContent(
                                Placeholder(
                                    width = 20.sp,
                                    height = 20.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                AiAssistantAppLogo(
                                    modifier = Modifier.size(20.dp),
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                )
                            }
                        )
                    )
                },
            )
        },
        bottomBar = {
            if (uiState.isModelReady) {
                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSendClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onStopClick = {
                        viewModel.cancel()
                    },
                    isGenerating = uiState.messages.any { it.isPending },
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isDownloadRequired -> {
                ModelDownloadPrompt(
                    modifier = Modifier.padding(paddingValues),
                    onDownloadClick = { viewModel.startDownload() }
                )
            }

            uiState.isLoading -> {
                ModelDownloadProgress(
                    modifier = Modifier.padding(paddingValues),
                    progress = uiState.downloadProgress
                )
            }

            uiState.isModelReady -> {
                Column(
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                        .fillMaxSize()
                ) {
                    if (uiState.error != null) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        items(uiState.messages) { message ->
                            AiChatBubble(message)
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelDownloadPrompt(
    modifier: Modifier = Modifier,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AI Assistant Offline",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To use the AI chat features, you need to download the language model (approx. 550MB). This will allow the AI to work even when you're offline.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDownloadClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download AI Model")
        }
    }
}

@Composable
private fun ModelDownloadProgress(
    modifier: Modifier = Modifier,
    progress: Float
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Downloading AI Model...",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please keep the app open while downloading.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiChatBubble(message: ChatMessage) {
    ChatBubble(
        message = message.copy(
            text = message.text + (if (message.isPending) "..." else "")
        ),
        isContinuationBySameUser = false
    )
}
