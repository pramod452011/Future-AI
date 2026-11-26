package com.example.ai

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

private const val TAG = "MaleVoiceSelector"

/**
 * Selects the highest-quality natural Male voice available in Android/Google TTS
 * for the requested locale, and tunes pitch/rate so the voice sounds clear,
 * warm, and unmistakably masculine even on devices with limited voice packs.
 */
object NaturalMaleVoiceSelector {

    // Known Google Speech Services male voice codes across hi-IN, en-IN, en-US, and en-GB
    private val KNOWN_GOOGLE_MALE_TOKENS = listOf(
        "-hid-", // Hindi (India) Male
        "-hie-", // Hindi (India) Male
        "-end-", // English (India) Male
        "-ene-", // English (India) Male
        "-iom-", // English (US) Male
        "-iob-", // English (US) Male
        "-iol-", // English (US) Male
        "-tpd-", // English (US) Male
        "-tpf-", // English (US) Male
        "-gbb-", // English (UK) Male
        "-gbd-", // English (UK) Male
        "-rjs-", // English (UK) Male
        "#male",
    )

    private fun isLikelyMaleVoice(voice: Voice): Boolean {
        val lower = voice.name.lowercase(Locale.ROOT)
        if (lower.contains("female")) return false
        if (lower.contains("male")) return true
        return KNOWN_GOOGLE_MALE_TOKENS.any { token -> lower.contains(token) }
    }

    private fun isInstalled(voice: Voice): Boolean =
        !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)

    fun applyMaleVoice(
        engine: TextToSpeech,
        preferredLocale: Locale,
    ): Boolean {
        try {
            val allVoices = engine.voices?.filter { isInstalled(it) }.orEmpty()
            if (allVoices.isNotEmpty()) {
                val exactLocaleVoices = allVoices.filter {
                    it.locale.language.equals(preferredLocale.language, ignoreCase = true) &&
                        it.locale.country.equals(preferredLocale.country, ignoreCase = true)
                }
                val sameLangVoices = allVoices.filter {
                    it.locale.language.equals(preferredLocale.language, ignoreCase = true)
                }

                // 1. Prefer verified Male voice in exact locale (e.g., hi-IN or en-IN)
                val exactMale = exactLocaleVoices
                    .filter { isLikelyMaleVoice(it) }
                    .maxByOrNull { scoreVoice(it) }

                // 2. Fallback to verified Male voice in same language (e.g., en-US / en-GB)
                val langMale = exactMale ?: sameLangVoices
                    .filter { isLikelyMaleVoice(it) }
                    .maxByOrNull { scoreVoice(it) }

                if (langMale != null) {
                    engine.voice = langMale
                    engine.setSpeechRate(0.96f)
                    engine.setPitch(0.94f)
                    Log.d(TAG, "Selected native male voice: ${langMale.name} (${langMale.locale})")
                    return true
                }

                // 3. No dedicated male voice pack installed — choose best voice in locale & deepen pitch naturally
                val bestFallback = exactLocaleVoices.maxByOrNull { scoreVoice(it) }
                    ?: sameLangVoices.maxByOrNull { scoreVoice(it) }

                if (bestFallback != null) {
                    engine.voice = bestFallback
                    engine.setSpeechRate(0.95f)
                    engine.setPitch(0.78f)
                    Log.d(TAG, "Applied deeper male pitch tuning on voice: ${bestFallback.name}")
                    return false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Male voice lookup fallback: ${e.message}")
        }

        val result = engine.setLanguage(preferredLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(0.95f)
        engine.setPitch(0.78f)
        return false
    }

    private fun scoreVoice(voice: Voice): Int {
        var score = voice.quality
        // Prefer local low-latency voices slightly when quality ties, or high-quality neural voices
        if (!voice.isNetworkConnectionRequired) {
            score += 50
        }
        return score
    }
}
