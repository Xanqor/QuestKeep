package com.questkeep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    val t = LocalXanteya.current
    val bubbleColor = if (isCurrentUser) t.colors.panel else t.colors.panel.copy(alpha = 0.85f)
    val borderColor = if (isCurrentUser) t.colors.accent else t.colors.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = t.colors.accent,
                fontWeight = FontWeight.SemiBold,
                fontFamily = t.display,
                modifier = Modifier.padding(start = 40.dp, bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                AvatarCircle(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            }
            Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
                val bubbleShape = when (t.id) {
                    "falloran" -> TornBottom
                    "nirventhal" -> OrganicPanel
                    "tsingate" -> CutCornerShape
                    else -> RoundedCornerShape(
                        topStart = t.corner,
                        topEnd = t.corner,
                        bottomStart = if (isCurrentUser) t.corner else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else t.corner
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .border(1.dp, borderColor, bubbleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 290.dp)
                ) {
                    Column {
                        if (t.id == "falloran") {
                            Text(
                                text = "ТЕЛЕГРАММА",
                                color = t.colors.muted,
                                fontSize = 9.sp,
                                fontFamily = t.display,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = message.content,
                            color = t.colors.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = t.body,
                            lineHeight = 20.sp
                        )
                    }
                }
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.colors.muted,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    val t = LocalXanteya.current
    val borderColor = if (isCurrentUser) t.colors.accent else t.colors.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = t.colors.accent,
                fontFamily = t.display,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 40.dp, bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                AvatarCircle(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            }
            Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
                Card(
                    shape = RoundedCornerShape(t.corner),
                    colors = CardDefaults.cardColors(containerColor = t.colors.panel),
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(t.corner))
                ) {
                    Column {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = message.content.ifEmpty { "Изображение" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 240.dp)
                                .clip(RoundedCornerShape(topStart = t.corner, topEnd = t.corner))
                        )
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = t.colors.text,
                                fontFamily = t.body,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.colors.muted,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DiceRollMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    val t = LocalXanteya.current
    val borderColor = t.colors.accent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = t.colors.accent,
                fontFamily = t.display,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 40.dp, bottom = 2.dp)
            )
        }
        Row(
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                AvatarCircle(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            }
            Surface(
                shape = RoundedCornerShape(t.corner),
                color = t.colors.panel,
                modifier = Modifier
                    .border(1.5.dp, borderColor, RoundedCornerShape(t.corner))
                    .widthIn(max = 280.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Casino,
                        contentDescription = "Бросок кубика",
                        tint = t.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = message.content,
                            color = t.colors.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formatTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = t.colors.muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhisperMessage(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier) {
    val t = LocalXanteya.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(t.corner),
            color = t.colors.panel,
            modifier = Modifier
                .border(1.dp, t.colors.hot.copy(alpha = 0.6f), RoundedCornerShape(t.corner))
                .widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Шёпот",
                    tint = t.colors.hot,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (isCurrentUser) "Шёпот для ${message.targetPlayerName ?: "ГМ"}" else "Шёпот от ${message.senderName}",
                        fontSize = 11.sp,
                        color = t.colors.hot,
                        fontFamily = t.display,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = message.content,
                        color = t.colors.text,
                        fontFamily = t.body,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMessage(message: ChatMessage, modifier: Modifier) {
    val t = LocalXanteya.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = t.colors.panel.copy(alpha = 0.6f),
            shape = RoundedCornerShape(t.corner),
            modifier = Modifier.border(0.5.dp, t.colors.line, RoundedCornerShape(t.corner))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = t.colors.muted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = message.content,
                    color = t.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = t.body,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun AvatarCircle(name: String, modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(t.colors.panel)
            .border(1.dp, t.colors.line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = t.colors.accent,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
