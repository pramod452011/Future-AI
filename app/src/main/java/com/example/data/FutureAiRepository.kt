package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChatHistoryEntry
import com.example.model.GeminiUsageEvent
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FutureAiRepository(
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
    )

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("उपयोगकर्ता साइन-इन नहीं है।")

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    private fun chatsCollection(uid: String) = userDocRef(uid).collection("chats")

    private fun usageCollection(uid: String) = userDocRef(uid).collection("usage")

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val snapshot = userDocRef(uid).get().await()
            if (!snapshot.exists()) {
                val safeDisplayName = user.displayName
                    ?.trim()
                    ?.take(80)
                    ?.ifEmpty { null }
                    ?: user.email?.substringBefore("@")?.take(80)?.ifEmpty { null }
                    ?: "Future AI User"
                val safeEmail = user.email?.trim()?.take(150).orEmpty()
                val safePhotoUrl = user.photoUrl?.toString()?.trim()?.take(500)?.ifEmpty { null }

                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "photoUrl" to safePhotoUrl,
                    "preferredLanguage" to "Hindi",
                    "glowBoostEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }

                userDocRef(uid).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> {
        val uid = requireUserId()
        val path = "users/$uid"
        return userDocRef(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                } else {
                    null
                }
            }
            .catch { cause ->
                if (cause is Exception) {
                    handleFirestoreError(cause, OperationType.GET, path)
                }
                throw cause
            }
    }

    suspend fun updateUserSettings(
        displayName: String,
        preferredLanguage: String,
        glowBoostEnabled: Boolean,
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val trimmedName = displayName.trim().take(80)
            require(trimmedName.isNotEmpty()) { "नाम खाली नहीं हो सकता।" }
            val safeLang = if (preferredLanguage in ALLOWED_LANGUAGES) preferredLanguage else "Hindi"

            val updates = mapOf<String, Any>(
                "displayName" to trimmedName,
                "preferredLanguage" to safeLang,
                "glowBoostEnabled" to glowBoostEnabled,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            userDocRef(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeChatHistory(): Flow<List<ChatHistoryEntry>> {
        val uid = requireUserId()
        val path = "users/$uid/chats"
        return chatsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        ChatHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
            }
            .catch { cause ->
                if (cause is Exception) {
                    handleFirestoreError(cause, OperationType.LIST, path)
                }
                throw cause
            }
    }

    suspend fun saveChatEntry(
        prompt: String,
        response: String,
        toolType: String = "ai_chat",
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/chats"
        return try {
            val safePrompt = prompt.trim().take(4000)
            val safeResponse = response.trim().take(16000)
            if (safePrompt.isEmpty() || safeResponse.isEmpty()) {
                return Result.success(Unit)
            }
            val safeTitle = safePrompt.replace("\n", " ").take(80).ifEmpty { "AI Chat" }
            val safeTool = if (toolType in ALLOWED_TOOLS) toolType else "ai_chat"

            val payload = mapOf<String, Any>(
                "userId" to uid,
                "title" to safeTitle,
                "prompt" to safePrompt,
                "response" to safeResponse,
                "toolType" to safeTool,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            chatsCollection(uid).add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatEntry(chatId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/chats/$chatId"
        return try {
            chatsCollection(uid).document(chatId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeUsageEvents(): Flow<List<GeminiUsageEvent>> {
        val uid = requireUserId()
        val path = "users/$uid/usage"
        return usageCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        GeminiUsageEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
            }
            .catch { cause ->
                if (cause is Exception) {
                    handleFirestoreError(cause, OperationType.LIST, path)
                }
                throw cause
            }
    }

    suspend fun recordGeminiUsage(
        toolType: String,
        modelName: String,
        promptChars: Int,
        responseChars: Int,
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/usage"
        return try {
            val safeTool = if (toolType in ALLOWED_USAGE_TOOLS) toolType else "ai_chat"
            val safeModel = modelName.trim().take(60).ifEmpty { "gemini-3.8-flash" }
            val safePromptChars = promptChars.coerceIn(0, 20_000)
            val safeResponseChars = responseChars.coerceIn(0, 50_000)

            val payload = mapOf<String, Any>(
                "userId" to uid,
                "toolType" to safeTool,
                "modelName" to safeModel,
                "promptChars" to safePromptChars,
                "responseChars" to safeResponseChars,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            usageCollection(uid).add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes all private chat history documents, Gemini usage logs, and the user profile document.
     */
    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = requireUserId()
        val userPath = "users/$uid"
        return try {
            val chatsSnapshot = chatsCollection(uid).get().await()
            chatsSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            val usageSnapshot = usageCollection(uid).get().await()
            usageSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            userDocRef(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userPath)
            Result.failure(e)
        }
    }

    private companion object {
        val ALLOWED_LANGUAGES = setOf("Hindi", "Hinglish", "English")
        val ALLOWED_TOOLS = setOf(
            "ai_chat",
            "ai_voice_assistant",
            "ai_writer",
            "ai_translator",
            "ai_video",
            "ai_music",
        )
        val ALLOWED_USAGE_TOOLS = setOf(
            "ai_chat",
            "ai_voice_assistant",
            "ai_writer",
            "ai_translator",
            "ai_image",
            "ai_video",
            "ai_music",
            "ai_voice",
            "connection_test",
        )
    }
}
