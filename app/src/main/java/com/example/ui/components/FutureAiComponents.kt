package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Shared futuristic Dark Navy atmospheric canvas with gentle Neon Blue & Purple ambient orbs.
 * Every screen in Future AI wraps its layout with this container for a unified look.
 */
@Composable
fun FutureAiBackground(
    modifier: Modifier = Modifier,
    glowIntensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_nebula")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula_drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                val width = size.width
                val height = size.height

                // Subtle futuristic coordinate grid
                val step = 56.dp.toPx()
                var x = 0f
                while (x <= width) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.22f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f,
                    )
                    x += step
                }
                var y = 0f
                while (y <= height) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                    )
                    y += step
                }

                // Top-left Neon Blue glow orb
                val blueCenter = Offset(
                    x = width * (0.18f + 0.08f * drift),
                    y = height * (0.12f + 0.04f * drift),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonBlue.copy(alpha = 0.22f * glowIntensity),
                            Color.Transparent,
                        ),
                        center = blueCenter,
                        radius = width * 0.72f,
                    ),
                    radius = width * 0.72f,
                    center = blueCenter,
                )

                // Bottom-right Electric Purple glow orb
                val purpleCenter = Offset(
                    x = width * (0.85f - 0.08f * drift),
                    y = height * (0.78f - 0.05f * drift),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            electricPurple.copy(alpha = 0.26f * glowIntensity),
                            Color.Transparent,
                        ),
                        center = purpleCenter,
                        radius = width * 0.8f,
                    ),
                    radius = width * 0.8f,
                    center = purpleCenter,
                )
            },
        content = content,
    )
}

/**
 * Glowing AI Robot Logo emblem with animated Neon Blue -> Electric Purple ring.
 */
@Composable
fun FutureAiLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    animatePulse: Boolean = true,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background

    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )

    val effectiveScale = if (animatePulse) pulse else 1f
    val ringBrush = Brush.linearGradient(listOf(neonBlue, electricPurple, neonBlue))

    Box(
        modifier = modifier.size(size + 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ambient halo
        Box(
            modifier = Modifier
                .size(size + 18.dp)
                .scale(effectiveScale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonBlue.copy(alpha = 0.34f),
                                electricPurple.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
        )

        // Glowing neon ring + robot logo
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(width = 2.5.dp, brush = ringBrush, shape = CircleShape)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_future_ai_logo),
                contentDescription = stringResource(id = R.string.cd_future_ai_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
    }
}

/**
 * Frosted Dark Navy glass card with subtle Neon Blue -> Electric Purple border.
 */
@Composable
fun FutureGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    accentBorder: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline

    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            listOf(
                neonBlue.copy(alpha = 0.55f),
                electricPurple.copy(alpha = 0.45f),
            ),
        )
    } else {
        Brush.linearGradient(listOf(outline.copy(alpha = 0.5f), outline.copy(alpha = 0.25f)))
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = neonBlue),
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = surfaceContainer.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Primary futuristic gradient button with smooth spring press animation.
 */
@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val onBackground = MaterialTheme.colorScheme.onBackground

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "button_scale",
    )

    val gradient = Brush.horizontalGradient(
        colors = if (enabled) {
            listOf(neonBlue, electricPurple)
        } else {
            listOf(neonBlue.copy(alpha = 0.35f), electricPurple.copy(alpha = 0.35f))
        },
    )

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush = gradient)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = onBackground),
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onBackground,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = onBackground,
            )
        }
    }
}

/**
 * Secondary glowing outline button for futuristic actions.
 */
@Composable
fun NeonOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "outline_button_scale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceHigh.copy(alpha = 0.55f))
            .border(
                width = 1.2.dp,
                color = neonBlue.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = neonBlue),
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = neonBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = onSurface,
            )
        }
    }
}
