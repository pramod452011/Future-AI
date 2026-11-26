package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiVideoViewModel"

enum class VideoAspectOption(val label: String) {
    REELS_9_16("9:16 Reels"),
    YOUTUBE_16_9("16:9 YouTube"),
    SQUARE_1_1("1:1 Square"),
}

data class VideoScenePlan(
    val sceneNumber: Int,
    val timecode: String,
    val visualDescription: String,
    val cameraMovement: String,
    val voiceover: String,
)

data class VideoStoryboard(
    val title: String,
    val veoMasterPrompt: String,
    val musicMood: String,
    val scenes: List<VideoScenePlan>,
)

sealed interface VideoStudioResult {
    data object Idle : VideoStudioResult
    data object Loading : VideoStudioResult
    data class Success(val storyboard: VideoStoryboard) : VideoStudioResult
    data class Error(val message: String) : VideoStudioResult
}

data class AiVideoUiState(
    val idea: String = "",
    val aspect: VideoAspectOption = VideoAspectOption.REELS_9_16,
    val result: VideoStudioResult = VideoStudioResult.Idle,
)

class AiVideoViewModel : ViewModel() {

    private val model by lazy {
        val sceneSchema = Schema.obj(
            properties = mapOf(
                "sceneNumber" to Schema.integer(description = "Sequential scene number starting at 1"),
                "timecode" to Schema.string(description = "Time range e.g. 0:00 - 0:04"),
                "visualDescription" to Schema.string(description = "Vivid visual description of the scene"),
                "cameraMovement" to Schema.string(description = "Camera motion e.g. Slow drone push-in"),
                "voiceover" to Schema.string(description = "Spoken voiceover line matching user language"),
            ),
        )
        val storyboardSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Catchy video title"),
                "veoMasterPrompt" to Schema.string(
                    description = "Single detailed cinematic English prompt ready for Google Veo video generation",
                ),
                "musicMood" to Schema.string(description = "Recommended background sound & music style"),
                "scenes" to Schema.array(
                    items = sceneSchema,
                    description = "3 to 4 sequential cinematic scenes",
                ),
            ),
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = storyboardSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
        )
    }

    private val _uiState = MutableStateFlow(AiVideoUiState())
    val uiState: StateFlow<AiVideoUiState> = _uiState.asStateFlow()

    fun updateIdea(text: String) {
        _uiState.update { it.copy(idea = text) }
    }

    fun selectAspect(option: VideoAspectOption) {
        _uiState.update { it.copy(aspect = option) }
    }

    fun generateStoryboard() {
        val current = _uiState.value
        val cleanIdea = current.idea.trim()
        if (cleanIdea.isEmpty() || current.result is VideoStudioResult.Loading) return

        val prompt = buildString {
            append("You are FUTURE AI Video Director creating a Google Veo-ready cinematic video plan.\n")
            append("Video idea: $cleanIdea\n")
            append("Format: ${current.aspect.label}\n")
            append("Write the veoMasterPrompt in vivid cinematic English. ")
            append("Write title, visualDescription, cameraMovement, musicMood, and voiceover in Hindi (or matching the user's language) so it is easy to read. ")
            append("Generate exactly 4 sequential scenes.")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(result = VideoStudioResult.Loading) }
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("खाली उत्तर प्राप्त हुआ।")
                val json = JSONObject(jsonString)
                val scenesArray = json.getJSONArray("scenes")
                val scenes = (0 until scenesArray.length()).map { idx ->
                    val obj = scenesArray.getJSONObject(idx)
                    VideoScenePlan(
                        sceneNumber = obj.optInt("sceneNumber", idx + 1),
                        timecode = obj.optString("timecode", ""),
                        visualDescription = obj.optString("visualDescription", ""),
                        cameraMovement = obj.optString("cameraMovement", ""),
                        voiceover = obj.optString("voiceover", ""),
                    )
                }
                val storyboard = VideoStoryboard(
                    title = json.optString("title", cleanIdea),
                    veoMasterPrompt = json.optString("veoMasterPrompt", ""),
                    musicMood = json.optString("musicMood", ""),
                    scenes = scenes,
                )
                _uiState.update { it.copy(result = VideoStudioResult.Success(storyboard)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Video storyboard timed out", e)
                _uiState.update {
                    it.copy(
                        result = VideoStudioResult.Error(
                            "वीडियो प्लान बनने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video storyboard failed", e)
                _uiState.update {
                    it.copy(
                        result = VideoStudioResult.Error(
                            e.localizedMessage ?: "वीडियो स्टोरीबोर्ड बनाने में त्रुटि आई।",
                        ),
                    )
                }
            }
        }
    }
}
