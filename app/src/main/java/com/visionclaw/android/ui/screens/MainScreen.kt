package com.visionclaw.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Main screen — mode selection and session start.
 *
 * TODO: Implement Compose UI:
 * - "Start on Phone" button -> launches SessionScreen in PHONE mode
 * - "Start with Glasses" button -> launches SessionScreen in GLASSES mode
 * - Show Gemini config status (key configured? OpenClaw configured?)
 * - Request camera + microphone permissions
 */
@Composable
fun MainScreen(
    onStartPhone: () -> Unit = {},
    onStartGlasses: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VisionClaw",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI Assistant for Smart Glasses",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartPhone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start on Phone")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onStartGlasses,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start with Glasses")
        }
    }
}
