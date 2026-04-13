package com.questkeep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.questkeep.domain.entities.ChatMessage
import com.questkeep.domain.entities.MessageType
import com.questkeep.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    when (message.type) {
        MessageType.SYSTEM    -> SystemMessage(message, modifier)
        MessageType.WHISPER   -> WhisperMessage(message, isCurrentUser, modifier)
        MessageType.DICE_ROLL -> DiceRollMessage(message, isCurrentUser, modifier)
        MessageType.IMAGE     -> ImageMessage(message, isCurrentUser, modifier)
        MessageType.NORMAL    -> NormalMessage(message, isCurrentUser, modifier)
    }
}

@Composable
private fun NormalMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    val bubbleColor = if (isCurrentUser) PrimaryPurpleDark else DarkCard

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryPurpleLight,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                AvatarCircle(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            }
            Column {
                Box(
                    modifier = Modifier
                        .background(bubbleColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(text = message.content, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDisabled,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryPurpleLight,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                AvatarCircle(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            }
            Column {
                val url = message.imageUrl
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Изображение",
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDisabled,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DiceRollMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(text = message.senderName, style = MaterialTheme.typography.labelSmall,
                color = PrimaryPurpleLight, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        Box(
            modifier = Modifier
                .background(Color(0xFF2A1A3A), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                color = SecondaryGoldLight,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(text = formatTime(message.createdAt), style = MaterialTheme.typography.labelSmall,
            color = TextDisabled, modifier = Modifier.padding(top = 2.dp, start = 4.dp))
    }
}

@Composable
private fun WhisperMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isCurrentUser)
                "🤫 Шёпот → ${message.targetPlayerName ?: message.targetPlayerId?.take(8) ?: "GM"}"
            else
                "🤫 Шёпот от ${message.senderName}",
            style = MaterialTheme.typography.labelSmall,
            color = AccentOrange
        )
        Box(
            modifier = Modifier
                .background(Color(0xFF2A1800), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.content, color = AccentOrange.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun SystemMessage(message: ChatMessage, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = TextDisabled,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun AvatarCircle(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(PrimaryPurpleDark),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = OnPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
