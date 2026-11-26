package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiTranslatorViewModel"

data class TranslatorLanguage(
    val code: String,
    val displayName: String,
    val promptName: String,
)

object TranslatorLanguages {
    val autoDetect = TranslatorLanguage("auto", "ऑटो पहचान", "Auto-detect")

    val targetLanguages: List<TranslatorLanguage> = listOf(
        TranslatorLanguage("hi", "हिंदी (Hindi)", "Hindi"),
        TranslatorLanguage("en", "English", "English"),
        TranslatorLanguage("hinglish", "Hinglish", "Hinglish (Hindi in Roman script)"),
        TranslatorLanguage("mr", "मराठी (Marathi)", "Marathi"),
        TranslatorLanguage("bn", "বাংলা (Bengali)", "Bengali"),
        TranslatorLanguage("ta", "தமிழ் (Tamil)", "Tamil"),
        TranslatorLanguage("gu", "ગુજરાતી (Gujarati)", "Gujarati"),
        TranslatorLanguage("pa", "ਪੰਜਾਬੀ (Punjabi)", "Punjabi"),
        TranslatorLanguage("ur", "اردو (Urdu)", "Urdu"),
        TranslatorLanguage("es", "Español (Spanish)", "Spanish"),
        TranslatorLanguage("fr", "Français (French)", "French"),
        TranslatorLanguage("ja", "日本語 (Japanese)", "Japanese"),
        TranslatorLanguage("ar", "العربية (Arabic)", "Arabic"),
    )

    val sourceLanguages: List<TranslatorLanguage> = listOf(autoDetect) + targetLanguages
}

sealed interface TranslatorOutputState {
    data object Idle : TranslatorOutputState
    data object Loading : TranslatorOutputState
    data class Streaming(val translatedText: String) : TranslatorOutputState
    data class Success(val translatedText: String) : TranslatorOutputState
    data class Error(val message: String) : TranslatorOutputState
}

data class AiTranslatorUiState(
    val sourceLanguage: TranslatorLanguage = TranslatorLanguages.autoDetect,
    val targetLanguage: TranslatorLanguage = TranslatorLanguages.targetLanguages[1], // English
    val sourceText: String = "",
    val outputState: TranslatorOutputState = TranslatorOutputState.Idle,
)

class AiTranslatorViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow(AiTranslatorUiState())
    val uiState: StateFlow<AiTranslatorUiState> = _uiState.asStateFlow()

    fun selectSourceLanguage(language: TranslatorLanguage) {
        _uiState.update { it.copy(sourceLanguage = language) }
    }

    fun selectTargetLanguage(language: TranslatorLanguage) {
        _uiState.update { it.copy(targetLanguage = language) }
    }

    fun swapLanguages() {
        _uiState.update { current ->
            val newSource = current.targetLanguage
            val newTarget = if (current.sourceLanguage.code == "auto") {
                if (current.targetLanguage.code == "hi") {
                    TranslatorLanguages.targetLanguages[1]
                } else {
                    TranslatorLanguages.targetLanguages[0]
                }
            } else {
                current.sourceLanguage
            }
            val currentTranslation = (current.outputState as? TranslatorOutputState.Success)?.translatedText
            current.copy(
                sourceLanguage = newSource,
                targetLanguage = newTarget,
                sourceText = currentTranslation ?: current.sourceText,
                outputState = TranslatorOutputState.Idle,
            )
        }
    }

    fun updateSourceText(text: String) {
        _uiState.update { it.copy(sourceText = text) }
    }

    fun clearAll() {
        _uiState.update { it.copy(sourceText = "", outputState = TranslatorOutputState.Idle) }
    }

    fun translate() {
        val current = _uiState.value
        val input = current.sourceText.trim()
        if (input.isEmpty()) return
        if (current.outputState is TranslatorOutputState.Loading ||
            current.outputState is TranslatorOutputState.Streaming
        ) {
            return
        }

        val sourceInstruction = if (current.sourceLanguage.code == "auto") {
            "Detect the source language automatically."
        } else {
            "The source language is ${current.sourceLanguage.promptName}."
        }

        val prompt = buildString {
            append("You are FUTURE AI Translator, a precise and natural multilingual translator.\n")
            append("$sourceInstruction\n")
            append("Translate the following text accurately into ${current.targetLanguage.promptName}.\n")
            append("Return ONLY the translated text without quotes, notes, or extra commentary.\n\n")
            append("Text:\n$input")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(outputState = TranslatorOutputState.Loading) }
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    model.generateContentStream(prompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            _uiState.update {
                                it.copy(outputState = TranslatorOutputState.Streaming(accumulated.toString()))
                            }
                        }
                    }
                }
                val finalOutput = accumulated.toString().trim()
                if (finalOutput.isNotEmpty()) {
                    _uiState.update { it.copy(outputState = TranslatorOutputState.Success(finalOutput)) }
                } else {
                    _uiState.update {
                        it.copy(
                            outputState = TranslatorOutputState.Error(
                                "अनुवाद खाली प्राप्त हुआ। कृपया दोबारा प्रयास करें।",
                            ),
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Translation timed out", e)
                _uiState.update {
                    it.copy(
                        outputState = TranslatorOutputState.Error(
                            "अनुवाद में 60 सेकंड से अधिक समय लगा। कृपया पुनः प्रयास करें।",
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                _uiState.update {
                    it.copy(
                        outputState = TranslatorOutputState.Error(
                            e.localizedMessage ?: "अनुवाद करने में त्रुटि आई। कृपया पुनः प्रयास करें।",
                        ),
                    )
                }
            }
        }
    }
}
