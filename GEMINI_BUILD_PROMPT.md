# VisionClaw Android — Full Implementation Prompt

> Copy everything below this line and paste it into Gemini as your opening prompt.
> Point Gemini at the repo: https://github.com/jmanhype/VisionClaw-Android

---

You are implementing VisionClaw-Android, an Android port of the iOS app VisionClaw (https://github.com/sseanliu/VisionClaw). This is a real-time AI assistant for Meta Ray-Ban smart glasses that uses YOUR API — the Gemini Live WebSocket API — for bidirectional voice + vision conversation, with optional tool execution via OpenClaw.

The repo is already scaffolded with full project structure, build files, data classes, and placeholder implementations. Every file that needs implementation has `TODO()` markers. Your job is to implement every TODO across all phases, in order, producing working code.

## Repo Structure

```
VisionClaw-Android/
├── app/src/main/java/com/visionclaw/android/
│   ├── gemini/
│   │   ├── GeminiConfig.kt          ✅ Complete (API key, model, URLs, system prompt)
│   │   ├── GeminiModels.kt          ✅ Complete (all Moshi data classes for WS protocol)
│   │   └── GeminiLiveService.kt     🔨 TODO: WebSocket client
│   ├── audio/
│   │   ├── AudioCaptureManager.kt   🔨 TODO: AudioRecord mic capture
│   │   └── AudioPlaybackManager.kt  🔨 TODO: AudioTrack speaker playback
│   ├── camera/
│   │   ├── PhoneCameraManager.kt    🔨 TODO: CameraX frame capture
│   │   └── GlassesCameraManager.kt  🔨 TODO: Meta DAT SDK (Phase 4)
│   ├── openclaw/
│   │   ├── ToolCallModels.kt        ✅ Complete (execute tool declaration)
│   │   ├── OpenClawBridge.kt        🔨 TODO: HTTP client
│   │   └── ToolCallRouter.kt        🔨 TODO: Route tool calls
│   ├── ui/
│   │   ├── screens/MainScreen.kt    🔨 TODO: Permissions + navigation
│   │   ├── screens/SessionScreen.kt 🔨 TODO: Camera preview + AI button
│   │   └── viewmodels/SessionViewModel.kt 🔨 TODO: Wire all components
│   ├── di/AppModule.kt              ✅ Complete (OkHttp + Moshi providers)
│   ├── util/ImageUtil.kt            🔨 TODO: YUV -> JPEG conversion
│   ├── util/AudioUtil.kt            ✅ Complete (base64 encode/decode)
│   ├── VisionClawApp.kt             ✅ Complete (Hilt application)
│   └── MainActivity.kt              🔨 TODO: Navigation setup
├── app/src/main/AndroidManifest.xml  ✅ Complete
├── app/build.gradle.kts              ✅ Complete
├── build.gradle.kts                  ✅ Complete
├── settings.gradle.kts               ✅ Complete
├── gradle/libs.versions.toml         ✅ Complete
└── docs/ARCHITECTURE.md              ✅ Complete (full protocol reference)
```

## Implementation Phases

Execute these in order. After each phase, verify it compiles.

### PHASE 1: Gemini Live WebSocket (GeminiLiveService.kt)

This is the critical path. Implement the OkHttp WebSocket client.

**connect():**
1. Build OkHttp Request to `GeminiConfig.websocketUrl`
2. Create WebSocket via `okHttpClient.newWebSocket(request, listener)`
3. In `onOpen`: send the setup message (JSON):
```json
{
  "setup": {
    "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
    "generationConfig": {
      "responseModalities": ["AUDIO"],
      "speechConfig": {
        "voiceConfig": {
          "prebuiltVoiceConfig": { "voiceName": "Aoede" }
        }
      }
    },
    "systemInstruction": {
      "parts": [{ "text": "<system instruction from GeminiConfig>" }]
    },
    "tools": [{
      "functionDeclarations": [{
        "name": "execute",
        "description": "Execute a task using the connected personal assistant...",
        "parameters": {
          "type": "object",
          "properties": {
            "task": { "type": "string", "description": "Detailed task description..." }
          },
          "required": ["task"]
        }
      }]
    }]
  }
}
```
4. In `onMessage`: parse JSON, detect message type:
   - `setupComplete` → update state to CONNECTED
   - `serverContent.modelTurn.parts[].inlineData` (audio) → decode base64, send to audioOutputChannel
   - `serverContent.turnComplete` → signal end of AI speech
   - `toolCall.functionCalls` → send each to toolCallChannel
5. In `onFailure`/`onClosing` → update state, log error

**sendAudio(pcmData):**
```json
{
  "realtimeInput": {
    "mediaChunks": [{
      "mimeType": "audio/pcm;rate=16000",
      "data": "<base64 encoded pcmData>"
    }]
  }
}
```

**sendVideoFrame(jpegData):**
```json
{
  "realtimeInput": {
    "mediaChunks": [{
      "mimeType": "image/jpeg",
      "data": "<base64 encoded jpegData>"
    }]
  }
}
```

**sendToolResponse(callId, result):**
```json
{
  "toolResponse": {
    "functionResponses": [{
      "id": "<callId>",
      "response": { "result": "<result>" }
    }]
  }
}
```

Use Moshi with the data classes in GeminiModels.kt for serialization. Use `android.util.Base64` for encoding.

### PHASE 2: Audio Pipeline

**AudioCaptureManager.startCapture():**
1. Create `AudioRecord` with source `MediaRecorder.AudioSource.VOICE_COMMUNICATION` (enables AEC)
2. Sample rate: 16000, channel: MONO, encoding: PCM_16BIT
3. Apply `AcousticEchoCanceler.create(audioRecord.audioSessionId)` if available
4. Launch coroutine loop:
   - `audioRecord.read(buffer, 0, CHUNK_SIZE)` — reads 3200 bytes (100ms)
   - If not muted: `onChunk(buffer.copyOf(bytesRead))`
5. Set `_isRecording.value = true`

**AudioPlaybackManager.startPlayback():**
1. Create `AudioTrack` in streaming mode:
   - Usage: `AudioAttributes.USAGE_MEDIA`
   - Content type: `CONTENT_TYPE_SPEECH`
   - Sample rate: 24000, channel: MONO, encoding: PCM_16BIT
2. `audioTrack.play()`
3. Launch coroutine that receives from `audioChannel`:
   - `onPlaybackStateChanged?.invoke(true)` when first chunk arrives
   - `audioTrack.write(chunk, 0, chunk.size)`
   - `onPlaybackStateChanged?.invoke(false)` when channel is empty/idle

### PHASE 3: Camera (PhoneCameraManager.kt)

**startCapture():**
1. Get `ProcessCameraProvider.getInstance(context)`
2. Create `ImageAnalysis` use case with `STRATEGY_KEEP_ONLY_LATEST`
3. Set analyzer:
   - Check `System.currentTimeMillis() - lastFrameTime >= VIDEO_FRAME_INTERVAL_MS`
   - If yes: convert `ImageProxy` to JPEG bytes using `ImageUtil.imageToJpeg()`
   - Call `onFrame(jpegBytes)`
   - Update `lastFrameTime`
   - Always call `imageProxy.close()`
4. Optionally bind a `Preview` use case for the UI
5. `cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, ...)`

**ImageUtil.imageToJpeg():**
- Get YUV planes from Image
- Convert YUV_420_888 to NV21 byte array
- Create `YuvImage(nv21, ImageFormat.NV21, width, height, null)`
- `yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, outputStream)`
- Return outputStream bytes

### PHASE 4: OpenClaw Integration

**OpenClawBridge.executeTask():**
1. Build URL: `${GeminiConfig.OPENCLAW_HOST}:${GeminiConfig.OPENCLAW_PORT}/v1/chat/completions`
2. Build JSON body:
```json
{
  "messages": [{ "role": "user", "content": "<task>" }],
  "model": "openclaw"
}
```
3. Create OkHttp Request with:
   - POST method
   - `Content-Type: application/json`
   - `Authorization: Bearer ${GeminiConfig.OPENCLAW_GATEWAY_TOKEN}`
4. Execute synchronously (already on IO dispatcher)
5. Parse response JSON, extract `choices[0].message.content`
6. Return `Result.success(content)` or `Result.failure(exception)`

**ToolCallRouter.handleToolCall():**
1. Extract `call.args["task"]`
2. If task is null, send error response to Gemini
3. Call `openClawBridge.executeTask(task)`
4. On success: `geminiLiveService.sendToolResponse(call.id, result)`
5. On failure: `geminiLiveService.sendToolResponse(call.id, "Error: ${error.message}")`

### PHASE 5: ViewModel + UI Wiring

**SessionViewModel.startSession():**
1. Set sessionState = CONNECTING
2. Connect GeminiLiveService
3. Start AudioCaptureManager with `onChunk = { geminiService.sendAudio(it) }`
4. Start AudioPlaybackManager consuming `geminiService.audioOutputChannel`
5. Wire playback state to mic muting: `audioPlaybackManager.onPlaybackStateChanged = { audioCaptureManager.setMuted(it) }`
6. Start PhoneCameraManager with `onFrame = { geminiService.sendVideoFrame(it) }`
7. Launch coroutine consuming `geminiService.toolCallChannel`:
   - For each call: `toolCallRouter.handleToolCall(call)`
8. Collect `geminiService.connectionState` and update sessionState

**SessionViewModel.stopSession():**
1. Disconnect GeminiLiveService
2. Stop AudioCaptureManager
3. Stop AudioPlaybackManager
4. Stop PhoneCameraManager
5. Set sessionState = IDLE

**MainScreen:**
1. Add runtime permission requests for CAMERA + RECORD_AUDIO (use `rememberLauncherForActivityResult`)
2. Show config status badges (Gemini configured, OpenClaw configured)
3. Navigate to SessionScreen on button tap

**SessionScreen:**
1. Add CameraX `PreviewView` wrapped in `AndroidView` composable
2. Large circular AI button (tap to start/stop session)
3. Connection status indicator (color-coded: gray/yellow/green/red)
4. Optional: scrolling transcript of conversation

**MainActivity:**
1. Set up NavHost with two routes: "main" and "session/{mode}"
2. Pass navigation callbacks to MainScreen

### PHASE 6: Meta DAT SDK (GlassesCameraManager.kt)

This requires the Meta DAT SDK dependency. In `gradle.properties`, set:
```
gpr.user=<github_username>
gpr.token=<github_token_with_read:packages>
```

Then uncomment the meta-dat lines in `app/build.gradle.kts`.

Implement using the DAT SDK's video streaming API — reference: https://wearables.developer.meta.com/docs/build-integration-android/

For testing without glasses, use the Mock Device Kit from the SDK.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on connected device
./gradlew connectedAndroidTest   # Run instrumented tests
```

## Critical Notes

- The `GeminiModels.kt` data classes are ALREADY COMPLETE with Moshi annotations. Use them directly.
- The `ToolCallModels.kt` execute tool declaration is ALREADY COMPLETE. Reference it in the setup message.
- `GeminiConfig.kt` has the API key and all config values pre-set. Don't change them.
- `AppModule.kt` provides OkHttpClient and Moshi via Hilt. Inject them, don't create new instances.
- The OkHttp WebSocket read timeout is set to 0 (infinite) — this is intentional for the persistent connection.
- Audio capture uses VOICE_COMMUNICATION source, NOT MIC — this enables hardware echo cancellation.
- Always close `ImageProxy` in the CameraX analyzer, even if you skip the frame.
- Mute the mic during AI speech playback to prevent feedback loops.

## When Done

After all phases compile and run:
1. Test phone mode: camera + voice conversation with Gemini
2. Test tool calling: ask Gemini to do something (requires OpenClaw running)
3. Push to the repo
4. The project owner will do a code review via Claude Opus

Start with Phase 1 (GeminiLiveService.kt). Read the existing file first, then implement all TODO methods.
