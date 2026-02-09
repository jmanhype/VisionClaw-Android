package com.visionclaw.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.visionclaw.android.data.model.ConversationMessage
import com.visionclaw.android.data.model.MessageType
import com.visionclaw.android.data.model.Speaker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays conversation messages with timestamps and visual distinction between user and AI.
 * Auto-scrolls to the latest message.
 */
@Composable
fun ConversationHistoryView(
    messages: List<ConversationMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(message = msg)
        }
    }
}

@Composable
private fun MessageBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))
    val isUser = message.speaker == Speaker.USER
    val isToolCall = message.type == MessageType.TOOL_CALL

    val backgroundColor = when {
        isToolCall -> MaterialTheme.colorScheme.tertiaryContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start)
    {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(12.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = "[$timeStr] ${message.speaker.name}${if (isToolCall) " [TOOL_CALL]" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
