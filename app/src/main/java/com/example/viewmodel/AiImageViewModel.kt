package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiImageViewModel"

enum class ImageStylePreset(@StringRes val labelRes: Int, val stylePrompt: String) {
    FUTURISTIC(R.string.image_style_futuristic, "futuristic cyberpunk neon lighting, ultra detailed, cinematic"),
    REALISTIC(R.string.image_style_realistic, "photorealistic, 8k DSLR photography, natural lighting, sharp focus"),
    ART_3D(R.string.image_style_3d, "3D octane render, smooth studio lighting, vibrant digital art"),
    ANIME(R.string.image_style_anime, "high quality anime illustration, vivid colors, dramatic atmosphere"),
    PAINTING(R.string.image_style_painting, "expressive digital oil painting, rich textures, artistic composition"),
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

data class AiImageScreenState(
    val prompt: String = "",
    val selectedStyle: ImageStylePreset = ImageStylePreset.FUTURISTIC,
    val isEnhancingPrompt: Boolean = false,
    val resultState: ImageGenUiState = ImageGenUiState.Idle,
)

class AiImageViewModel : ViewModel() {

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config,
        )
    }

    private val promptHelperModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow(AiImageScreenState())
    val uiState: StateFlow<AiImageScreenState> = _uiState.asStateFlow()

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(prompt = text) }
    }

    fun selectStyle(style: ImageStylePreset) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun enhancePromptWithGemini() {
        val current = _uiState.value
        val raw = current.prompt.trim()
        if (raw.isEmpty() || current.isEnhancingPrompt) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEnhancingPrompt = true) }
            try {
                val response = withTimeout(45_000L) {
                    promptHelperModel.generateContent(
                        "Convert this user idea (may be in Hindi or English) into a single vivid, " +
                            "concise English image generation prompt (under 35 words) suitable for a " +
                            "${current.selectedStyle.stylePrompt} artwork. Output ONLY the improved prompt.\n\nIdea: $raw",
                    )
                }
                val enhanced = response.text?.trim().orEmpty()
                if (enhanced.isNotEmpty()) {
                    _uiState.update { it.copy(prompt = enhanced, isEnhancingPrompt = false) }
                } else {
                    _uiState.update { it.copy(isEnhancingPrompt = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Prompt enhancement failed", e)
                _uiState.update {
                    it.copy(
                        isEnhancingPrompt = false,
                        resultState = ImageGenUiState.Error(
                            e.localizedMessage ?: "Prompt सुधारने में त्रुटि आई।",
                        ),
                    )
                }
            }
        }
    }

    fun generateImage() {
        val current = _uiState.value
        val cleanPrompt = current.prompt.trim()
        if (cleanPrompt.isEmpty() || current.resultState is ImageGenUiState.Loading) return

        val finalPrompt = "$cleanPrompt, ${current.selectedStyle.stylePrompt}"

        viewModelScope.launch {
            _uiState.update { it.copy(resultState = ImageGenUiState.Loading) }
            try {
                val bitmap = withTimeout(60_000L) {
                    val response = imageModel.generateContent(finalPrompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()
                        ?.image
                }
                if (bitmap != null) {
                    _uiState.update { it.copy(resultState = ImageGenUiState.Success(bitmap)) }
                } else {
                    _uiState.update {
                        it.copy(
                            resultState = ImageGenUiState.Error(
                                "मॉडल से कोई इमेज प्राप्त नहीं हुई। कृपया दूसरा प्रॉम्प्ट आज़माएँ।",
                            ),
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _uiState.update {
                    it.copy(
                        resultState = ImageGenUiState.Error(
                            "इमेज बनने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                        ),
                    )
                }
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Image generation quota exceeded", e)
                _uiState.update {
                    it.copy(
                        resultState = ImageGenUiState.Error(
                            "कोटा या बिलिंग सीमा पूरी हो गई है। Gemini इमेज जनरेशन के लिए Firebase Blaze (Pay-as-you-go) प्लान की आवश्यकता हो सकती है।",
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _uiState.update {
                    it.copy(
                        resultState = ImageGenUiState.Error(
                            e.localizedMessage
                                ?: "इमेज बनाने में त्रुटि आई। कृपया जाँचें कि आपके Firebase प्रोजेक्ट में इमेज मॉडल उपलब्ध है।",
                        ),
                    )
                }
            }
        }
    }
}
