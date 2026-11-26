package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.NaturalMaleVoiceSelector
import com.example.ui.components.VoiceVisualMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VoiceWelcomeViewModel"
private const val PREFS_NAME = "future_ai_voice_prefs"
private const val KEY_PREFERRED_LANG = "preferred_voice_language"

enum class AssistantLanguage(
    val code: String,
    val displayLabel: String,
    val sttLocaleTag: String,
    val ttsLocale: Locale,
) {
    HINDI(
        code = "HINDI",
        displayLabel = "हिंदी",
        sttLocaleTag = "hi-IN",
        ttsLocale = Locale.forLanguageTag("hi-IN"),
    ),
    ENGLISH(
        code = "ENGLISH",
        displayLabel = "English",
        sttLocaleTag = "en-IN",
        ttsLocale = Locale.forLanguageTag("en-IN"),
    ),
    BHOJPURI(
        code = "BHOJPURI",
        displayLabel = "भोजपुरी",
        sttLocaleTag = "hi-IN",
        ttsLocale = Locale.forLanguageTag("hi-IN"),
    );

    companion object {
        fun fromCode(raw: String?): AssistantLanguage = when (raw?.trim()?.uppercase(Locale.ROOT)) {
            "ENGLISH", "EN" -> ENGLISH
            "BHOJPURI", "BHO" -> BHOJPURI
            else -> HINDI
        }
    }
}

data class VoiceTurn(
    val userText: String,
    val aiReply: String,
    val language: AssistantLanguage,
)

data class VoiceWelcomeUiState(
    val isTtsReady: Boolean = false,
    val voiceEnabled: Boolean = true,
    val visualMode: VoiceVisualMode = VoiceVisualMode.IDLE,
    val activeLanguage: AssistantLanguage = AssistantLanguage.HINDI,
    val autoDetectedNotice: String? = null,
    val partialTranscript: String = "",
    val lastUserTranscript: String = "",
    val lastAiReply: String = "",
    val recentTurns: List<VoiceTurn> = emptyList(),
    val micRmsDb: Float = 0f,
    val errorMessage: String? = null,
    val statusBanner: String? = null,
    val hasSpokenGreeting: Boolean = false,
)

class VoiceWelcomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var hasTriggeredInitialGreeting = false

    private var onExchangeCompletedCallback: ((prompt: String, reply: String) -> Unit)? = null

    private val savedInitialLanguage = AssistantLanguage.fromCode(
        prefs.getString(KEY_PREFERRED_LANG, AssistantLanguage.HINDI.code),
    )

    private val _uiState = MutableStateFlow(
        VoiceWelcomeUiState(activeLanguage = savedInitialLanguage),
    )
    val uiState: StateFlow<VoiceWelcomeUiState> = _uiState.asStateFlow()

    private val voiceResponseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "detectedLanguage" to Schema.enumeration(
                    values = listOf("HINDI", "ENGLISH", "BHOJPURI"),
                    description = "Detected language of the user's latest message: HINDI, ENGLISH, or BHOJPURI.",
                ),
                "replyText" to Schema.string(
                    description = "Short, natural, friendly spoken reply in the exact detectedLanguage. Plain text only with no markdown or emojis.",
                ),
            ),
        )
    }

    private val geminiVoiceModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = voiceResponseSchema
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    "You are FUTURE AI, a warm, intelligent, natural male personal voice assistant.\n" +
                        "Follow these rules strictly:\n" +
                        "1. Detect the language of the user's latest message: HINDI, ENGLISH, or BHOJPURI.\n" +
                        "2. If the user speaks or writes Hindi (or Hinglish), set detectedLanguage=HINDI and reply in natural, polite Hindi using Devanagari script.\n" +
                        "3. If the user speaks or writes English, set detectedLanguage=ENGLISH and reply in natural, crisp English.\n" +
                        "4. If the user speaks or writes Bhojpuri (e.g., का हाल बा, रउआ, हमार, कइसन बानी, काहे, बाटे, भोजपुरी, ka haal ba, raua, hamar, kaisan), set detectedLanguage=BHOJPURI and reply warmly in natural respectful Bhojpuri written in Devanagari script so Hindi Text-to-Speech pronounces it clearly.\n" +
                        "5. If the user explicitly asks you to switch languages (e.g., 'Speak in English', 'हिंदी में बोलो', 'भोजपुरी में बात करअ'), immediately switch detectedLanguage and reply in that requested language.\n" +
                        "6. Keep replyText concise (2 to 4 natural spoken sentences, under 65 words), human-like, and completely free of markdown, bullet points, asterisks, or emojis.",
                )
            },
        )
    }

    init {
        initTextToSpeech()
    }

    fun setOnExchangeCompleted(callback: ((prompt: String, reply: String) -> Unit)?) {
        onExchangeCompletedCallback = callback
    }

    private fun initTextToSpeech() {
        val app = getApplication<Application>()
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(0.92f)
                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _uiState.update { state ->
                                if (state.visualMode != VoiceVisualMode.LISTENING) {
                                    state.copy(visualMode = VoiceVisualMode.SPEAKING)
                                } else {
                                    state
                                }
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == UTTERANCE_GREET_EN && _uiState.value.voiceEnabled) {
                                mainHandler.post {
                                    val followUp = greetingFollowUpFor(_uiState.value.activeLanguage)
                                    speakPart(
                                        text = followUp.first,
                                        locale = followUp.second,
                                        utteranceId = UTTERANCE_GREET_SECOND,
                                    )
                                }
                            } else {
                                _uiState.update { state ->
                                    if (state.visualMode == VoiceVisualMode.SPEAKING) {
                                        state.copy(
                                            visualMode = VoiceVisualMode.IDLE,
                                            hasSpokenGreeting = true,
                                        )
                                    } else {
                                        state.copy(hasSpokenGreeting = true)
                                    }
                                }
                            }
                        }

                        @Deprecated("Deprecated in Android API")
                        override fun onError(utteranceId: String?) {
                            _uiState.update { state ->
                                state.copy(
                                    visualMode = if (state.visualMode == VoiceVisualMode.SPEAKING) {
                                        VoiceVisualMode.IDLE
                                    } else {
                                        state.visualMode
                                    },
                                )
                            }
                        }
                    },
                )

                _uiState.update { it.copy(isTtsReady = true, errorMessage = null) }

                if (!hasTriggeredInitialGreeting && _uiState.value.voiceEnabled) {
                    hasTriggeredInitialGreeting = true
                    mainHandler.post {
                        playWelcomeGreeting()
                    }
                }
            } else {
                Log.e(TAG, "TextToSpeech init failed with status=$status")
                _uiState.update {
                    it.copy(
                        isTtsReady = false,
                        errorMessage = app.getString(R.string.voice_tts_not_ready),
                    )
                }
            }
        }
    }

    private fun greetingFollowUpFor(language: AssistantLanguage): Pair<String, Locale> {
        val app = getApplication<Application>()
        return when (language) {
            AssistantLanguage.HINDI ->
                app.getString(R.string.voice_assist_greeting_hi) to AssistantLanguage.HINDI.ttsLocale
            AssistantLanguage.ENGLISH ->
                app.getString(R.string.voice_assist_greeting_en_help) to AssistantLanguage.ENGLISH.ttsLocale
            AssistantLanguage.BHOJPURI ->
                app.getString(R.string.voice_assist_greeting_bho_help) to AssistantLanguage.BHOJPURI.ttsLocale
        }
    }

    fun selectLanguage(language: AssistantLanguage) {
        persistLanguage(language)
        _uiState.update {
            it.copy(
                activeLanguage = language,
                autoDetectedNotice = "सक्रिय भाषा बदली गई: ${language.displayLabel}",
                errorMessage = null,
            )
        }
        if (_uiState.value.voiceEnabled && _uiState.value.isTtsReady) {
            val confirmation = when (language) {
                AssistantLanguage.HINDI -> "अब मैं आपसे हिंदी में बात करूँगा। बताइए, मैं आपकी क्या मदद कर सकता हूँ?"
                AssistantLanguage.ENGLISH -> "I have switched to English. How can I help you today?"
                AssistantLanguage.BHOJPURI -> "अब हम रउआ से भोजपुरी में बात करब। बताईं, हम का मदद कर सकीले?"
            }
            speakPart(confirmation, language.ttsLocale, UTTERANCE_REPLY)
        }
    }

    fun playWelcomeGreeting() {
        val app = getApplication<Application>()
        if (!_uiState.value.isTtsReady) {
            _uiState.update {
                it.copy(statusBanner = app.getString(R.string.voice_assist_tts_loading))
            }
            return
        }
        stopListening()
        _uiState.update {
            it.copy(
                voiceEnabled = true,
                errorMessage = null,
                statusBanner = null,
                visualMode = VoiceVisualMode.SPEAKING,
            )
        }
        speakPart(
            text = app.getString(R.string.voice_assist_greeting_en),
            locale = AssistantLanguage.ENGLISH.ttsLocale,
            utteranceId = UTTERANCE_GREET_EN,
        )
    }

    fun toggleVoiceEnabled() {
        val app = getApplication<Application>()
        val nextEnabled = !_uiState.value.voiceEnabled
        if (!nextEnabled) {
            stopSpeaking()
        }
        _uiState.update {
            it.copy(
                voiceEnabled = nextEnabled,
                statusBanner = if (nextEnabled) {
                    app.getString(R.string.voice_assist_enabled_note)
                } else {
                    app.getString(R.string.voice_assist_muted_note)
                },
            )
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _uiState.update { state ->
            if (state.visualMode == VoiceVisualMode.SPEAKING) {
                state.copy(visualMode = VoiceVisualMode.IDLE)
            } else {
                state
            }
        }
    }

    fun onMicPermissionDenied() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                visualMode = VoiceVisualMode.IDLE,
                errorMessage = app.getString(R.string.voice_assist_mic_permission_denied),
            )
        }
    }

    fun startListening() {
        val app = getApplication<Application>()
        mainHandler.post {
            stopSpeaking()

            if (!SpeechRecognizer.isRecognitionAvailable(app)) {
                _uiState.update {
                    it.copy(
                        visualMode = VoiceVisualMode.IDLE,
                        errorMessage = app.getString(R.string.voice_assist_stt_unavailable),
                    )
                }
                return@post
            }

            try {
                ensureSpeechRecognizer()
                val sttLang = _uiState.value.activeLanguage.sttLocaleTag
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.packageName)
                }

                _uiState.update {
                    it.copy(
                        visualMode = VoiceVisualMode.LISTENING,
                        partialTranscript = "",
                        errorMessage = null,
                        statusBanner = app.getString(R.string.voice_assist_listening_live),
                    )
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _uiState.update {
                    it.copy(
                        visualMode = VoiceVisualMode.IDLE,
                        errorMessage = e.localizedMessage
                            ?: app.getString(R.string.voice_assist_stt_unavailable),
                    )
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "stopListening ignored: ${e.message}")
            }
            _uiState.update { state ->
                if (state.visualMode == VoiceVisualMode.LISTENING) {
                    state.copy(
                        visualMode = VoiceVisualMode.IDLE,
                        micRmsDb = 0f,
                        statusBanner = null,
                    )
                } else {
                    state
                }
            }
        }
    }

    /**
     * Submits a spoken or typed query to Gemini 3.8 Flash via Firebase AI Logic,
     * auto-detects Hindi / English / Bhojpuri, switches active language dynamically,
     * updates UI text, and speaks the reply in natural Male Voice when Voice is ON.
     */
    fun processUserUtterance(rawInput: String) {
        val text = rawInput.trim()
        if (text.isEmpty()) return
        if (_uiState.value.visualMode == VoiceVisualMode.THINKING) return

        stopSpeaking()
        val heuristicHint = quickDetectLanguage(text, _uiState.value.activeLanguage)

        _uiState.update {
            it.copy(
                visualMode = VoiceVisualMode.THINKING,
                partialTranscript = "",
                lastUserTranscript = text,
                errorMessage = null,
                statusBanner = "भाषा पहचानकर Gemini से उत्तर तैयार हो रहा है…",
            )
        }

        viewModelScope.launch {
            try {
                val historyContext = buildString {
                    val previousTurns = _uiState.value.recentTurns.takeLast(5)
                    if (previousTurns.isNotEmpty()) {
                        append("Recent conversation context:\n")
                        previousTurns.forEach { turn ->
                            append("User (${turn.language.code}): ${turn.userText}\n")
                            append("FUTURE AI: ${turn.aiReply}\n")
                        }
                        append("\n")
                    }
                    append("Current preferred language: ${_uiState.value.activeLanguage.code}\n")
                    append("Heuristic language hint for latest message: ${heuristicHint.code}\n")
                    append("User latest message: $text")
                }

                val response = withTimeout(60_000L) {
                    geminiVoiceModel.generateContent(historyContext)
                }
                val rawJson = response.text?.trim().orEmpty()
                if (rawJson.isEmpty()) {
                    throw IllegalStateException("Gemini से खाली उत्तर मिला।")
                }

                val parsed = parseStructuredReply(rawJson, heuristicHint)
                val detectedLang = parsed.first
                val replyText = parsed.second
                val previousLang = _uiState.value.activeLanguage

                persistLanguage(detectedLang)

                val switchBanner = if (detectedLang != previousLang) {
                    "भाषा अपने-आप बदली: ${detectedLang.displayLabel}"
                } else {
                    "पहचानी गई भाषा: ${detectedLang.displayLabel}"
                }

                val turn = VoiceTurn(
                    userText = text,
                    aiReply = replyText,
                    language = detectedLang,
                )

                _uiState.update { state ->
                    state.copy(
                        activeLanguage = detectedLang,
                        autoDetectedNotice = switchBanner,
                        lastAiReply = replyText,
                        recentTurns = (state.recentTurns + turn).takeLast(12),
                        visualMode = VoiceVisualMode.IDLE,
                        statusBanner = null,
                        errorMessage = null,
                    )
                }

                onExchangeCompletedCallback?.invoke(text, replyText)

                if (_uiState.value.voiceEnabled && _uiState.value.isTtsReady) {
                    speakPart(
                        text = replyText,
                        locale = detectedLang.ttsLocale,
                        utteranceId = UTTERANCE_REPLY,
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini voice assistant timed out", e)
                _uiState.update {
                    it.copy(
                        visualMode = VoiceVisualMode.IDLE,
                        statusBanner = null,
                        errorMessage = "जवाब आने में 60 सेकंड से अधिक समय लगा। कृपया दोबारा प्रयास करें।",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini voice assistant failed", e)
                _uiState.update {
                    it.copy(
                        visualMode = VoiceVisualMode.IDLE,
                        statusBanner = null,
                        errorMessage = e.localizedMessage
                            ?: "AI से जुड़ने में समस्या आई। कृपया पुनः प्रयास करें।",
                    )
                }
            }
        }
    }

    private fun parseStructuredReply(
        rawJson: String,
        fallbackLanguage: AssistantLanguage,
    ): Pair<AssistantLanguage, String> {
        return try {
            val cleaned = rawJson
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleaned)
            val lang = AssistantLanguage.fromCode(obj.optString("detectedLanguage", fallbackLanguage.code))
            val reply = obj.optString("replyText").trim()
            if (reply.isEmpty()) {
                fallbackLanguage to cleaned
            } else {
                lang to reply
            }
        } catch (e: Exception) {
            Log.w(TAG, "Structured JSON parse fallback: ${e.message}")
            fallbackLanguage to rawJson
        }
    }

    private fun quickDetectLanguage(
        text: String,
        fallback: AssistantLanguage,
    ): AssistantLanguage {
        val lower = text.lowercase(Locale.ROOT)
        val bhojpuriMarkers = listOf(
            "का हाल बा", "रउआ", "रउरा", "हमार", "तोहार", "कइसन", "बानी", "बाटे", "काहे",
            "भोजपुरी", "भइल", "हवे", "करत बानी", "जाये के", "लागत बा", "बाड़ू", "बड़ें",
            "ka haal ba", "raua", "raura", "hamar", "tohar", "kaisan", "baani", "bhojpuri",
            "bate", "kahe", "bhail", "batawa",
        )
        if (bhojpuriMarkers.any { lower.contains(it) || text.contains(it) }) {
            return AssistantLanguage.BHOJPURI
        }

        val hasDevanagari = text.any { ch -> ch in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            return AssistantLanguage.HINDI
        }

        val hinglishMarkers = listOf(
            "namaste", "kaise ho", "kya hai", "batao", "hindi", "samjhao", "madad", "mera",
            "tumhara", "shayari", "kahani", "dhanyawad", "shukriya",
        )
        if (hinglishMarkers.any { lower.contains(it) }) {
            return AssistantLanguage.HINDI
        }

        val hasLatinLetters = text.any { it in 'a'..'z' || it in 'A'..'Z' }
        if (hasLatinLetters) {
            return AssistantLanguage.ENGLISH
        }

        return fallback
    }

    private fun persistLanguage(language: AssistantLanguage) {
        prefs.edit().putString(KEY_PREFERRED_LANG, language.code).apply()
    }

    private fun ensureSpeechRecognizer() {
        if (speechRecognizer != null) return
        val app = getApplication<Application>()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.update {
                        it.copy(
                            visualMode = VoiceVisualMode.LISTENING,
                            errorMessage = null,
                        )
                    }
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) {
                    _uiState.update { it.copy(micRmsDb = rmsdB) }
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _uiState.update { state ->
                        state.copy(
                            micRmsDb = 0f,
                            visualMode = if (state.visualMode == VoiceVisualMode.LISTENING) {
                                VoiceVisualMode.THINKING
                            } else {
                                state.visualMode
                            },
                        )
                    }
                }

                override fun onError(error: Int) {
                    val friendlyMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "कोई आवाज़ स्पष्ट सुनाई नहीं दी। कृपया माइक दबाकर फिर बोलें।"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "बोलने का समय समाप्त हुआ। कृपया फिर से माइक पर टैप करें।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            app.getString(R.string.voice_assist_mic_permission_denied)
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "इंटरनेट कनेक्शन धीमा है। कृपया नेटवर्क चेक करके पुनः प्रयास करें।"
                        SpeechRecognizer.ERROR_AUDIO ->
                            "माइक्रोफ़ोन से ऑडियो लेने में समस्या आई।"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                            "माइक अभी व्यस्त है, कृपया एक सेकंड बाद पुनः टैप करें।"
                        else ->
                            "आवाज़ पहचानने में समस्या आई (कोड $error)। कृपया फिर कोशिश करें।"
                    }
                    _uiState.update {
                        it.copy(
                            visualMode = VoiceVisualMode.IDLE,
                            micRmsDb = 0f,
                            statusBanner = null,
                            errorMessage = friendlyMsg,
                        )
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim().orEmpty()
                    if (spokenText.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                visualMode = VoiceVisualMode.IDLE,
                                statusBanner = null,
                                errorMessage = "कोई शब्द पहचाना नहीं जा सका। कृपया फिर बोलें।",
                            )
                        }
                        return
                    }
                    processUserUtterance(spokenText)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        _uiState.update { it.copy(partialTranscript = partial) }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
        speechRecognizer = recognizer
    }

    private fun speakPart(
        text: String,
        locale: Locale,
        utteranceId: String,
    ) {
        val engine = tts ?: return
        NaturalMaleVoiceSelector.applyMaleVoice(engine, locale)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
            speechRecognizer = null
        }
    }

    private companion object {
        const val UTTERANCE_GREET_EN = "future_ai_greet_en"
        const val UTTERANCE_GREET_SECOND = "future_ai_greet_second"
        const val UTTERANCE_REPLY = "future_ai_reply"
    }
}
