package com.example.ui.screens

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ai.NaturalMaleVoiceSelector
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonOutlineButton
import com.example.viewmodel.AiVoiceViewModel
import com.example.viewmodel.VoiceLocaleOptions
import java.util.Locale
import java.util.UUID

@Composable
fun AiVoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiVoiceViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsStatusMessage by remember { mutableStateOf<String?>(null) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    val ttsNotReadyMsg = stringResource(id = R.string.voice_tts_not_ready)
    val ttsFallbackMsg = stringResource(id = R.string.voice_tts_fallback_lang)

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    ttsStatusMessage = null
                    engine?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                mainHandler.post { isSpeaking = true }
                            }

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post { isSpeaking = false }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                mainHandler.post { isSpeaking = false }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                mainHandler.post { isSpeaking = false }
                            }
                        },
                    )
                } else {
                    ttsReady = false
                    ttsStatusMessage = ttsNotReadyMsg
                }
            }
        }
        ttsEngine = engine

        onDispose {
            engine.stop()
            engine.shutdown()
            ttsEngine = null
        }
    }

    val quickSamples = listOf(
        stringResource(id = R.string.voice_sample_1),
        stringResource(id = R.string.voice_sample_2),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        FutureAiBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                ttsEngine?.stop()
                                isSpeaking = false
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("voice_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.voice_screen_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(id = R.string.voice_screen_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Live Soundwave Visualizer Card
                    FutureGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isSpeaking) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    text = if (isSpeaking) {
                                        stringResource(id = R.string.voice_status_speaking)
                                    } else if (ttsReady) {
                                        stringResource(id = R.string.voice_status_ready)
                                    } else {
                                        stringResource(id = R.string.voice_status_loading)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSpeaking) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                            Text(
                                text = state.selectedVoice.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        NeonSoundWaveBars(isActive = isSpeaking)
                    }

                    // Voice Settings & Input Card
                    FutureGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.voice_label_language),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VoiceLocaleOptions.all.forEach { option ->
                                val selected = state.selectedVoice.id == option.id
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            },
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            RoundedCornerShape(50),
                                        )
                                        .clickable { viewModel.selectVoice(option) }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                        .testTag("voice_locale_${option.id}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = state.text,
                            onValueChange = { viewModel.updateText(it) },
                            label = { Text(text = stringResource(id = R.string.voice_input_label)) },
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.voice_input_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_text_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickSamples.forEach { sample ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            RoundedCornerShape(50),
                                        )
                                        .clickable { viewModel.updateText(sample) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = sample,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(id = R.string.voice_speed_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fx", state.speechRate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = state.speechRate,
                            onValueChange = { viewModel.updateSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier.testTag("voice_speed_slider"),
                        )

                        // Pitch Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(id = R.string.voice_pitch_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fx", state.pitch),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Slider(
                            value = state.pitch,
                            onValueChange = { viewModel.updatePitch(it) },
                            valueRange = 0.6f..1.6f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier.testTag("voice_pitch_slider"),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Play / Stop Button
                        NeonGradientButton(
                            text = if (isSpeaking) {
                                stringResource(id = R.string.voice_stop_button)
                            } else {
                                stringResource(id = R.string.voice_speak_button)
                            },
                            icon = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            enabled = ttsReady && (isSpeaking || state.text.isNotBlank()),
                            onClick = {
                                val engine = ttsEngine ?: return@NeonGradientButton
                                if (isSpeaking) {
                                    engine.stop()
                                    isSpeaking = false
                                } else {
                                    val matchedNativeMale = NaturalMaleVoiceSelector.applyMaleVoice(
                                        engine = engine,
                                        preferredLocale = state.selectedVoice.locale,
                                    )
                                    ttsStatusMessage = null
                                    engine.setSpeechRate(state.speechRate)
                                    val effectivePitch = if (matchedNativeMale) {
                                        state.pitch
                                    } else {
                                        (state.pitch * 0.84f).coerceIn(0.6f, 1.4f)
                                    }
                                    engine.setPitch(effectivePitch)
                                    engine.speak(
                                        state.text.trim(),
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        UUID.randomUUID().toString(),
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_play_stop_button"),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gemini Script Polish Button
                        NeonOutlineButton(
                            text = if (state.isPolishing) {
                                stringResource(id = R.string.voice_polishing)
                            } else {
                                stringResource(id = R.string.voice_polish_button)
                            },
                            icon = Icons.Rounded.AutoAwesome,
                            onClick = {
                                if (!state.isPolishing && state.text.isNotBlank()) {
                                    viewModel.polishForVoiceover()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_polish_button"),
                        )
                    }

                    // Status or Error Note
                    val alertMessage = state.errorMessage ?: ttsStatusMessage
                    if (!alertMessage.isNullOrBlank()) {
                        FutureGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentBorder = false,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = alertMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NeonSoundWaveBars(
    isActive: Boolean,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")

    val waveA by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveA",
    )
    val waveB by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveB",
    )

    val barCount = 14
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(barCount) { idx ->
            val factor = when {
                !isActive -> 0.18f + (idx % 3) * 0.08f
                idx % 2 == 0 -> waveA
                else -> waveB
            }
            val barHeight = (10f + 34f * factor).dp
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(neonBlue, electricPurple),
                        ),
                    ),
            )
        }
    }
}
