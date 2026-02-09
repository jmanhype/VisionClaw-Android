package com.visionclaw.android.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.squareup.moshi.Moshi
import com.visionclaw.android.data.model.ConversationMessage
import com.visionclaw.android.data.model.ConversationSession
import com.visionclaw.android.data.model.MessageType
import com.visionclaw.android.data.model.Speaker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists conversation messages to local storage (JSON files per session).
 * Storage path: app's Documents/VisionClaw folder.
 */
@Singleton
class ConversationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "ConversationRepository"
        private const val FOLDER_NAME = "VisionClaw"
        private const val SESSION_PREFIX = "session_"
        private const val SESSION_SUFFIX = ".json"
    }

    private val storageDir: File
        get() {
            val docs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val base = docs ?: context.filesDir
            return File(base, FOLDER_NAME).also { if (!it.exists()) it.mkdirs() }
        }

    private val mutex = Mutex()
    private val _currentSessionMessages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val currentSessionMessages: StateFlow<List<ConversationMessage>> = _currentSessionMessages

    private var currentSessionId: String? = null

    private val sessionAdapter by lazy {
        moshi.adapter(SessionFile::class.java)
    }

    /** Session file format for JSON persistence */
    private data class SessionFile(
        val id: String,
        val startTimeMillis: Long,
        val mode: String,
        val messages: List<ConversationMessage>
    )

    /**
     * Start a new session and set it as current. Call when user starts a session.
     */
    suspend fun startSession(sessionId: String, mode: String) = mutex.withLock {
        currentSessionId = sessionId
        val session = ConversationSession(id = sessionId, startTimeMillis = System.currentTimeMillis(), mode = mode)
        val file = SessionFile(session.id, session.startTimeMillis, session.mode, emptyList())
        writeSessionFile(sessionId, file)
        _currentSessionMessages.value = emptyList()
    }

    /**
     * End current session (clear in-memory current id; data already persisted).
     */
    fun endSession() {
        currentSessionId = null
        _currentSessionMessages.value = emptyList()
    }

    /**
     * Add a message to the current session and persist.
     */
    suspend fun addMessage(message: ConversationMessage) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sid = currentSessionId ?: return@withContext
            if (message.sessionId != sid) return@withContext
            val file = readSessionFile(sid) ?: return@withContext
            val updated = file.messages + message
            writeSessionFile(sid, file.copy(messages = updated))
            _currentSessionMessages.value = if (currentSessionId == sid) updated else _currentSessionMessages.value
        }
    }

    /**
     * Add a user utterance (e.g. "[User spoke]" when no transcript available).
     */
    suspend fun addUserMessage(sessionId: String, content: String) {
        addMessage(
            ConversationMessage(
                id = "msg_${System.currentTimeMillis()}_${(0..9999).random()}",
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = Speaker.USER,
                content = content.ifBlank { "[User spoke]" },
                type = MessageType.TEXT
            )
        )
    }

    /**
     * List all stored sessions, newest first.
     */
    suspend fun getAllSessions(): List<ConversationSession> = withContext(Dispatchers.IO) {
        mutex.withLock {
            storageDir.listFiles()?.filter { it.name.startsWith(SESSION_PREFIX) && it.name.endsWith(SESSION_SUFFIX) }
                ?.mapNotNull { f ->
                    runCatching {
                        val file = readSessionFileFromFile(f) ?: return@mapNotNull null
                        ConversationSession(file.id, file.startTimeMillis, file.mode)
                    }.getOrNull()
                }
                ?.sortedByDescending { it.startTimeMillis }
                ?: emptyList()
        }
    }

    /**
     * Get messages for a session (for export or history view).
     */
    suspend fun getMessages(sessionId: String): List<ConversationMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readSessionFile(sessionId)?.messages?.sortedBy { it.timestamp } ?: emptyList()
        }
    }

    /**
     * Get session metadata by id.
     */
    suspend fun getSession(sessionId: String): ConversationSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readSessionFile(sessionId)?.let {
                ConversationSession(it.id, it.startTimeMillis, it.mode)
            }
        }
    }

    /**
     * Delete one session and its file.
     */
    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessionFile(sessionId).delete()
            if (currentSessionId == sessionId) endSession()
        }
    }

    /**
     * Clear all sessions (delete all session files).
     */
    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        mutex.withLock {
            storageDir.listFiles()?.filter { it.name.startsWith(SESSION_PREFIX) && it.name.endsWith(SESSION_SUFFIX) }
                ?.forEach { it.delete() }
            currentSessionId = null
            _currentSessionMessages.value = emptyList()
        }
    }

    /**
     * Format a session as export text (ISO 8601 timestamps in header, [HH:mm:ss] in body per requirement).
     */
    suspend fun formatSessionForExport(sessionId: String): String? = withContext(Dispatchers.IO) {
        val session = getSession(sessionId) ?: return@withContext null
        val messages = getMessages(sessionId)
        val startTime = session.startTimeMillis
        val endTime = messages.maxOfOrNull { it.timestamp } ?: startTime
        val durationMinutes = (endTime - startTime) / (60 * 1000)
        val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm z", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date(startTime))
        val timeOnlyFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        val sb = StringBuilder()
        sb.appendLine("VisionClaw Session Export")
        sb.appendLine("Date: $isoDate")
        sb.appendLine("Mode: ${session.mode}")
        sb.appendLine("Duration: $durationMinutes minutes")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        for (msg in messages) {
            val timeStr = timeOnlyFormat.format(java.util.Date(msg.timestamp))
            when (msg.type) {
                MessageType.TOOL_CALL -> sb.appendLine("[$timeStr] [TOOL_CALL] ${msg.content}")
                else -> sb.appendLine("[$timeStr] ${msg.speaker.name}: ${msg.content}")
            }
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("End of session")
        return@withContext sb.toString()
    }

    private fun sessionFile(sessionId: String): File = File(storageDir, "$SESSION_PREFIX$sessionId$SESSION_SUFFIX")

    private fun readSessionFile(sessionId: String): SessionFile? = readSessionFileFromFile(sessionFile(sessionId))

    private fun readSessionFileFromFile(file: File): SessionFile? {
        if (!file.exists()) return null
        return runCatching {
            file.readText().let { json ->
                sessionAdapter.fromJson(json)
            }
        }.onFailure { Log.e(TAG, "Read session failed: ${file.name}", it) }.getOrNull()
    }

    private fun writeSessionFile(sessionId: String, sessionFile: SessionFile) {
        val file = sessionFile(sessionId)
        runCatching {
            file.writeText(sessionAdapter.toJson(sessionFile))
        }.onFailure { Log.e(TAG, "Write session failed: $sessionId", it) }
    }
}
