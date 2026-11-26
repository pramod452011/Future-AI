package com.example.viewmodel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.FutureAiRepository
import com.example.data.deleteAccountWithReauthIfNeeded
import com.example.model.ChatHistoryEntry
import com.example.model.GeminiUsageEvent
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val ACCOUNT_TAG = "AccountViewModel"

sealed interface AccountUiState<out T> {
    data object Loading : AccountUiState<Nothing>
    data class Success<T>(val data: T) : AccountUiState<T>
    data class Error(val message: String) : AccountUiState<Nothing>
}

sealed interface GeminiConnectionTestState {
    data object Idle : GeminiConnectionTestState
    data object Testing : GeminiConnectionTestState
    data class Connected(
        val latencyMs: Long,
        val modelName: String,
        val sampleReply: String,
    ) : GeminiConnectionTestState
    data class Error(val message: String) : GeminiConnectionTestState
}

class AccountViewModel(
    private val repository: FutureAiRepository,
) : ViewModel() {

    private val pingModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isSavingSettings = MutableStateFlow(false)
    val isSavingSettings: StateFlow<Boolean> = _isSavingSettings.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _connectionTestState =
        MutableStateFlow<GeminiConnectionTestState>(GeminiConnectionTestState.Idle)
    val connectionTestState: StateFlow<GeminiConnectionTestState> =
        _connectionTestState.asStateFlow()

    init {
        Firebase.auth.currentUser?.let { user ->
            viewModelScope.launch {
                repository.ensureUserProfile(user)
            }
        }
    }

    val profileState: StateFlow<AccountUiState<UserProfile?>> =
        repository.observeUserProfile()
            .map<UserProfile?, AccountUiState<UserProfile?>> { profile ->
                AccountUiState.Success(profile)
            }
            .catch { cause ->
                emit(
                    AccountUiState.Error(
                        cause.localizedMessage ?: "प्रोफ़ाइल लोड करने में समस्या आई।",
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AccountUiState.Loading,
            )

    val chatHistoryState: StateFlow<AccountUiState<List<ChatHistoryEntry>>> =
        repository.observeChatHistory()
            .map<List<ChatHistoryEntry>, AccountUiState<List<ChatHistoryEntry>>> { list ->
                AccountUiState.Success(list)
            }
            .catch { cause ->
                emit(
                    AccountUiState.Error(
                        cause.localizedMessage ?: "चैट हिस्ट्री लोड करने में समस्या आई।",
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AccountUiState.Loading,
            )

    val usageState: StateFlow<AccountUiState<List<GeminiUsageEvent>>> =
        repository.observeUsageEvents()
            .map<List<GeminiUsageEvent>, AccountUiState<List<GeminiUsageEvent>>> { list ->
                AccountUiState.Success(list)
            }
            .catch { cause ->
                emit(
                    AccountUiState.Error(
                        cause.localizedMessage ?: "Gemini Usage लोड करने में समस्या आई।",
                    ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AccountUiState.Loading,
            )

    fun saveChatExchange(prompt: String, response: String, toolType: String = "ai_chat") {
        viewModelScope.launch {
            repository.saveChatEntry(prompt = prompt, response = response, toolType = toolType)
            repository.recordGeminiUsage(
                toolType = toolType,
                modelName = "gemini-3.8-flash",
                promptChars = prompt.length,
                responseChars = response.length,
            )
        }
    }

    fun recordGeminiUsage(
        toolType: String,
        modelName: String = "gemini-3.8-flash",
        promptChars: Int,
        responseChars: Int,
    ) {
        viewModelScope.launch {
            repository.recordGeminiUsage(
                toolType = toolType,
                modelName = modelName,
                promptChars = promptChars,
                responseChars = responseChars,
            )
        }
    }

    fun runGeminiConnectionTest() {
        if (_connectionTestState.value is GeminiConnectionTestState.Testing) return
        viewModelScope.launch {
            _connectionTestState.value = GeminiConnectionTestState.Testing
            val prompt = "Reply in one short polite Hindi sentence confirming FUTURE AI Gemini server backend is connected."
            val start = SystemClock.elapsedRealtime()
            try {
                val result = withTimeout(30_000L) {
                    pingModel.generateContent(prompt)
                }
                val elapsed = SystemClock.elapsedRealtime() - start
                val reply = result.text?.trim().orEmpty()
                if (reply.isEmpty()) {
                    _connectionTestState.value = GeminiConnectionTestState.Error(
                        "Gemini सर्वर से खाली उत्तर मिला। कृपया पुनः प्रयास करें।",
                    )
                } else {
                    _connectionTestState.value = GeminiConnectionTestState.Connected(
                        latencyMs = elapsed,
                        modelName = "gemini-3.8-flash",
                        sampleReply = reply,
                    )
                    repository.recordGeminiUsage(
                        toolType = "connection_test",
                        modelName = "gemini-3.8-flash",
                        promptChars = prompt.length,
                        responseChars = reply.length,
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(ACCOUNT_TAG, "Gemini connection test timed out", e)
                _connectionTestState.value = GeminiConnectionTestState.Error(
                    "कनेक्शन टेस्ट में 30 सेकंड से अधिक समय लगा। कृपया नेटवर्क चेक करें।",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(ACCOUNT_TAG, "Gemini connection test failed", e)
                _connectionTestState.value = GeminiConnectionTestState.Error(
                    e.localizedMessage ?: "Gemini सर्वर से जुड़ने में समस्या आई।",
                )
            }
        }
    }

    fun deleteChatEntry(chatId: String) {
        viewModelScope.launch {
            val result = repository.deleteChatEntry(chatId)
            _actionMessage.value = if (result.isSuccess) {
                "चैट हिस्ट्री से हटा दी गई।"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "चैट हटाने में समस्या आई।"
            }
        }
    }

    fun updateSettings(
        displayName: String,
        preferredLanguage: String,
        glowBoostEnabled: Boolean,
    ) {
        viewModelScope.launch {
            _isSavingSettings.value = true
            val result = repository.updateUserSettings(
                displayName = displayName,
                preferredLanguage = preferredLanguage,
                glowBoostEnabled = glowBoostEnabled,
            )
            _isSavingSettings.value = false
            _actionMessage.value = if (result.isSuccess) {
                "आपकी सेटिंग्स क्लाउड में सेव हो गईं!"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "सेटिंग्स सेव करने में समस्या आई।"
            }
        }
    }

    fun deleteAccount(activityContext: Context) {
        if (_isDeletingAccount.value) return
        viewModelScope.launch {
            _isDeletingAccount.value = true
            val result = deleteAccountWithReauthIfNeeded(
                activityContext = activityContext,
                deleteCloudData = { repository.deleteAllUserData() },
            )
            _isDeletingAccount.value = false
            result.onFailure { error ->
                _actionMessage.value = error.localizedMessage
                    ?: "अकाउंट हटाने में समस्या आई। कृपया पुनः प्रयास करें।"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]
                    ?: error("Application context required for AccountViewModel")
                val db = FirebaseFirestore.getInstance(
                    app.getString(R.string.firestore_database_id),
                )
                AccountViewModel(FutureAiRepository(db))
            }
        }
    }
}
