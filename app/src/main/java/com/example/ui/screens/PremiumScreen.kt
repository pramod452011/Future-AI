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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonGradientButton

@Composable
fun PremiumScreen(
    onOpenAiChat: () -> Unit,
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
                        .widthIn(max = 640.dp)
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
                            modifier = Modifier.testTag("premium_back_button"),
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
                                text = stringResource(id = R.string.premium_screen_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(id = R.string.premium_screen_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = electricPurple,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Transparent notice — no real billing active
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                .border(
                                    width = 1.dp,
                                    color = neonBlue.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = neonBlue,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.premium_transparent_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Current Free Starter Plan
                        FutureGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_free_plan_card"),
                            contentPadding = PaddingValues(20.dp),
                            accentBorder = true,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.premium_free_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(neonBlue.copy(alpha = 0.2f))
                                        .border(1.dp, neonBlue, RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.premium_badge_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = neonBlue,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(id = R.string.premium_free_price),
                                style = MaterialTheme.typography.titleMedium,
                                color = neonBlue,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            PlanFeatureItem(stringResource(id = R.string.premium_free_feat_1))
                            PlanFeatureItem(stringResource(id = R.string.premium_free_feat_2))
                            PlanFeatureItem(stringResource(id = R.string.premium_free_feat_3))
                            PlanFeatureItem(stringResource(id = R.string.premium_free_feat_4))
                        }

                        // Future AI Pro Roadmap Preview Card
                        FutureGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_pro_plan_card"),
                            contentPadding = PaddingValues(0.dp),
                            accentBorder = true,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                electricPurple.copy(alpha = 0.24f),
                                                neonBlue.copy(alpha = 0.14f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .padding(20.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                                imageVector = Icons.Rounded.WorkspacePremium,
                                                contentDescription = null,
                                                tint = electricPurple,
                                                modifier = Modifier.size(26.dp),
                                            )
                                            Text(
                                                text = stringResource(id = R.string.premium_pro_title),
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(electricPurple.copy(alpha = 0.22f))
                                                .border(1.dp, electricPurple, RoundedCornerShape(50))
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.premium_badge_preview),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = electricPurple,
                                            )
                                        }
                                    }

                                    Text(
                                        text = stringResource(id = R.string.premium_pro_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    PlanFeatureItem(stringResource(id = R.string.premium_pro_feat_1))
                                    PlanFeatureItem(stringResource(id = R.string.premium_pro_feat_2))
                                    PlanFeatureItem(stringResource(id = R.string.premium_pro_feat_3))
                                    PlanFeatureItem(stringResource(id = R.string.premium_pro_feat_4))
                                }
                            }
                        }

                        NeonGradientButton(
                            text = stringResource(id = R.string.premium_use_free_cta),
                            icon = Icons.Rounded.AutoAwesome,
                            onClick = onOpenAiChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_open_chat_button"),
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
