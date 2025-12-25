package com.itheamc.aiassistant.ui.features.ai.views.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type your message...",
    isGenerating: Boolean = false,
) {

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                )
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field with placeholder
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = { if (!isGenerating) onTextChange(it) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    keyboardActions = KeyboardActions(
                        onSend = { if (isGenerating) onStopClick() else onSendClick() },
                    ),
                )
            }

            // Send button with theme color
            IconButton(
                onClick = { if (isGenerating) onStopClick() else onSendClick() },
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .size(36.dp)
                    .background(
                        color = if (isGenerating) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                shape = RoundedCornerShape(36.dp)
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isGenerating) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
