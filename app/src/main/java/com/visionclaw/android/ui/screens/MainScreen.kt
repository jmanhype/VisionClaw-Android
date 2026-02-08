package com.visionclaw.android.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.visionclaw.android.gemini.GeminiConfig

/**
 * Main screen — mode selection and session start.
 */
@Composable
fun MainScreen(
    onStartPhone: () -> Unit = {},
    onStartGlasses: () -> Unit = {}
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) {
            onStartPhone()
        }
    }

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

        // Status Badges
        StatusBadge("Gemini API", GeminiConfig.isConfigured)
        Spacer(modifier = Modifier.height(8.dp))
        StatusBadge("OpenClaw Gateway", GeminiConfig.isOpenClawConfigured)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = GeminiConfig.isConfigured
        ) {
            Text("Start on Phone")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onStartGlasses,
            modifier = Modifier.fillMaxWidth(),
            enabled = false // Not implemented yet
        ) {
            Text("Start with Glasses")
        }
    }
}

@Composable
fun StatusBadge(label: String, isConfigured: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (isConfigured) "✅" else "❌",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "$label: ${if (isConfigured) "Configured" else "Missing"}",
            color = if (isConfigured) Color.Green else Color.Red
        )
    }
}
