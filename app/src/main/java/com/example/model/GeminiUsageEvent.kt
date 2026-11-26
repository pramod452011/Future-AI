package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class GeminiUsageEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val toolType: String = "ai_chat",
    val modelName: String = "gemini-3.8-flash",
    val promptChars: Int = 0,
    val responseChars: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
)
