package com.visionclaw.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.visionclaw.android.gemini.GeminiLiveService
import com.visionclaw.android.ui.components.ConversationHistoryView
import com.visionclaw.android.ui.viewmodels.SessionViewModel
import kotlinx.coroutines.launch

/**
 * Active session screen — camera, conversation history, export, AI button.
 */
@Composable
fun SessionScreen(
    mode: SessionViewModel.SessionMode = SessionViewModel.SessionMode.PHONE,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val conversationMessages by viewModel.conversationMessages.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(mode) {
        viewModel.startSession(mode)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Camera Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (mode == SessionViewModel.SessionMode.PHONE) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            viewModel.bindCamera(lifecycleOwner, previewView.surfaceProvider)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Glasses Mode (Camera from device)", color = Color.White)
            }
        }

        // Conversation history
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            if (conversationMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Conversation will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ConversationHistoryView(messages = conversationMessages)
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                GeminiLiveService.ConnectionState.CONNECTED -> Color.Green
                                GeminiLiveService.ConnectionState.CONNECTING -> Color.Yellow
                                GeminiLiveService.ConnectionState.ERROR -> Color.Red
                                GeminiLiveService.ConnectionState.DISCONNECTED -> Color.Gray
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Status: ${connectionState.name}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export & Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val text = viewModel.getExportTextForCurrentSession()
                            if (text != null) {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Conversation"))
                            }
                        }
                    },
                    enabled = conversationMessages.isNotEmpty()
                ) {
                    Text("Export Conversation")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val text = viewModel.getExportTextForCurrentSession()
                            if (text != null) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("VisionClaw export", text))
                            }
                        }
                    },
                    enabled = conversationMessages.isNotEmpty()
                ) {
                    Text("Copy")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Button (Start/Stop)
            Button(
                onClick = {
                    if (sessionState == SessionViewModel.SessionState.ACTIVE ||
                        sessionState == SessionViewModel.SessionState.CONNECTING) {
                        viewModel.stopSession()
                    } else {
                        viewModel.startSession(mode)
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionState == SessionViewModel.SessionState.ACTIVE)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (sessionState == SessionViewModel.SessionState.ACTIVE) "STOP" else "GO"
                )
            }
        }
    }
}
