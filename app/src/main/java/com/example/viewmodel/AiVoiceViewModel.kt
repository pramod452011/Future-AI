package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiVoiceViewModel"

data class VoiceLocaleOption(
    val id: String,
    val label: String,
    val locale: Locale,
)

object VoiceLocaleOptions {
    val all: List<VoiceLocaleOption> = listOf(
        VoiceLocaleOption("hi_in", "हिंदी (भारत)", Locale("hi", "IN")),
        VoiceLocaleOption("en_in", "English (India)", Locale("en", "IN")),
        VoiceLocaleOption("en_us", "English (US)", Locale.US),
        VoiceLocaleOption("en_gb", "English (UK)", Locale.UK),
    )
}

data class AiVoiceUiState(
    val text: String = "",
    val selectedVoice: VoiceLocaleOption = VoiceLocaleOptions.all.first(),
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isPolishing: Boolean = false,
    val errorMessage: String? = null,
)

class AiVoiceViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow(AiVoiceUiState())
    val uiState: StateFlow<AiVoiceUiState> = _uiState.asStateFlow()

    fun updateText(newText: String) {
        _uiState.update { it.copy(text = newText, errorMessage = null) }
    }

    fun selectVoice(option: VoiceLocaleOption) {
        _uiState.update { it.copy(selectedVoice = option) }
    }

    fun updateSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate.coerceIn(0.5f, 2.0f)) }
    }

    fun updatePitch(pitch: Float) {
        _uiState.update { it.copy(pitch = pitch.coerceIn(0.6f, 1.6f)) }
    }

    fun polishForVoiceover() {
        val current = _uiState.value
        val source = current.text.trim()
        if (source.isEmpty() || current.isPolishing) return

        val targetLang = if (current.selectedVoice.id == "hi_in") "Hindi" else "English"
        val prompt = buildString {
            append("You are FUTURE AI Voice Script Writer.\n")
            append("Rewrite the following text into a smooth, natural, expressive spoken voiceover script in $targetLang.\n")
            append("Keep it concise (under 60 words), plain spoken text with no markdown, no emojis, and no stage directions.\n\n")
            append("Input: $source")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPolishing = true, errorMessage = null) }
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                val polished = response.text?.trim().orEmpty()
                if (polished.isNotEmpty()) {
                    _uiState.update { it.copy(text = polished, isPolishing = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            isPolishing = false,
                            errorMessage = "AI से खाली उत्तर मिला। कृपया दोबारा प्रयास करें।",
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Voice script polish timed out", e)
                _uiState.update {
                    it.copy(
                        isPolishing = false,
                        errorMessage = "स्क्रिप्ट सुधारने में 60 सेकंड से अधिक समय लगा। कृपया पुनः प्रयास करें।",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice script polish failed", e)
                _uiState.update {
                    it.copy(
                        isPolishing = false,
                        errorMessage = e.localizedMessage ?: "स्क्रिप्ट सुधारने में त्रुटि आई।",
                    )
                }
            }
        }
    }
}
