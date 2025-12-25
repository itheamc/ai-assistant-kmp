package com.itheamc.aiassistant.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Enum class for ai assistant app button type
 */
enum class AiAssistantAppButtonType { Normal, Outlined, Text }

@Composable
fun AiAssistantAppButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    buttonType: AiAssistantAppButtonType = AiAssistantAppButtonType.Normal,
    enabled: Boolean = true,
    secondary: Boolean = false,
    loading: Boolean = false
) {
    when (buttonType) {
        AiAssistantAppButtonType.Normal -> {
            Button(
                onClick = { onClick?.invoke() },
                enabled = !loading && enabled,
                modifier = modifier
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = if (secondary) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) else ButtonDefaults.buttonColors()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (secondary) MaterialTheme.colorScheme.onSurface else ButtonDefaults.buttonColors().contentColor
                    )
                } else {
                    leading?.invoke()
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
                trailing?.invoke()
            }
        }

        AiAssistantAppButtonType.Outlined -> {
            OutlinedButton(
                onClick = { onClick?.invoke() },
                enabled = !loading && enabled,
                modifier = modifier
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = if (secondary) ButtonDefaults.outlinedButtonColors(
                    contentColor = ButtonDefaults.outlinedButtonColors().contentColor.copy(
                        alpha = 0.85f
                    ),
                ) else ButtonDefaults.outlinedButtonColors()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (secondary) ButtonDefaults.outlinedButtonColors().contentColor else MaterialTheme.colorScheme.primary
                    )
                } else {
                    leading?.invoke()
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (secondary) ButtonDefaults.outlinedButtonColors().contentColor else MaterialTheme.colorScheme.primary
                    ),
                )
                trailing?.invoke()
            }
        }

        AiAssistantAppButtonType.Text -> {
            TextButton(
                onClick = { onClick?.invoke() },
                enabled = !loading && enabled,
                modifier = modifier
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = if (secondary) ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) else ButtonDefaults.textButtonColors()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (secondary) MaterialTheme.colorScheme.onSurfaceVariant else ButtonDefaults.textButtonColors().contentColor
                    )
                } else {
                    leading?.invoke()
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
                trailing?.invoke()
            }
        }
    }
}