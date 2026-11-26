package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.PI
import kotlin.math.sin

enum class VoiceVisualMode {
    IDLE,
    SPEAKING,
    LISTENING,
    THINKING,
}

@Composable
fun VoiceRobotAvatar(
    mode: VoiceVisualMode,
    modifier: Modifier = Modifier,
    size: Dp = 216.dp,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val plasmaCyan = MaterialTheme.colorScheme.tertiary

    val activeAccent by animateColorAsState(
        targetValue = when (mode) {
            VoiceVisualMode.IDLE -> neonBlue
            VoiceVisualMode.SPEAKING -> plasmaCyan
            VoiceVisualMode.LISTENING -> electricPurple
            VoiceVisualMode.THINKING -> neonBlue
        },
        animationSpec = tween(400),
        label = "robot_active_accent",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "voice_robot_motion")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    VoiceVisualMode.IDLE -> 11000
                    VoiceVisualMode.SPEAKING -> 5500
                    VoiceVisualMode.LISTENING -> 4200
                    VoiceVisualMode.THINKING -> 2800
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer_orbit_rotation",
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inner_orbit_rotation",
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mode == VoiceVisualMode.IDLE) 2400 else 1200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "robot_pulse",
    )

    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "robot_float",
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("voice_robot_avatar"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension * 0.49f
            val midR = this.size.minDimension * 0.41f
            val innerR = this.size.minDimension * 0.34f

            // Ambient radial plasma glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = 0.28f + 0.16f * pulseProgress),
                        electricPurple.copy(alpha = 0.14f + 0.08f * pulseProgress),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = maxR,
                ),
                radius = maxR,
                center = center,
            )

            // Sonar ripple ring during active listening or speaking
            if (mode == VoiceVisualMode.LISTENING || mode == VoiceVisualMode.SPEAKING) {
                val rippleRadius = innerR + (maxR - innerR) * pulseProgress
                drawCircle(
                    color = activeAccent.copy(alpha = (1f - pulseProgress) * 0.65f),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            // Outer holographic rotating arc
            rotate(degrees = outerRotation, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            activeAccent.copy(alpha = 0.9f),
                            electricPurple.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                    ),
                    startAngle = 0f,
                    sweepAngle = 225f,
                    useCenter = false,
                    topLeft = Offset(center.x - midR, center.y - midR),
                    size = Size(midR * 2f, midR * 2f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = activeAccent,
                    radius = 4.5.dp.toPx(),
                    center = Offset(center.x + midR, center.y),
                )
            }

            // Inner counter-rotating orbit arc
            rotate(degrees = innerRotation, pivot = center) {
                drawArc(
                    color = electricPurple.copy(alpha = 0.65f),
                    startAngle = 35f,
                    sweepAngle = 125f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerR, center.y - innerR),
                    size = Size(innerR * 2f, innerR * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = neonBlue,
                    radius = 3.5.dp.toPx(),
                    center = Offset(center.x - innerR, center.y),
                )
            }
        }

        // Gently floating AI Robot Core Portrait
        Box(
            modifier = Modifier
                .offset(y = floatOffsetDp.dp)
                .size(size * 0.56f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(listOf(activeAccent, electricPurple)),
                    shape = CircleShape,
                ),
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

@Composable
fun VoiceEqualizerWave(
    mode: VoiceVisualMode,
    modifier: Modifier = Modifier,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary
    val plasmaCyan = MaterialTheme.colorScheme.tertiary

    val isActive = mode != VoiceVisualMode.IDLE
    val infiniteTransition = rememberInfiniteTransition(label = "voice_equalizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 900 else 2400,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )

    val barCount = 11
    Row(
        modifier = modifier
            .height(42.dp)
            .testTag("voice_equalizer_wave"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until barCount) {
            val centerFactor = 1f - kotlin.math.abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f + 0.6f)
            val wave = ((sin(phase + i * 0.68f) + 1f) / 2f)
            val minBar = 7f
            val maxBar = if (isActive) 38f else 15f
            val heightDp = (minBar + (maxBar - minBar) * centerFactor * (0.35f + 0.65f * wave)).dp

            val barColor = when {
                mode == VoiceVisualMode.LISTENING -> if (i % 2 == 0) electricPurple else neonBlue
                mode == VoiceVisualMode.SPEAKING -> if (i % 2 == 0) plasmaCyan else neonBlue
                else -> neonBlue.copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
    }
}
