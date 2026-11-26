package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GeminiUsageEvent
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.viewmodel.AccountUiState
import com.example.viewmodel.GeminiConnectionTestState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GeminiStatusScreen(
    userEmail: String,
    usageState: AccountUiState<List<GeminiUsageEvent>>,
    connectionTestState: GeminiConnectionTestState,
    onRunConnectionTest: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        FutureAiBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("gemini_status_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.gemini_status_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(id = R.string.gemini_status_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = neonBlue,
                            )
                        }
                    }

                    val events = (usageState as? AccountUiState.Success)?.data.orEmpty()
                    val totalRequests = events.size
                    val estimatedTokens = remember(events) {
                        events.sumOf { (it.promptChars + it.responseChars) / 4 }
                    }
                    val toolBreakdown = remember(events) {
                        events.groupingBy { it.toolType }
                            .eachCount()
                            .entries
                            .sortedByDescending { it.value }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gemini_status_list"),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // 1. Server Backend & Security Architecture Card
                        item {
                            FutureGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(18.dp),
                                accentBorder = true,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = neonBlue,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Text(
                                        text = stringResource(id = R.string.gemini_security_heading),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                SecurityCheckRow(stringResource(id = R.string.gemini_check_backend))
                                SecurityCheckRow(stringResource(id = R.string.gemini_check_no_key))
                                SecurityCheckRow(stringResource(id = R.string.gemini_check_separate_auth))
                                SecurityCheckRow(
                                    stringResource(
                                        id = R.string.gemini_check_user_scope,
                                        userEmail.ifBlank { "Signed-in User" },
                                    ),
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                when (connectionTestState) {
                                    is GeminiConnectionTestState.Testing -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.4.dp,
                                                color = neonBlue,
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(id = R.string.gemini_testing_now),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = neonBlue,
                                            )
                                        }
                                    }

                                    else -> {
                                        NeonGradientButton(
                                            text = stringResource(id = R.string.gemini_test_button),
                                            icon = Icons.Rounded.Bolt,
                                            onClick = onRunConnectionTest,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("gemini_test_connection_button"),
                                        )
                                    }
                                }

                                if (connectionTestState is GeminiConnectionTestState.Connected) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                            .border(
                                                width = 1.dp,
                                                color = neonBlue.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(12.dp),
                                            )
                                            .padding(12.dp)
                                            .testTag("gemini_test_success_card"),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CloudDone,
                                                    contentDescription = null,
                                                    tint = neonBlue,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    text = stringResource(
                                                        id = R.string.gemini_test_connected_badge,
                                                        connectionTestState.modelName,
                                                        connectionTestState.latencyMs,
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = neonBlue,
                                                )
                                            }
                                            Text(
                                                text = connectionTestState.sampleReply,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }

                                if (connectionTestState is GeminiConnectionTestState.Error) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = connectionTestState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Per-User Gemini Usage Summary Card
                        item {
                            FutureGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(18.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = electricPurple,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = stringResource(id = R.string.gemini_usage_heading),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                when (usageState) {
                                    is AccountUiState.Loading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = neonBlue,
                                        )
                                    }

                                    is AccountUiState.Error -> {
                                        Text(
                                            text = usageState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }

                                    is AccountUiState.Success -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            UsageStatBox(
                                                value = totalRequests.toString(),
                                                label = stringResource(id = R.string.gemini_usage_total_calls),
                                                accent = neonBlue,
                                                modifier = Modifier.weight(1f),
                                            )
                                            UsageStatBox(
                                                value = estimatedTokens.toString(),
                                                label = stringResource(id = R.string.gemini_usage_est_tokens),
                                                accent = electricPurple,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }

                                        if (toolBreakdown.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(id = R.string.gemini_usage_by_tool),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            toolBreakdown.forEach { (tool, count) ->
                                                val progress = (count.toFloat() / totalRequests.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            text = toolDisplayName(tool),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        )
                                                        Text(
                                                            text = "$count",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = neonBlue,
                                                        )
                                                    }
                                                    LinearProgressIndicator(
                                                        progress = { progress },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(50)),
                                                        color = neonBlue,
                                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stringResource(id = R.string.gemini_usage_empty_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Recent Per-User Gemini Requests Log
                        if (events.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(id = R.string.gemini_usage_recent_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            items(
                                items = events.take(20),
                                key = { it.id },
                            ) { event ->
                                UsageEventRowCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCheckRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UsageStatBox(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UsageEventRowCard(event: GeminiUsageEvent) {
    val timeText = remember(event.createdAt) {
        event.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
        } ?: "अभी"
    }
    val estTokens = (event.promptChars + event.responseChars) / 4

    FutureGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        accentBorder = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Column {
                    Text(
                        text = toolDisplayName(event.toolType),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${event.modelName} • ~$estTokens tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun toolDisplayName(toolType: String): String = when (toolType) {
    "ai_chat" -> "AI Chat"
    "ai_voice_assistant" -> "Voice Welcome Assistant"
    "ai_writer" -> "AI Content Writer"
    "ai_translator" -> "AI Translator"
    "ai_image" -> "AI Image Studio"
    "ai_video" -> "AI Video Studio"
    "ai_music" -> "AI Music Studio"
    "ai_voice" -> "AI Voice Studio"
    "connection_test" -> "Server Connection Test"
    else -> toolType
}
