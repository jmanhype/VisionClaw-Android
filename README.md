# VisionClaw Android

Android port of [VisionClaw](https://github.com/sseanliu/VisionClaw) — a real-time AI assistant for Meta Ray-Ban smart glasses.

See what you see, hear what you say, and take actions on your behalf — all through voice.

Built on [Meta Wearables DAT SDK (Android)](https://github.com/facebook/meta-wearables-dat-android) + [Gemini Live API](https://ai.google.dev/gemini-api/docs/live) + [OpenClaw](https://github.com/nichochar/openclaw) (optional).

## What It Does

Put on your glasses (or use your phone camera), tap the AI button, and talk:

- **"What am I looking at?"** — Gemini sees through your camera and describes the scene
- **"Add milk to my shopping list"** — delegates to OpenClaw for real-world actions
- **"Send a message to John saying I'll be late"** — routes through OpenClaw
- **"Search for the best coffee shops nearby"** — web search, results spoken back

## Quick Start

### 1. Clone and open

```bash
git clone https://github.com/jmanhype/VisionClaw-Android.git
```

Open in Android Studio.

### 2. Add your Gemini API key

In `app/src/main/java/com/visionclaw/android/gemini/GeminiConfig.kt`:

```kotlin
const val API_KEY = "YOUR_GEMINI_API_KEY"
```

Get a free key at [Google AI Studio](https://aistudio.google.com/apikey).

### 3. Build and run

Select your Android device and hit Run.

### 4. Try it out

**Phone mode (no glasses needed):**
1. Tap "Start on Phone" — uses back camera
2. Tap the AI button to start a Gemini Live session
3. Talk — it sees through your camera and responds with voice

**With Meta Ray-Ban glasses:**
1. Pair glasses via Meta AI app (enable Developer Mode)
2. Tap "Start Streaming"
3. Tap the AI button for voice + vision conversation

## Setup: OpenClaw (Optional)

OpenClaw gives Gemini the ability to take real-world actions. Without it, Gemini is voice + vision only.

In `GeminiConfig.kt`, update:

```kotlin
const val OPENCLAW_HOST = "http://Your-Mac.local"
const val OPENCLAW_PORT = 18789
const val OPENCLAW_GATEWAY_TOKEN = "your-gateway-token-here"
```

See [OpenClaw setup guide](https://github.com/nichochar/openclaw) for gateway configuration.

## Requirements

- Android 10+ (API 29+)
- Android Studio Flamingo+
- Gemini API key ([free](https://aistudio.google.com/apikey))
- Meta Ray-Ban glasses (optional — phone mode for testing)
- OpenClaw on your Mac/PC (optional — for agentic actions)

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full architecture, module map, WebSocket protocol, and audio pipeline details.

## License

Apache 2.0 — See [LICENSE](LICENSE).
