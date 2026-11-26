package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonOutlineButton
import com.example.ui.components.VoiceEqualizerWave
import com.example.ui.components.VoiceRobotAvatar
import com.example.ui.components.VoiceVisualMode
import com.example.viewmodel.AssistantLanguage
import com.example.viewmodel.VoiceWelcomeViewModel
import kotlinx.coroutines.delay

@Composable
fun VoiceWelcomeAssistantScreen(
    onContinueToApp: () -> Unit,
    showBackArrow: Boolean = false,
    onExchangeCompleted: ((prompt: String, reply: String) -> Unit)? = null,
    viewModel: VoiceWelcomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showGreetingLine1 by remember { mutableStateOf(false) }
    var showGreetingLine2 by remember { mutableStateOf(false) }
    var typedPrompt by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(onExchangeCompleted) {
        viewModel.setOnExchangeCompleted(onExchangeCompleted)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            viewModel.onMicPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        delay(180)
        showGreetingLine1 = true
        delay(520)
        showGreetingLine2 = true
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
            viewModel.stopListening()
        }
    }

    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val isThinking = uiState.visualMode == VoiceVisualMode.THINKING
    val isListening = uiState.visualMode == VoiceVisualMode.LISTENING
    val isSpeaking = uiState.visualMode == VoiceVisualMode.SPEAKING

    val quickPrompts = listOf(
        stringResource(id = R.string.voice_assist_sample_hi),
        stringResource(id = R.string.voice_assist_sample_en),
        stringResource(id = R.string.voice_assist_sample_bho),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        FutureAiBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 580.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top Controls Row: Back / Male Voice ON-OFF / Continue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (showBackArrow) {
                                IconButton(
                                    onClick = {
                                        viewModel.stopSpeaking()
                                        viewModel.stopListening()
                                        onContinueToApp()
                                    },
                                    modifier = Modifier.testTag("voice_assistant_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(id = R.string.cd_back),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (uiState.voiceEnabled) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (uiState.voiceEnabled) {
                                            neonBlue.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(50),
                                    )
                                    .clickable { viewModel.toggleVoiceEnabled() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("voice_assistant_sound_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = if (uiState.voiceEnabled) {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    },
                                    contentDescription = stringResource(id = R.string.cd_toggle_voice),
                                    tint = if (uiState.voiceEnabled) {
                                        neonBlue
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = if (uiState.voiceEnabled) {
                                        stringResource(id = R.string.voice_assist_voice_on)
                                    } else {
                                        stringResource(id = R.string.voice_assist_voice_off)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.voiceEnabled) {
                                        neonBlue
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }

                        if (!showBackArrow) {
                            TextButton(
                                onClick = {
                                    viewModel.stopSpeaking()
                                    viewModel.stopListening()
                                    onContinueToApp()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("voice_assistant_continue_button"),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.voice_assist_open_app),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = neonBlue,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = neonBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Auto-Detected Language Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f))
                            .border(
                                width = 1.dp,
                                color = electricPurple.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(50),
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("voice_assistant_language_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = electricPurple,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = uiState.autoDetectedNotice
                                ?: stringResource(
                                    id = R.string.voice_assist_active_lang_badge,
                                    uiState.activeLanguage.displayLabel,
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hero Animated AI Robot Avatar
                    VoiceRobotAvatar(
                        mode = uiState.visualMode,
                        size = 188.dp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    VoiceEqualizerWave(mode = uiState.visualMode)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Greeting & AI Dialogue Card
                    FutureGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_assistant_greeting_card"),
                        contentPadding = PaddingValues(16.dp),
                        accentBorder = true,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (isThinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = neonBlue,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = neonBlue,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Text(
                                text = when (uiState.visualMode) {
                                    VoiceVisualMode.SPEAKING -> stringResource(id = R.string.voice_assist_state_speaking)
                                    VoiceVisualMode.LISTENING -> stringResource(id = R.string.voice_assist_state_listening)
                                    VoiceVisualMode.THINKING -> stringResource(id = R.string.voice_assist_state_thinking)
                                    VoiceVisualMode.IDLE -> stringResource(id = R.string.voice_assist_state_ready)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = neonBlue,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedVisibility(
                            visible = showGreetingLine1,
                            enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { 12 },
                        ) {
                            Text(
                                text = stringResource(id = R.string.voice_assist_greeting_en),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        AnimatedVisibility(
                            visible = showGreetingLine2,
                            enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { 12 },
                        ) {
                            Text(
                                text = when (uiState.activeLanguage) {
                                    AssistantLanguage.HINDI -> stringResource(id = R.string.voice_assist_greeting_hi)
                                    AssistantLanguage.ENGLISH -> stringResource(id = R.string.voice_assist_greeting_en_help)
                                    AssistantLanguage.BHOJPURI -> stringResource(id = R.string.voice_assist_greeting_bho_help)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = neonBlue,
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = uiState.statusBanner
                                ?: if (!uiState.isTtsReady) {
                                    stringResource(id = R.string.voice_assist_tts_loading)
                                } else {
                                    stringResource(id = R.string.voice_assist_step3_ready_hint)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Live Speech/Text Exchange (User + Gemini Reply)
                    val liveHeard = uiState.partialTranscript.ifBlank { uiState.lastUserTranscript }
                    AnimatedVisibility(visible = liveHeard.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FutureGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_assistant_transcript_card"),
                                contentPadding = PaddingValues(14.dp),
                                accentBorder = false,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.voice_assist_user_said_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = electricPurple,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = liveHeard,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            if (uiState.lastAiReply.isNotEmpty() && uiState.partialTranscript.isEmpty()) {
                                FutureGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("voice_assistant_reply_card"),
                                    contentPadding = PaddingValues(14.dp),
                                    accentBorder = true,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.voice_assist_ai_reply_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = neonBlue,
                                        )
                                        Text(
                                            text = uiState.activeLanguage.displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = electricPurple,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.lastAiReply,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Error Banner
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Language Selector & Auto-Detect Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistantLanguage.entries.forEach { lang ->
                            InteractiveLanguageChip(
                                label = lang.displayLabel,
                                selected = uiState.activeLanguage == lang,
                                onClick = { viewModel.selectLanguage(lang) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voice_lang_chip_${lang.code.lowercase()}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick 1-Tap Multilingual Test Prompts (Hindi / English / Bhojpuri)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickPrompts.forEachIndexed { index, sample ->
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                                    .border(
                                        width = 1.dp,
                                        color = neonBlue.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(50),
                                    )
                                    .clickable(enabled = !isThinking) {
                                        viewModel.processUserUtterance(sample)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("voice_quick_prompt_$index"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = sample,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Always-Available Glowing Microphone Button
                    GlowingMicActionButton(
                        isListening = isListening,
                        isBusy = isThinking,
                        onClick = {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                val hasMicPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPerm) {
                                    viewModel.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when {
                            isListening -> stringResource(id = R.string.voice_assist_tap_to_stop)
                            isThinking -> stringResource(id = R.string.voice_assist_state_thinking)
                            else -> stringResource(id = R.string.voice_assist_tap_to_speak)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Compact Text Input (also auto-detects Hindi/English/Bhojpuri + speaks reply!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = typedPrompt,
                            onValueChange = { typedPrompt = it },
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.voice_assist_type_placeholder),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_assistant_text_input"),
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (typedPrompt.isNotBlank() && !isThinking) {
                                        val msg = typedPrompt
                                        typedPrompt = ""
                                        viewModel.processUserUtterance(msg)
                                    }
                                },
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = neonBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )

                        val canSendTyped = typedPrompt.isNotBlank() && !isThinking
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSendTyped) {
                                        Brush.linearGradient(listOf(neonBlue, electricPurple))
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ),
                                        )
                                    },
                                )
                                .clickable(enabled = canSendTyped) {
                                    val msg = typedPrompt
                                    typedPrompt = ""
                                    viewModel.processUserUtterance(msg)
                                }
                                .testTag("voice_assistant_send_text_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(id = R.string.cd_send_message),
                                tint = if (canSendTyped) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enable Voice / Replay Greeting OR Stop Speaking button
                    NeonOutlineButton(
                        text = when {
                            isSpeaking -> stringResource(id = R.string.voice_assist_stop_voice)
                            uiState.hasSpokenGreeting -> stringResource(id = R.string.voice_assist_replay_greeting)
                            else -> stringResource(id = R.string.voice_assist_enable_voice_btn)
                        },
                        icon = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.playWelcomeGreeting()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_assistant_enable_voice_button"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun InteractiveLanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun GlowingMicActionButton(
    isListening: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.26f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 750 else 1600,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_halo_scale",
    )

    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            (if (isListening) electricPurple else neonBlue).copy(alpha = 0.42f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (isListening) {
                            listOf(electricPurple, MaterialTheme.colorScheme.error)
                        } else {
                            listOf(neonBlue, electricPurple)
                        },
                    ),
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.45f),
                    shape = CircleShape,
                )
                .clickable(enabled = !isBusy, onClick = onClick)
                .testTag("voice_assistant_mic_button"),
            contentAlignment = Alignment.Center,
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.6.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = stringResource(id = R.string.cd_voice_assistant_mic),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
