package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureAiLogoBadge
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonGradientButton
import kotlinx.coroutines.launch

private data class WelcomeSlide(
    val badgeRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accentPrimary: Boolean,
)

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slides = remember {
        listOf(
            WelcomeSlide(
                badgeRes = R.string.welcome_slide_1_badge,
                titleRes = R.string.welcome_slide_1_title,
                descRes = R.string.welcome_slide_1_desc,
                icon = Icons.Rounded.Forum,
                accentPrimary = true,
            ),
            WelcomeSlide(
                badgeRes = R.string.welcome_slide_2_badge,
                titleRes = R.string.welcome_slide_2_title,
                descRes = R.string.welcome_slide_2_desc,
                icon = Icons.Rounded.Brush,
                accentPrimary = false,
            ),
            WelcomeSlide(
                badgeRes = R.string.welcome_slide_3_badge,
                titleRes = R.string.welcome_slide_3_title,
                descRes = R.string.welcome_slide_3_desc,
                icon = Icons.Rounded.RecordVoiceOver,
                accentPrimary = true,
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    var heroVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        heroVisible = true
    }

    val isLastPage = pagerState.currentPage == slides.lastIndex

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        FutureAiBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top Bar with futuristic status pill & Skip action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(50),
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.welcome_brand_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        TextButton(
                            onClick = onGetStarted,
                            modifier = Modifier.testTag("welcome_skip_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.welcome_skip),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hero Animated Orbital Logo Section
                    AnimatedVisibility(
                        visible = heroVisible,
                        enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { -28 },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            OrbitalLogoHero()

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = stringResource(id = R.string.welcome_brand_title),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(id = R.string.welcome_brand_tagline),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Swipeable Feature Spotlight Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("welcome_pager"),
                        pageSpacing = 14.dp,
                    ) { page ->
                        WelcomeFeatureCard(slide = slides[page])
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Neon Page Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(slides.size) { index ->
                            val selected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (selected) 28.dp else 8.dp,
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                label = "indicator_width",
                            )
                            val color by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                animationSpec = tween(250),
                                label = "indicator_color",
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary CTA Button (Next -> Launch Future AI)
                    NeonGradientButton(
                        text = if (isLastPage) {
                            stringResource(id = R.string.welcome_get_started)
                        } else {
                            stringResource(id = R.string.welcome_next)
                        },
                        icon = if (isLastPage) {
                            Icons.Rounded.RocketLaunch
                        } else {
                            Icons.AutoMirrored.Rounded.ArrowForward
                        },
                        onClick = {
                            if (isLastPage) {
                                onGetStarted()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (isLastPage) "welcome_start_button" else "welcome_next_button"),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OrbitalLogoHero() {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "orbital_rings")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer_orbit",
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inner_orbit",
    )

    Box(
        modifier = Modifier.size(196.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusOuter = size.minDimension * 0.48f
            val radiusInner = size.minDimension * 0.40f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer rotating neon arc
            rotate(degrees = outerAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            neonBlue.copy(alpha = 0.85f),
                            electricPurple.copy(alpha = 0.75f),
                            Color.Transparent,
                        ),
                    ),
                    startAngle = 0f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
                    size = androidx.compose.ui.geometry.Size(radiusOuter * 2, radiusOuter * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            // Inner counter-rotating dashed highlight arc
            rotate(degrees = innerAngle, pivot = center) {
                drawArc(
                    color = electricPurple.copy(alpha = 0.55f),
                    startAngle = 40f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
                    size = androidx.compose.ui.geometry.Size(radiusInner * 2, radiusInner * 2),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = neonBlue,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x + radiusInner, center.y),
                )
            }
        }

        FutureAiLogoBadge(
            size = 118.dp,
            animatePulse = true,
            modifier = Modifier.testTag("welcome_robot_logo"),
        )
    }
}

@Composable
private fun WelcomeFeatureCard(
    slide: WelcomeSlide,
) {
    val accent = if (slide.accentPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val containerAccent = if (slide.accentPrimary) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    FutureGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerAccent)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(containerAccent.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(id = slide.badgeRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = slide.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = slide.descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
