package com.visionclaw.android.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.visionclaw.android.gemini.GeminiLiveService
import com.visionclaw.android.ui.viewmodels.SessionViewModel

/**
 * Active session screen — shows AI button, transcript, visual feedback.
 */
@Composable
fun SessionScreen(
    mode: SessionViewModel.SessionMode = SessionViewModel.SessionMode.PHONE,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

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
