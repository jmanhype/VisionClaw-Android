# VisionClaw Android — Architecture

## Overview

Android port of [VisionClaw](https://github.com/sseanliu/VisionClaw). Real-time AI assistant
for Meta Ray-Ban smart glasses using Gemini Live API (WebSocket) + OpenClaw (optional tool execution).

## System Diagram

```
Meta Ray-Ban Glasses (or Phone camera)
       |
       | video frames + mic audio
       v
Android App (this project)
       |
       | JPEG frames (~1fps) + PCM audio (16kHz mono, Int16)
       v
Gemini Live API (WebSocket: wss://generativelanguage.googleapis.com/ws/...)
       |
       |-- Audio response (PCM 24kHz mono) --> App --> Speaker
       |-- Tool calls (execute) ------------> App --> OpenClaw Gateway (HTTP)
       |                                                  |
       |                                                  v
       |                                          56+ skills: web search,
       |                                          messaging, smart home, etc.
       |                                                  |
       |<---- Tool response (text) <------- App <---------+
       |
       v
  Gemini speaks the result
```

## Module Map

```
com.visionclaw.android/
├── gemini/
│   ├── GeminiConfig.kt          # API key, model, WebSocket URL, system prompt
│   ├── GeminiLiveService.kt     # WebSocket client (OkHttp) — send/receive frames + audio
│   └── GeminiModels.kt          # Data classes for WS messages (setup, audio, tool calls)
├── audio/
│   ├── AudioCaptureManager.kt   # AudioRecord — PCM Int16, 16kHz mono, 100ms chunks
│   └── AudioPlaybackManager.kt  # AudioTrack — PCM Int16, 24kHz mono playback queue
├── camera/
│   ├── PhoneCameraManager.kt    # CameraX — back camera, throttle to ~1fps JPEG
│   └── GlassesCameraManager.kt  # Meta DAT SDK — video stream from glasses
├── openclaw/
│   ├── OpenClawBridge.kt        # HTTP client (Retrofit/OkHttp) for OpenClaw gateway
│   ├── ToolCallRouter.kt        # Routes Gemini tool calls -> OpenClaw -> responses
│   └── ToolCallModels.kt        # Data classes for tool declarations + results
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt        # Compose — mode selection (phone/glasses), AI button
│   │   └── SessionScreen.kt     # Compose — active session, transcript, visual feedback
│   ├── components/
│   │   ├── AiButton.kt          # Tap-to-talk button with recording state
│   │   └── TranscriptView.kt    # Scrolling conversation transcript
│   └── viewmodels/
│       └── SessionViewModel.kt  # Session lifecycle, state management, wiring
├── di/
│   └── AppModule.kt             # Hilt/Koin dependency injection
└── util/
    ├── ImageUtil.kt             # Bitmap -> JPEG byte array, quality/resize
    └── AudioUtil.kt             # PCM format conversions, buffer sizing
```

## Key Design Decisions

### 1. WebSocket Client: OkHttp
- Gemini Live uses a persistent WebSocket
- OkHttp is the standard Android WebSocket client
- Messages are JSON with base64-encoded binary payloads (audio + JPEG)

### 2. Camera: CameraX
- Phone mode: CameraX back camera, throttled to 1fps
- Glasses mode: Meta DAT SDK `videoFramePublisher` equivalent
- Output: JPEG at 50% quality, base64-encoded for Gemini

### 3. Audio: AudioRecord + AudioTrack
- **Capture**: AudioRecord, PCM Int16, 16kHz mono, 100ms buffer chunks
- **Playback**: AudioTrack, PCM Int16, 24kHz mono, streaming mode
- **Echo cancellation**: Use `AcousticEchoCanceler` (Android audio effect)
- Mute mic during AI speech to prevent feedback

### 4. UI: Jetpack Compose
- Single-activity architecture
- Two screens: MainScreen (mode select) + SessionScreen (active conversation)
- ViewModel manages session state + Gemini service

### 5. OpenClaw Integration: Retrofit
- POST to `http://{host}:{port}/v1/chat/completions`
- Bearer token auth (gateway token)
- Single `execute` tool declaration sent to Gemini in setup message

## Gemini Live WebSocket Protocol

### Connection
```
wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key={API_KEY}
```

### Setup Message (sent on connect)
```json
{
  "setup": {
    "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
    "generationConfig": {
      "responseModalities": ["AUDIO"],
      "speechConfig": {
        "voiceConfig": { "prebuiltVoiceConfig": { "voiceName": "Aoede" } }
      }
    },
    "systemInstruction": { "parts": [{ "text": "..." }] },
    "tools": [{ "functionDeclarations": [...] }]
  }
}
```

### Sending Audio
```json
{
  "realtimeInput": {
    "mediaChunks": [{
      "mimeType": "audio/pcm;rate=16000",
      "data": "<base64 PCM Int16 mono>"
    }]
  }
}
```

### Sending Video Frame
```json
{
  "realtimeInput": {
    "mediaChunks": [{
      "mimeType": "image/jpeg",
      "data": "<base64 JPEG>"
    }]
  }
}
```

### Receiving Audio
```json
{
  "serverContent": {
    "modelTurn": {
      "parts": [{
        "inlineData": {
          "mimeType": "audio/pcm;rate=24000",
          "data": "<base64 PCM Int16 mono>"
        }
      }]
    }
  }
}
```

### Receiving Tool Call
```json
{
  "toolCall": {
    "functionCalls": [{
      "id": "call_123",
      "name": "execute",
      "args": { "task": "Add eggs to shopping list" }
    }]
  }
}
```

### Sending Tool Response
```json
{
  "toolResponse": {
    "functionResponses": [{
      "id": "call_123",
      "response": { "result": "Added eggs to your shopping list" }
    }]
  }
}
```

## Build Dependencies

| Dependency | Purpose |
|-----------|---------|
| OkHttp 4.x | WebSocket client for Gemini Live |
| CameraX 1.3+ | Phone camera capture |
| Jetpack Compose BOM | UI framework |
| Hilt | Dependency injection |
| Moshi/Gson | JSON serialization |
| Meta DAT SDK 0.4.0 | Glasses integration |
| Coroutines | Async audio/video pipelines |

## Audio Pipeline Detail

```
┌─────────────┐    PCM 16kHz    ┌──────────────────┐    base64    ┌─────────────┐
│ AudioRecord  │───────────────>│ AudioCaptureManager│───────────>│ GeminiLive  │
│ (mic)        │   100ms chunks │ (coroutine loop)   │   JSON msg │ WebSocket   │
└─────────────┘                └──────────────────┘              └──────┬──────┘
                                                                       │
                                                                       │ PCM 24kHz
                                                                       v
┌─────────────┐    PCM 24kHz    ┌──────────────────┐    base64    ┌──────────────┐
│ AudioTrack   │<───────────────│AudioPlaybackManager│<───────────│ GeminiLive   │
│ (speaker)    │   write()      │ (queue + drain)    │   decode   │ WebSocket    │
└─────────────┘                └──────────────────┘              └──────────────┘
```

## Testing Strategy

1. **Mock Device** — Meta DAT SDK includes mock device for testing without glasses
2. **Phone mode first** — Get camera + Gemini working with phone camera before glasses
3. **Audio loopback** — Test mic capture -> playback locally before WebSocket
4. **Emulator** — Camera and audio work on emulator with virtual devices
