package com.example.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
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

private const val TAG = "AiWriterViewModel"

enum class WriterFormat(@StringRes val labelRes: Int, val promptName: String) {
    STORY(R.string.writer_format_story, "short story"),
    SHAYARI(R.string.writer_format_shayari, "heartfelt shayari (poetry)"),
    SONG(R.string.writer_format_song, "original song lyrics with verses and chorus"),
    SCRIPT(R.string.writer_format_script, "engaging YouTube / Reels video script with hook and scenes"),
}

enum class WriterTone(@StringRes val labelRes: Int, val promptName: String) {
    MOTIVATIONAL(R.string.writer_tone_motivational, "motivational and inspiring"),
    EMOTIONAL(R.string.writer_tone_emotional, "deeply emotional"),
    ROMANTIC(R.string.writer_tone_romantic, "romantic and poetic"),
    FUNNY(R.string.writer_tone_funny, "funny and entertaining"),
}

enum class WriterLanguage(@StringRes val labelRes: Int, val promptName: String) {
    HINDI(R.string.writer_lang_hindi, "Hindi (Devanagari script)"),
    HINGLISH(R.string.writer_lang_hinglish, "Hinglish (Hindi written in English letters)"),
    ENGLISH(R.string.writer_lang_english, "English"),
}

sealed interface WriterUiState {
    data object Idle : WriterUiState
    data object Loading : WriterUiState
    data class Streaming(val text: String) : WriterUiState
    data class Success(val text: String) : WriterUiState
    data class Error(val message: String) : WriterUiState
}

data class AiWriterScreenState(
    val selectedFormat: WriterFormat = WriterFormat.SHAYARI,
    val selectedTone: WriterTone = WriterTone.MOTIVATIONAL,
    val selectedLanguage: WriterLanguage = WriterLanguage.HINDI,
    val topic: String = "",
    val outputState: WriterUiState = WriterUiState.Idle,
)

class AiWriterViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow(AiWriterScreenState())
    val uiState: StateFlow<AiWriterScreenState> = _uiState.asStateFlow()

    fun selectFormat(format: WriterFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun selectTone(tone: WriterTone) {
        _uiState.update { it.copy(selectedTone = tone) }
    }

    fun selectLanguage(language: WriterLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun updateTopic(topic: String) {
        _uiState.update { it.copy(topic = topic) }
    }

    fun generateContent() {
        val current = _uiState.value
        val cleanTopic = current.topic.trim()
        if (cleanTopic.isEmpty()) return
        if (current.outputState is WriterUiState.Loading || current.outputState is WriterUiState.Streaming) {
            return
        }

        val prompt = buildString {
            append("You are FUTURE AI Content Writer, an expert creative writer.\n")
            append("Write an original, high-quality ${current.selectedFormat.promptName}.\n")
            append("Topic: $cleanTopic\n")
            append("Tone: ${current.selectedTone.promptName}\n")
            append("Language: ${current.selectedLanguage.promptName}\n")
            append("Make it expressive, memorable, and neatly formatted for mobile reading. Do not include meta explanations.")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(outputState = WriterUiState.Loading) }
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    model.generateContentStream(prompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            accumulated.append(text)
                            _uiState.update {
                                it.copy(outputState = WriterUiState.Streaming(accumulated.toString()))
                            }
                        }
                    }
                }
                val finalOutput = accumulated.toString().trim()
                if (finalOutput.isNotEmpty()) {
                    _uiState.update { it.copy(outputState = WriterUiState.Success(finalOutput)) }
                } else {
                    _uiState.update {
                        it.copy(
                            outputState = WriterUiState.Error(
                                "AI से खाली उत्तर मिला। कृपया दूसरा विषय लिखकर पुनः प्रयास करें।",
                            ),
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Content writer timed out", e)
                _uiState.update {
                    it.copy(
                        outputState = WriterUiState.Error(
                            "कंटेंट लिखने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Content writer failed", e)
                _uiState.update {
                    it.copy(
                        outputState = WriterUiState.Error(
                            e.localizedMessage ?: "कंटेंट बनाने में त्रुटि आई। कृपया पुनः प्रयास करें।",
                        ),
                    )
                }
            }
        }
    }

    fun clearOutput() {
        _uiState.update { it.copy(outputState = WriterUiState.Idle) }
    }
}
