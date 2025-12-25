package com.itheamc.aiassistant.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun <T> DebouncedAction(
    input: () -> T,
    debounceMillis: Long = 10L,
    action: suspend (T) -> Unit
) {
    LaunchedEffect(input()) {
        snapshotFlow { input() }
            .debounce(debounceMillis)
            .collect { value ->
                action(value)
            }
    }
}