package com.example.viewmodel

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiMusicViewModel"

enum class MusicGenre(@StringRes val labelRes: Int, val promptName: String) {
    BOLLYWOOD(R.string.music_genre_bollywood, "Bollywood melodic pop"),
    LOFI(R.string.music_genre_lofi, "chill Lo-Fi beats"),
    CYBERPUNK(R.string.music_genre_cyberpunk, "futuristic cyberpunk synthwave"),
    ACOUSTIC(R.string.music_genre_acoustic, "warm acoustic guitar ballad"),
}

data class MusicComposition(
    val title: String,
    val mood: String,
    val bpm: Int,
    val musicalKey: String,
    val chordProgression: String,
    val lyrics: String,
    val melodyNotes: List<String>,
)

sealed interface MusicStudioResult {
    data object Idle : MusicStudioResult
    data object Loading : MusicStudioResult
    data class Success(val composition: MusicComposition) : MusicStudioResult
    data class Error(val message: String) : MusicStudioResult
}

data class AiMusicUiState(
    val themePrompt: String = "",
    val selectedGenre: MusicGenre = MusicGenre.BOLLYWOOD,
    val result: MusicStudioResult = MusicStudioResult.Idle,
    val isPlayingMelody: Boolean = false,
)

class AiMusicViewModel : ViewModel() {

    private val model by lazy {
        val musicSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Original song title"),
                "mood" to Schema.string(description = "Song mood e.g. Uplifting, Romantic, Energetic"),
                "bpm" to Schema.integer(description = "Tempo in beats per minute between 70 and 140"),
                "musicalKey" to Schema.string(description = "Musical key e.g. C Major or A Minor"),
                "chordProgression" to Schema.string(description = "4-chord progression e.g. Am - F - C - G"),
                "lyrics" to Schema.string(description = "Short original verse and chorus lyrics"),
                "melodyNotes" to Schema.array(
                    items = Schema.string(),
                    description = "8 to 16 musical note names from C4, D4, E4, F4, G4, A4, B4, C5, D5, E5",
                ),
            ),
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = musicSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
        )
    }

    private val _uiState = MutableStateFlow(AiMusicUiState())
    val uiState: StateFlow<AiMusicUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(themePrompt = text) }
    }

    fun selectGenre(genre: MusicGenre) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun composeMusic() {
        val current = _uiState.value
        val idea = current.themePrompt.trim()
        if (idea.isEmpty() || current.result is MusicStudioResult.Loading) return

        stopMelodyPlayback()

        val prompt = buildString {
            append("You are FUTURE AI Music Composer.\n")
            append("Create an original ${current.selectedGenre.promptName} musical composition based on: $idea\n")
            append("Write catchy title and lyrics in Hindi (or matching user language).\n")
            append("For melodyNotes, output 12 pleasant musical note tokens strictly chosen from: ")
            append("C4, D4, E4, F4, G4, A4, B4, C5, D5, E5.")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(result = MusicStudioResult.Loading) }
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("खाली उत्तर प्राप्त हुआ।")
                val json = JSONObject(jsonString)
                val notesArray = json.getJSONArray("melodyNotes")
                val notes = (0 until notesArray.length()).map { notesArray.getString(it) }
                    .ifEmpty { listOf("C4", "E4", "G4", "A4", "G4", "E4", "D4", "C4") }

                val composition = MusicComposition(
                    title = json.optString("title", idea),
                    mood = json.optString("mood", ""),
                    bpm = json.optInt("bpm", 100).coerceIn(70, 150),
                    musicalKey = json.optString("musicalKey", "C Major"),
                    chordProgression = json.optString("chordProgression", "C - G - Am - F"),
                    lyrics = json.optString("lyrics", ""),
                    melodyNotes = notes,
                )
                _uiState.update { it.copy(result = MusicStudioResult.Success(composition)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Music composition timed out", e)
                _uiState.update {
                    it.copy(
                        result = MusicStudioResult.Error(
                            "संगीत बनने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Music composition failed", e)
                _uiState.update {
                    it.copy(
                        result = MusicStudioResult.Error(
                            e.localizedMessage ?: "संगीत तैयार करने में त्रुटि आई।",
                        ),
                    )
                }
            }
        }
    }

    fun toggleMelodyPlayback() {
        if (_uiState.value.isPlayingMelody) {
            stopMelodyPlayback()
            return
        }
        val composition = (_uiState.value.result as? MusicStudioResult.Success)?.composition ?: return

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isPlayingMelody = true) }
            val sampleRate = 22050
            val noteDurationSec = (60.0 / composition.bpm.toDouble()) * 0.75
            val samplesPerNote = (sampleRate * noteDurationSec).toInt().coerceAtLeast(1024)
            val totalSamples = samplesPerNote * composition.melodyNotes.size
            val pcm = ShortArray(totalSamples)

            composition.melodyNotes.forEachIndexed { noteIdx, noteName ->
                val freq = noteToFrequency(noteName)
                val offset = noteIdx * samplesPerNote
                for (i in 0 until samplesPerNote) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    // Smooth piano-like synth envelope with warm second harmonic
                    val attack = (i.toDouble() / (sampleRate * 0.015)).coerceAtMost(1.0)
                    val decay = exp(-3.2 * t / noteDurationSec)
                    val envelope = attack * decay
                    val wave = 0.72 * sin(2.0 * PI * freq * t) +
                        0.28 * sin(2.0 * PI * (freq * 2.0) * t)
                    pcm[offset + i] = (wave * envelope * 22000.0).toInt().toShort()
                }
            }

            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val bufferBytes = maxOf(minBuf, pcm.size * 2)
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                var written = 0
                val chunkSize = 2048
                while (isActive && written < pcm.size) {
                    val count = minOf(chunkSize, pcm.size - written)
                    track.write(pcm, written, count)
                    written += count
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack melody playback failed", e)
            } finally {
                runCatching {
                    track?.stop()
                    track?.release()
                }
                _uiState.update { it.copy(isPlayingMelody = false) }
            }
        }
    }

    fun stopMelodyPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { it.copy(isPlayingMelody = false) }
    }

    override fun onCleared() {
        stopMelodyPlayback()
        super.onCleared()
    }

    private fun noteToFrequency(note: String): Double = when (note.trim().uppercase()) {
        "C4" -> 261.63
        "D4" -> 293.66
        "E4" -> 329.63
        "F4" -> 349.23
        "G4" -> 392.00
        "A4" -> 440.00
        "B4" -> 493.88
        "C5" -> 523.25
        "D5" -> 587.33
        "E5" -> 659.25
        else -> 329.63
    }
}
