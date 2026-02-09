# TASK: Add Conversation Export Feature to VisionClaw-Android

**Project:** VisionClaw-Android (jplanetx/VisionClaw-Android)  
**Branch:** Create new branch: `feature/conversation-export`  
**Assigned To:** Cursor Pro / Codex  
**Priority:** High  
**Context:** Read this file first, then review existing codebase

---

## Background

VisionClaw is an Android app for Meta Ray-Ban glasses that provides AI vision + voice via Gemini Live API. Currently, conversations are not persisted - they exist only in memory during active sessions.

**User workflow need:**
- Use VisionClaw on mobile (cellular data)
- Have conversations (QA testing, notes, ideas)
- Export conversation transcript
- Share to Telegram → forward to OpenClaw for action execution
- Use for GitHub issue creation, documentation, task capture

---

## Goal

Add conversation logging and export functionality so users can:
1. View full conversation history during and after sessions
2. Export conversations as shareable text
3. Include timestamps and screenshots (where applicable)
4. Share via Android share intent (Telegram, email, clipboard)

---

## Requirements

### Core Features

**1. Conversation Logging**
- Save all user utterances (speech input)
- Save all Gemini responses (text + audio transcripts)
- Include timestamps (ISO 8601 format)
- Tag messages with speaker (USER vs GEMINI)
- Persist to local storage (app's Documents/VisionClaw folder)
- Survive app restart

**2. In-App Conversation View**
- Show conversation history in session screen
- Auto-scroll to latest message
- Display timestamps
- Visual distinction between user and AI messages

**3. Export Functionality**
- "Export Conversation" button in UI
- Generates formatted text file:
  ```
  VisionClaw Session Export
  Date: 2026-02-09 11:30 PST
  Mode: Phone Camera
  Duration: 15 minutes
  
  ---
  
  [11:30:15] USER: What am I looking at?
  [11:30:18] GEMINI: You're looking at a Meta Ray-Ban smart glasses box...
  
  [11:31:42] USER: Add milk to shopping list
  [11:31:43] GEMINI: I'll route that to OpenClaw...
  [TOOL_CALL] execute: Add milk to Jason's shopping list
  
  ---
  End of session
  ```
- Use Android share intent (share to any app)
- Option to copy to clipboard

**4. Session Management**
- Each session gets unique ID (timestamp-based)
- List previous sessions
- Option to delete old sessions
- Option to clear all history

---

## Technical Guidance

### Suggested Architecture

**New Files to Create:**

1. **`data/model/ConversationMessage.kt`**
   ```kotlin
   data class ConversationMessage(
       val id: String,
       val sessionId: String,
       val timestamp: Long,
       val speaker: Speaker,
       val content: String,
       val type: MessageType
   )
   
   enum class Speaker { USER, GEMINI }
   enum class MessageType { TEXT, TOOL_CALL, SYSTEM }
   ```

2. **`data/repository/ConversationRepository.kt`**
   - Save messages to local storage (Room DB or JSON files)
   - Query by session ID
   - Export session as formatted text

3. **`domain/usecase/ExportConversationUseCase.kt`**
   - Format conversation for export
   - Generate shareable text
   - Invoke Android share intent

4. **UI Updates:**
   - `ui/components/ConversationHistoryView.kt` - Display messages
   - `ui/screens/SessionScreen.kt` - Add export button
   - `ui/screens/HistoryScreen.kt` (optional) - List past sessions

**Integration Points:**

- **GeminiLiveService.kt** - Hook into message receive flow to log
- **SessionViewModel.kt** - Add conversation state, export trigger
- **ToolCallRouter.kt** - Log tool calls as special message type

---

## Success Criteria

- [ ] All conversations persisted to local storage
- [ ] In-app history view shows messages with timestamps
- [ ] Export button generates properly formatted text
- [ ] Android share intent opens with conversation text
- [ ] Tool calls are clearly marked in export
- [ ] Works in both Phone and Glasses modes
- [ ] No crashes when exporting long conversations
- [ ] User can clear history

---

## Testing Instructions

1. Start a session, have a conversation (at least 5 exchanges)
2. Trigger a tool call: "Send a message to test"
3. View conversation history in-app
4. Tap "Export Conversation"
5. Share to Files or Telegram
6. Verify exported format is readable
7. Start new session, verify both sessions saved separately
8. Clear history, verify all data removed

---

## Files to Review First

**Existing codebase structure:**
```
app/src/main/java/com/visionclaw/android/
├── gemini/
│   ├── GeminiLiveService.kt        # WebSocket message handling
│   └── GeminiConfig.kt             # Config constants
├── openclaw/
│   └── ToolCallRouter.kt           # Tool call execution
├── ui/
│   ├── screens/SessionScreen.kt    # Main active session UI
│   └── viewmodels/SessionViewModel.kt  # Session state management
├── audio/
│   └── AudioCaptureManager.kt      # Captures user speech
└── MainActivity.kt
```

**Read these files to understand current flow:**
1. `GeminiLiveService.kt` - How messages are received from Gemini
2. `SessionViewModel.kt` - Session lifecycle management
3. `ToolCallRouter.kt` - How tool calls are handled
4. `SessionScreen.kt` - Where to add export button UI

---

## Implementation Notes

**Storage Options:**

**Option A: Room Database** (Recommended for larger apps)
- Persistent, queryable
- Good for future features (search, filters)
- Requires Room dependency

**Option B: JSON Files** (Simpler, faster to implement)
- One JSON file per session
- Stored in app's internal storage
- Easy to read/write, no DB setup

**Choose based on preference.** JSON is faster to implement, Room is more scalable.

**Share Intent Example:**
```kotlin
val shareIntent = Intent().apply {
    action = Intent.ACTION_SEND
    putExtra(Intent.EXTRA_TEXT, conversationText)
    type = "text/plain"
}
startActivity(Intent.createChooser(shareIntent, "Export Conversation"))
```

---

## After Implementation

**Commit message format:**
```
feat: Add conversation export functionality

- Persist all conversation messages to local storage
- Add in-app conversation history view
- Implement export to share intent (Telegram, email, clipboard)
- Include tool calls in export with special formatting
- Add session management (list, delete sessions)

Enables mobile workflow: record conversations on-the-go, export to
OpenClaw via Telegram for task execution.
```

**Branch:** `feature/conversation-export`  
**Push to:** `jplanetx/VisionClaw-Android`  
**After push:** Notify Jason (create PR or merge to main per his preference)

---

## Questions/Blockers

If unclear:
- Storage mechanism choice (Room vs JSON)
- Export format preferences
- UI placement/design

Ask Jason via Clawd (Telegram relay).

---

**Good luck! This is a high-value feature. 🚀**
