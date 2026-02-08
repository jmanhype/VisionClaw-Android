package com.visionclaw.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.visionclaw.android.ui.viewmodels.SessionViewModel

/**
 * Active session screen — shows AI button, transcript, visual feedback.
 *
 * TODO: Implement Compose UI:
 * - Camera preview (CameraX PreviewView)
 * - AI button (tap to start/stop voice session)
 * - Connection status indicator
 * - Scrolling transcript of conversation
 * - Visual feedback for mic active / AI speaking
 */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TODO: Camera preview area (takes most of the screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera Preview")
        }

        // TODO: AI button + status
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Status: $connectionState")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // TODO: Toggle session
                }
            ) {
                Text("AI Button")
            }
        }
    }
}
