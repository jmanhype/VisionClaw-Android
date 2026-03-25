# VisionClaw-Android

Android client for [VisionClaw](https://github.com/sseanliu/VisionClaw). Connects to Gemini Live API for real-time voice + vision on Meta Ray-Ban smart glasses or a phone camera. Optionally delegates actions to [OpenClaw](https://github.com/nichochar/openclaw).

Built with Kotlin, Jetpack Compose, CameraX, and Hilt.

## Status

Early-stage prototype. The Meta DAT SDK dependency is commented out in `build.gradle.kts` pending GitHub Packages credentials, so glasses streaming is stubbed. Phone-camera mode works.

## What it does

1. Captures camera frames (phone back camera or Meta Ray-Ban stream).
2. Opens a WebSocket to Gemini Live API, sending audio + images.
3. Gemini responds with spoken audio.
4. If OpenClaw is configured, Gemini can route tool calls (add to list, send message, web search) through it.

## Requirements

| Requirement | Version / Notes |
|---|---|
| Android | 10+ (API 29) |
| Android Studio | Flamingo or later |
| Kotlin | 2.0 (via Compose compiler plugin) |
| Gemini API key | Free at [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| Meta Ray-Ban glasses | Optional -- phone camera works for testing |
| OpenClaw server | Optional -- needed only for agentic actions |

## Setup

```bash
git clone https://github.com/jmanhype/VisionClaw-Android.git
```

Open in Android Studio. Set your key in `app/src/main/java/com/visionclaw/android/gemini/GeminiConfig.kt`:

```kotlin
const val API_KEY = "YOUR_GEMINI_API_KEY"
```

Build and run on a physical device (emulator lacks camera + mic).

### OpenClaw (optional)

In `GeminiConfig.kt`:

```kotlin
const val OPENCLAW_HOST = "http://Your-Mac.local"
const val OPENCLAW_PORT = 18789
const val OPENCLAW_GATEWAY_TOKEN = "your-gateway-token-here"
```

See the [OpenClaw repo](https://github.com/nichochar/openclaw) for gateway setup.

## Project structure

```
app/src/main/java/com/visionclaw/android/
  audio/          AudioCaptureManager, AudioPlaybackManager
  camera/         GlassesCameraManager, PhoneCameraManager
  di/             Hilt AppModule
  gemini/         GeminiConfig, GeminiLiveService, GeminiModels
  openclaw/       OpenClawBridge, ToolCallRouter, ToolCallModels
  ui/screens/     MainScreen, SessionScreen (Compose)
  ui/viewmodels/  SessionViewModel
  util/           AudioUtil, ImageUtil
```

## Known limitations

- Meta DAT SDK integration is scaffolded but not wired (dependency commented out).
- No automated tests.
- API key is hardcoded in source rather than injected via build config or secrets.
- Audio pipeline assumes single-channel 16-bit PCM; no codec negotiation.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

Apache 2.0
