package com.itheamc.aiassistant.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itheamc.aiassistant.core.utils.timeAgo

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: String,
    val sender: String? = null,
    val self: Boolean = true,
)

@Composable
fun ChatBubble(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    isContinuationBySameUser: Boolean = false,
) {
    if (message.self) {
        SelfChatBubble(
            message = message.text,
            timestamp = message.timestamp,
            modifier = modifier,
            isContinuationBySameUser = isContinuationBySameUser,
        )
    } else {
        UserChatBubble(
            message = message.text,
            timestamp = message.timestamp,
            sender = message.sender ?: "Sender",
            modifier = modifier,
            isContinuationBySameUser = isContinuationBySameUser
        )
    }
}

/**
 * Composable for other user chat bubble
 */
@Composable
private fun UserChatBubble(
    modifier: Modifier = Modifier,
    message: String,
    timestamp: String,
    sender: String? = null,
    isContinuationBySameUser: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {

            if (isContinuationBySameUser) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                )
            } else {
                ChatUserAvatar(
                    sender = sender,
                    self = false,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                TimestampAndName(
                    timestamp = timestamp,
                    sender = sender,
                    self = false,
                    // showNameAlso = !isContinuationBySameUser,
                    showNameAlso = false
                )

                MessageBubble(
                    message = message,
                    self = false,
                    isContinuationBySameUser = isContinuationBySameUser,
                )
            }
        }
    }
}

/**
 * Composable for self chat bubble
 */
@Composable
private fun SelfChatBubble(
    modifier: Modifier = Modifier,
    message: String,
    timestamp: String,
    sender: String? = null,
    avatarUrl: String? = null,
    isContinuationBySameUser: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.End
            ) {
                TimestampAndName(
                    timestamp = timestamp,
                    sender = sender,
                    self = true,
                    // showNameAlso = !isContinuationBySameUser,
                    showNameAlso = false
                )

                MessageBubble(
                    message = message,
                    self = true,
                    isContinuationBySameUser = isContinuationBySameUser,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isContinuationBySameUser) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                )
            } else {
                ChatUserAvatar(
                    sender = sender,
                    self = true,
                )
            }
        }
    }
}

/**
 * Composable for the chat user avatar
 */
@Composable
private fun RowScope.ChatUserAvatar(
    modifier: Modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .align(Alignment.Bottom),
    sender: String? = null,
    self: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(
                if (self) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.25f
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sender?.first()?.uppercase() ?: if (self) "Y" else "",
            color = if (self) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }

}

/**
 * Composable for the Message Bubble
 */
@Composable
private fun MessageBubble(
    message: String,
    self: Boolean = true,
    isContinuationBySameUser: Boolean = false,
) {

    val bubbleColor =
        if (self) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.25f
        )
    val textColor =
        if (self) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .background(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (self || isContinuationBySameUser) 16.dp else 4.dp,
                    bottomEnd = if (self && !isContinuationBySameUser) 4.dp else 16.dp
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            lineHeight = 22.sp
        )
    }
}

/**
 * Composable for time stamp and name
 */
@Composable
private fun TimestampAndName(
    modifier: Modifier = Modifier.padding(bottom = 4.dp),
    timestamp: String,
    sender: String? = null,
    self: Boolean = true,
    showNameAlso: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (self) {
            Text(
                text = timestamp.timeAgo(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (showNameAlso) {
                Text(
                    text = sender ?: "Unknown",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (self) {
            if (showNameAlso) {
                Text(
                    text = "You",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                text = timestamp.timeAgo(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}