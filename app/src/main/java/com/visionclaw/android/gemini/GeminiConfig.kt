package com.visionclaw.android.gemini

object GeminiConfig {
    // ---------------------------------------------------------------
    // REQUIRED: Add your Gemini API key here.
    // Get one at https://aistudio.google.com/apikey
    // ---------------------------------------------------------------
    const val API_KEY = "AIzaSyAqucUkA9tKrkUWDInzlKqyqWZ7C2K9oyA"

    const val MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
    const val WEBSOCKET_BASE_URL =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    // Audio config
    const val INPUT_SAMPLE_RATE = 16000
    const val OUTPUT_SAMPLE_RATE = 24000
    const val AUDIO_CHANNELS = 1
    const val AUDIO_BITS_PER_SAMPLE = 16
    const val AUDIO_CHUNK_DURATION_MS = 100

    // Video config
    const val VIDEO_FRAME_INTERVAL_MS = 1000L  // ~1fps
    const val VIDEO_JPEG_QUALITY = 50           // 0-100

    // ---------------------------------------------------------------
    // OPTIONAL: OpenClaw gateway config
    // Only needed if you want Gemini to perform actions via OpenClaw.
    // See README.md for setup instructions.
    // ---------------------------------------------------------------
    const val OPENCLAW_HOST = "http://NewPC.local"
    const val OPENCLAW_PORT = 18789
    const val OPENCLAW_GATEWAY_TOKEN = "18aa0970a8a1009c7bafcc0eaeb44cddcaf3d20daea8cf49"

    val websocketUrl: String
        get() = "$WEBSOCKET_BASE_URL?key=$API_KEY"

    val isConfigured: Boolean
        get() = API_KEY != "YOUR_GEMINI_API_KEY" && API_KEY.isNotEmpty()

    val isOpenClawConfigured: Boolean
        get() = OPENCLAW_GATEWAY_TOKEN != "YOUR_OPENCLAW_GATEWAY_TOKEN"
            && OPENCLAW_GATEWAY_TOKEN.isNotEmpty()
            && !OPENCLAW_HOST.contains("YOUR_MAC_HOSTNAME")

    const val SYSTEM_INSTRUCTION = """
        You are an AI assistant for someone wearing Meta Ray-Ban smart glasses. You can see
        through their camera and have a voice conversation. Keep responses concise and natural.

        CRITICAL: You have NO memory, NO storage, and NO ability to take actions on your own.
        You cannot remember things, keep lists, set reminders, search the web, send messages,
        or do anything persistent. You are ONLY a voice interface.

        You have exactly ONE tool: execute. This connects you to a powerful personal assistant
        that can do anything -- send messages, search the web, manage lists, set reminders,
        create notes, research topics, control smart home devices, interact with apps, and more.

        ALWAYS use execute when the user asks you to:
        - Send a message to someone (any platform)
        - Search or look up anything
        - Add, create, or modify anything (lists, reminders, notes, todos, events)
        - Research, analyze, or draft anything
        - Control or interact with apps, devices, or services
        - Remember or store any information for later

        Be detailed in your task description. Include all relevant context.

        NEVER pretend to do these things yourself.

        IMPORTANT: Before calling execute, ALWAYS speak a brief acknowledgment first.
        Never call execute silently -- the user needs verbal confirmation.
    """
}
