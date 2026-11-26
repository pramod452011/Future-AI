package com.example.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class ToolCategory(@StringRes val labelRes: Int) {
    ALL(R.string.home_filter_all),
    CREATIVE(R.string.home_filter_creative),
    LANGUAGE(R.string.home_filter_language),
    ACCOUNT(R.string.home_filter_account),
}

enum class AccentTone {
    NEON_BLUE,
    ELECTRIC_PURPLE,
    PLASMA_CYAN,
}

data class FutureAiTool(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val stepLabelRes: Int,
    val icon: ImageVector,
    val category: ToolCategory,
    val accentTone: AccentTone,
)

object FutureAiCatalog {
    val tools: List<FutureAiTool> = listOf(
        FutureAiTool(
            id = "ai_chat",
            titleRes = R.string.tool_chat_title,
            subtitleRes = R.string.tool_chat_desc,
            stepLabelRes = R.string.step_tag_5,
            icon = Icons.Rounded.Forum,
            category = ToolCategory.LANGUAGE,
            accentTone = AccentTone.NEON_BLUE,
        ),
        FutureAiTool(
            id = "ai_image",
            titleRes = R.string.tool_image_title,
            subtitleRes = R.string.tool_image_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.Image,
            category = ToolCategory.CREATIVE,
            accentTone = AccentTone.ELECTRIC_PURPLE,
        ),
        FutureAiTool(
            id = "ai_video",
            titleRes = R.string.tool_video_title,
            subtitleRes = R.string.tool_video_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.Movie,
            category = ToolCategory.CREATIVE,
            accentTone = AccentTone.PLASMA_CYAN,
        ),
        FutureAiTool(
            id = "ai_music",
            titleRes = R.string.tool_music_title,
            subtitleRes = R.string.tool_music_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.MusicNote,
            category = ToolCategory.CREATIVE,
            accentTone = AccentTone.ELECTRIC_PURPLE,
        ),
        FutureAiTool(
            id = "ai_writer",
            titleRes = R.string.tool_writer_title,
            subtitleRes = R.string.tool_writer_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.AutoStories,
            category = ToolCategory.CREATIVE,
            accentTone = AccentTone.NEON_BLUE,
        ),
        FutureAiTool(
            id = "ai_translator",
            titleRes = R.string.tool_translator_title,
            subtitleRes = R.string.tool_translator_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.Translate,
            category = ToolCategory.LANGUAGE,
            accentTone = AccentTone.PLASMA_CYAN,
        ),
        FutureAiTool(
            id = "ai_voice",
            titleRes = R.string.tool_voice_title,
            subtitleRes = R.string.tool_voice_desc,
            stepLabelRes = R.string.step_tag_6,
            icon = Icons.Rounded.RecordVoiceOver,
            category = ToolCategory.LANGUAGE,
            accentTone = AccentTone.ELECTRIC_PURPLE,
        ),
        FutureAiTool(
            id = "chat_history",
            titleRes = R.string.tool_history_title,
            subtitleRes = R.string.tool_history_desc,
            stepLabelRes = R.string.step_tag_8,
            icon = Icons.Rounded.History,
            category = ToolCategory.ACCOUNT,
            accentTone = AccentTone.NEON_BLUE,
        ),
        FutureAiTool(
            id = "premium",
            titleRes = R.string.tool_premium_title,
            subtitleRes = R.string.tool_premium_desc,
            stepLabelRes = R.string.step_tag_8,
            icon = Icons.Rounded.WorkspacePremium,
            category = ToolCategory.ACCOUNT,
            accentTone = AccentTone.ELECTRIC_PURPLE,
        ),
        FutureAiTool(
            id = "settings",
            titleRes = R.string.tool_settings_title,
            subtitleRes = R.string.tool_settings_desc,
            stepLabelRes = R.string.step_tag_8,
            icon = Icons.Rounded.Settings,
            category = ToolCategory.ACCOUNT,
            accentTone = AccentTone.PLASMA_CYAN,
        ),
    )

    fun findById(id: String): FutureAiTool =
        tools.firstOrNull { it.id == id } ?: tools.first()
}
