package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiChatViewModel"

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false,
)

sealed interface ChatStatus {
    data object Idle : ChatStatus
    data object Loading : ChatStatus
    data object Streaming : ChatStatus
    data class Error(val message: String, val retryPrompt: String) : ChatStatus
}

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val status: ChatStatus = ChatStatus.Idle,
)

class AiChatViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are FUTURE AI, a helpful, friendly, and intelligent personal AI assistant. " +
                        "Reply naturally in the same language the user writes in (Hindi, Hinglish, or English). " +
                        "Keep responses clear, well-structured, concise, and easy to read on a mobile phone.",
                )
            },
        )
    }

    private var chatSession: Chat? = null

    private fun getOrCreateChat(): Chat {
        return chatSession ?: model.startChat().also { chatSession = it }
    }

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun sendMessage(
        rawPrompt: String,
        onExchangeCompleted: ((prompt: String, reply: String) -> Unit)? = null,
    ) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return
        if (_uiState.value.status is ChatStatus.Loading || _uiState.value.status is ChatStatus.Streaming) {
            return
        }

        val userMessage = ChatMessage(text = prompt, isFromUser = true)
        val assistantMessageId = UUID.randomUUID().toString()

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + ChatMessage(
                    id = assistantMessageId,
                    text = "",
                    isFromUser = false,
                    isStreaming = true,
                ),
                status = ChatStatus.Loading,
            )
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    val chat = getOrCreateChat()
                    chat.sendMessageStream(prompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            val snapshot = accumulated.toString()
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(text = snapshot, isStreaming = true)
                                        } else {
                                            msg
                                        }
                                    },
                                    status = ChatStatus.Streaming,
                                )
                            }
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(text = finalReply, isStreaming = false)
                                } else {
                                    msg
                                }
                            },
                            status = ChatStatus.Idle,
                        )
                    }
                    onExchangeCompleted?.invoke(prompt, finalReply)
                } else {
                    removeEmptyAssistantPlaceholder(assistantMessageId)
                    _uiState.update { state ->
                        state.copy(
                            status = ChatStatus.Error(
                                message = "Gemini ने खाली उत्तर भेजा। कृपया दोबारा प्रयास करें।",
                                retryPrompt = prompt,
                            ),
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat timed out", e)
                removeEmptyAssistantPlaceholder(assistantMessageId)
                _uiState.update { state ->
                    state.copy(
                        status = ChatStatus.Error(
                            message = "जवाब आने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                            retryPrompt = prompt,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat generation failed", e)
                removeEmptyAssistantPlaceholder(assistantMessageId)
                _uiState.update { state ->
                    state.copy(
                        status = ChatStatus.Error(
                            message = e.localizedMessage ?: "AI से जुड़ने में समस्या आई। कृपया पुनः प्रयास करें।",
                            retryPrompt = prompt,
                        ),
                    )
                }
            }
        }
    }

    fun retryLastFailedMessage(
        onExchangeCompleted: ((prompt: String, reply: String) -> Unit)? = null,
    ) {
        val errorState = _uiState.value.status as? ChatStatus.Error ?: return
        val retryText = errorState.retryPrompt
        // Remove the duplicated user bubble before resending cleanly
        _uiState.update { state ->
            val trimmed = if (state.messages.lastOrNull()?.isFromUser == true &&
                state.messages.last().text == retryText
            ) {
                state.messages.dropLast(1)
            } else {
                state.messages
            }
            state.copy(messages = trimmed, status = ChatStatus.Idle)
        }
        sendMessage(retryText, onExchangeCompleted)
    }

    fun dismissError() {
        _uiState.update { state ->
            if (state.status is ChatStatus.Error) {
                state.copy(status = ChatStatus.Idle)
            } else {
                state
            }
        }
    }

    fun startNewChat() {
        chatSession = null
        _uiState.value = AiChatUiState()
    }

    private fun removeEmptyAssistantPlaceholder(assistantMessageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id == assistantMessageId && it.text.isBlank() }
                    .map { msg ->
                        if (msg.id == assistantMessageId) msg.copy(isStreaming = false) else msg
                    },
            )
        }
    }
}
