package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AccentTone
import com.example.model.FutureAiCatalog
import com.example.model.FutureAiTool
import com.example.model.ToolCategory
import com.example.model.UserProfile
import com.example.ui.components.FutureAiBackground
import com.example.ui.components.FutureAiLogoBadge
import com.example.ui.components.FutureGlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonOutlineButton

private enum class HomeTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTagName: String,
) {
    HOME(R.string.nav_home, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_home"),
    CHAT(R.string.nav_chat, Icons.Rounded.Forum, Icons.Outlined.Forum, "nav_tab_chat"),
    TOOLS(R.string.nav_tools, Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_tab_tools"),
    PROFILE(R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_tab_profile"),
}

@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onReplayWelcome: () -> Unit,
    onOpenVoiceAssistant: () -> Unit = onReplayWelcome,
    userProfile: UserProfile? = null,
    userDisplayName: String = "",
    userEmail: String = "",
    userPhotoUrl: String? = null,
    chatHistoryCount: Int = 0,
    geminiUsageCount: Int = 0,
    onChatExchangeCompleted: ((prompt: String, reply: String) -> Unit)? = null,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    var selectedCategory by rememberSaveable { mutableStateOf(ToolCategory.ALL) }

    val resolvedName = userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName
    val resolvedEmail = userProfile?.email?.ifBlank { userEmail } ?: userEmail
    val resolvedPhoto = userProfile?.photoUrl?.ifBlank { userPhotoUrl } ?: userPhotoUrl
    val glowEnabled = userProfile?.glowBoostEnabled ?: true

    val filteredTools = remember(selectedCategory) {
        if (selectedCategory == ToolCategory.ALL) {
            FutureAiCatalog.tools
        } else {
            FutureAiCatalog.tools.filter { it.category == selectedCategory }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (selectedTab != HomeTab.CHAT) {
                    FloatingActionButton(
                        onClick = onOpenVoiceAssistant,
                        shape = CircleShape,
                        containerColor = Color.Transparent,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp,
                        ),
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                    ),
                                ),
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.45f),
                                shape = CircleShape,
                            )
                            .testTag("home_floating_mic_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = stringResource(id = R.string.cd_voice_assistant_mic),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    FutureAiBottomNavBar(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                    )
                }
            },
        ) { innerPadding ->
            FutureAiBackground {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        FutureAiNavigationRail(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "home_tab_content",
                        ) { tab ->
                            when (tab) {
                                HomeTab.HOME -> HomeDashboardContent(
                                    displayName = resolvedName,
                                    photoUrl = resolvedPhoto,
                                    glowEnabled = glowEnabled,
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = { selectedCategory = it },
                                    tools = filteredTools,
                                    onOpenTool = onOpenTool,
                                    onReplayWelcome = onReplayWelcome,
                                    onOpenProfileTab = { selectedTab = HomeTab.PROFILE },
                                )

                                HomeTab.CHAT -> AiChatScreen(
                                    onNavigateBack = null,
                                    onExchangeCompleted = onChatExchangeCompleted,
                                )

                                HomeTab.TOOLS -> AllToolsTabContent(
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = { selectedCategory = it },
                                    tools = filteredTools,
                                    onOpenTool = onOpenTool,
                                )

                                HomeTab.PROFILE -> ProfileTabContent(
                                    displayName = resolvedName,
                                    email = resolvedEmail,
                                    photoUrl = resolvedPhoto,
                                    preferredLanguage = userProfile?.preferredLanguage ?: "Hindi",
                                    chatHistoryCount = chatHistoryCount,
                                    geminiUsageCount = geminiUsageCount,
                                    onOpenTool = onOpenTool,
                                    onReplayWelcome = onReplayWelcome,
                                    onSignOut = onSignOut,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    displayName: String,
    photoUrl: String?,
    glowEnabled: Boolean,
    selectedCategory: ToolCategory,
    onSelectCategory: (ToolCategory) -> Unit,
    tools: List<FutureAiTool>,
    onOpenTool: (String) -> Unit,
    onReplayWelcome: () -> Unit,
    onOpenProfileTab: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize(),
    ) {
        val columns = if (maxWidth >= 560.dp) 3 else 2
        val toolRows = remember(tools, columns) { tools.chunked(columns) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                HomeTopHeader(
                    displayName = displayName,
                    photoUrl = photoUrl,
                    glowEnabled = glowEnabled,
                    onReplayWelcome = onReplayWelcome,
                    onOpenProfile = onOpenProfileTab,
                )
            }

            item(key = "hero_banner") {
                HeroAiChatBanner(
                    onOpenChat = { onOpenTool("ai_chat") },
                )
            }

            item(key = "section_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.home_section_tools),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onSelectCategory = onSelectCategory,
                    )
                }
            }

            items(
                count = toolRows.size,
                key = { rowIndex -> toolRows[rowIndex].joinToString("_") { it.id } },
            ) { rowIndex ->
                val rowTools = toolRows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowTools.forEach { tool ->
                        AiToolGridCard(
                            tool = tool,
                            onClick = { onOpenTool(tool.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowTools.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    displayName: String,
    photoUrl: String?,
    glowEnabled: Boolean,
    onReplayWelcome: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val firstName = remember(displayName) {
        displayName.trim().substringBefore(" ").take(20)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            FutureAiLogoBadge(
                size = 44.dp,
                animatePulse = glowEnabled,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (firstName.isNotEmpty()) {
                        stringResource(id = R.string.home_greeting_named, firstName)
                    } else {
                        stringResource(id = R.string.home_greeting)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(id = R.string.home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onReplayWelcome,
                modifier = Modifier.testTag("home_replay_welcome_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = stringResource(id = R.string.cd_replay_welcome),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        shape = CircleShape,
                    )
                    .testTag("home_profile_avatar_button"),
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(id = R.string.cd_open_profile),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(id = R.string.cd_open_profile),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroAiChatBanner(
    onOpenChat: () -> Unit,
) {
    val neonBlue = MaterialTheme.colorScheme.primary
    val electricPurple = MaterialTheme.colorScheme.secondary

    FutureGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_hero_chat_card"),
        contentPadding = PaddingValues(0.dp),
        onClick = onOpenChat,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            neonBlue.copy(alpha = 0.22f),
                            electricPurple.copy(alpha = 0.20f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                        .border(
                            width = 1.dp,
                            color = neonBlue.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = neonBlue,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.home_hero_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = neonBlue,
                    )
                }

                Text(
                    text = stringResource(id = R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = stringResource(id = R.string.home_hero_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                NeonGradientButton(
                    text = stringResource(id = R.string.home_hero_cta),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = onOpenChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_hero_start_chat_button"),
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ToolCategory,
    onSelectCategory: (ToolCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToolCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            val bgColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
            }
            val textColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val borderColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(50))
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("filter_chip_${category.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = category.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun AiToolGridCard(
    tool: FutureAiTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (tool.accentTone) {
        AccentTone.NEON_BLUE -> MaterialTheme.colorScheme.primary
        AccentTone.ELECTRIC_PURPLE -> MaterialTheme.colorScheme.secondary
        AccentTone.PLASMA_CYAN -> MaterialTheme.colorScheme.tertiary
    }
    val containerColor = when (tool.accentTone) {
        AccentTone.NEON_BLUE -> MaterialTheme.colorScheme.primaryContainer
        AccentTone.ELECTRIC_PURPLE -> MaterialTheme.colorScheme.secondaryContainer
        AccentTone.PLASMA_CYAN -> MaterialTheme.colorScheme.tertiaryContainer
    }

    FutureGlassCard(
        modifier = modifier
            .defaultMinSize(minHeight = 148.dp)
            .testTag("tool_card_${tool.id}"),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(16.dp),
        accentBorder = false,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(id = tool.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(id = tool.subtitleRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun AllToolsTabContent(
    selectedCategory: ToolCategory,
    onSelectCategory: (ToolCategory) -> Unit,
    tools: List<FutureAiTool>,
    onOpenTool: (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize(),
    ) {
        val columns = if (maxWidth >= 560.dp) 3 else 2
        val toolRows = remember(tools, columns) { tools.chunked(columns) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("all_tools_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "tools_title") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.home_section_tools),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onSelectCategory = onSelectCategory,
                    )
                }
            }

            items(
                count = toolRows.size,
                key = { rowIndex -> "tools_" + toolRows[rowIndex].joinToString("_") { it.id } },
            ) { rowIndex ->
                val rowTools = toolRows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowTools.forEach { tool ->
                        AiToolGridCard(
                            tool = tool,
                            onClick = { onOpenTool(tool.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowTools.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    displayName: String,
    email: String,
    photoUrl: String?,
    preferredLanguage: String,
    chatHistoryCount: Int,
    geminiUsageCount: Int,
    onOpenTool: (String) -> Unit,
    onReplayWelcome: () -> Unit,
    onSignOut: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .testTag("profile_tab_list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FutureGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp),
                accentBorder = true,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(id = R.string.cd_open_profile),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    } else {
                        FutureAiLogoBadge(size = 86.dp, animatePulse = false)
                    }

                    Text(
                        text = displayName.ifBlank { stringResource(id = R.string.tab_profile_title) },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.profile_cloud_synced),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = preferredLanguage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }

        // Quick Cloud History Stat Card
        item {
            FutureGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_history_card"),
                contentPadding = PaddingValues(18.dp),
                onClick = { onOpenTool("chat_history") },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.tool_history_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(id = R.string.profile_saved_chats_count, chatHistoryCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Gemini Server Connection & Per-User Usage Card
        item {
            FutureGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_gemini_status_card"),
                contentPadding = PaddingValues(18.dp),
                accentBorder = true,
                onClick = { onOpenTool("gemini_status") },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.gemini_status_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(id = R.string.profile_gemini_usage_count, geminiUsageCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonGradientButton(
                    text = stringResource(id = R.string.tool_premium_title),
                    icon = Icons.Rounded.WorkspacePremium,
                    onClick = { onOpenTool("premium") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_premium_button"),
                )
                NeonOutlineButton(
                    text = stringResource(id = R.string.tool_settings_title),
                    icon = Icons.Rounded.Settings,
                    onClick = { onOpenTool("settings") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_settings_button"),
                )
                NeonOutlineButton(
                    text = stringResource(id = R.string.tab_profile_replay_welcome),
                    icon = Icons.Rounded.Replay,
                    onClick = onReplayWelcome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_replay_welcome_button"),
                )
                NeonOutlineButton(
                    text = stringResource(id = R.string.settings_signout_button),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_signout_button"),
                )
            }
        }
    }
}

@Composable
private fun FutureAiBottomNavBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets.navigationBars,
        tonalElevation = 8.dp,
    ) {
        HomeTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(id = tab.labelRes),
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(tab.testTagName),
            )
        }
    }
}

@Composable
private fun FutureAiNavigationRail(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HomeTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(id = tab.labelRes),
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(tab.testTagName),
            )
        }
    }
}
