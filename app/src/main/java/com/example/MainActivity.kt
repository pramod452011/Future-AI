package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.data.authStateFlow
import com.example.data.signOutWithCredentialManager
import com.example.ui.navigation.FutureAiRoutes
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AiImageScreen
import com.example.ui.screens.AiMusicScreen
import com.example.ui.screens.AiTranslatorScreen
import com.example.ui.screens.AiVideoScreen
import com.example.ui.screens.AiVoiceScreen
import com.example.ui.screens.AiWriterScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatHistoryScreen
import com.example.ui.screens.GeminiStatusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceWelcomeAssistantScreen
import com.example.ui.theme.FutureAiTheme
import com.example.viewmodel.AccountUiState
import com.example.viewmodel.AccountViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            FutureAiTheme {
                FutureAiAppRoot()
            }
        }
    }
}

@Composable
fun FutureAiAppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by remember { authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var showWelcome by rememberSaveable { mutableStateOf(true) }

    when {
        showWelcome -> {
            VoiceWelcomeAssistantScreen(
                onContinueToApp = { showWelcome = false },
                showBackArrow = false,
            )
        }

        currentUser == null -> {
            // Unauthenticated state: render AuthScreen and never instantiate Firestore ViewModels
            AuthScreen()
        }

        else -> {
            val signedInUser = currentUser!!
            key(signedInUser.uid) {
                AuthenticatedFutureAiApp(
                    user = signedInUser,
                    onReplayWelcome = { showWelcome = true },
                    onSignOut = {
                        coroutineScope.launch {
                            signOutWithCredentialManager(context)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedFutureAiApp(
    user: FirebaseUser,
    onReplayWelcome: () -> Unit,
    onSignOut: () -> Unit,
) {
    val accountViewModel: AccountViewModel = viewModel(
        key = user.uid,
        factory = AccountViewModel.Factory,
    )
    val context = LocalContext.current
    val profileState by accountViewModel.profileState.collectAsStateWithLifecycle()
    val chatHistoryState by accountViewModel.chatHistoryState.collectAsStateWithLifecycle()
    val usageState by accountViewModel.usageState.collectAsStateWithLifecycle()
    val connectionTestState by accountViewModel.connectionTestState.collectAsStateWithLifecycle()
    val actionMessage by accountViewModel.actionMessage.collectAsStateWithLifecycle()
    val isSavingSettings by accountViewModel.isSavingSettings.collectAsStateWithLifecycle()
    val isDeletingAccount by accountViewModel.isDeletingAccount.collectAsStateWithLifecycle()

    val userProfile = (profileState as? AccountUiState.Success)?.data
    val historyCount = (chatHistoryState as? AccountUiState.Success)?.data?.size ?: 0
    val usageCount = (usageState as? AccountUiState.Success)?.data?.size ?: 0
    val fallbackName = user.displayName.orEmpty().ifBlank {
        user.email?.substringBefore("@").orEmpty()
    }
    val fallbackEmail = user.email.orEmpty()
    val fallbackPhoto = user.photoUrl?.toString()

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FutureAiRoutes.ROUTE_HOME,
    ) {
        composable(route = FutureAiRoutes.ROUTE_HOME) {
            HomeScreen(
                onOpenTool = { toolId ->
                    when (toolId) {
                        "ai_chat" -> navController.navigate(FutureAiRoutes.ROUTE_AI_CHAT)
                        "ai_image" -> navController.navigate(FutureAiRoutes.ROUTE_AI_IMAGE)
                        "ai_video" -> navController.navigate(FutureAiRoutes.ROUTE_AI_VIDEO)
                        "ai_music" -> navController.navigate(FutureAiRoutes.ROUTE_AI_MUSIC)
                        "ai_writer" -> navController.navigate(FutureAiRoutes.ROUTE_AI_WRITER)
                        "ai_translator" -> navController.navigate(FutureAiRoutes.ROUTE_AI_TRANSLATOR)
                        "ai_voice" -> navController.navigate(FutureAiRoutes.ROUTE_AI_VOICE)
                        "chat_history" -> navController.navigate(FutureAiRoutes.ROUTE_CHAT_HISTORY)
                        "premium" -> navController.navigate(FutureAiRoutes.ROUTE_PREMIUM)
                        "settings" -> navController.navigate(FutureAiRoutes.ROUTE_SETTINGS)
                        "gemini_status" -> navController.navigate(FutureAiRoutes.ROUTE_GEMINI_STATUS)
                    }
                },
                onReplayWelcome = onReplayWelcome,
                onOpenVoiceAssistant = {
                    navController.navigate(FutureAiRoutes.ROUTE_VOICE_ASSISTANT)
                },
                userProfile = userProfile,
                userDisplayName = fallbackName,
                userEmail = fallbackEmail,
                userPhotoUrl = fallbackPhoto,
                chatHistoryCount = historyCount,
                geminiUsageCount = usageCount,
                onChatExchangeCompleted = { prompt, reply ->
                    accountViewModel.saveChatExchange(prompt, reply, "ai_chat")
                },
                onSignOut = onSignOut,
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_CHAT) {
            AiChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onExchangeCompleted = { prompt, reply ->
                    accountViewModel.saveChatExchange(prompt, reply, "ai_chat")
                },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_IMAGE) {
            AiImageScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_VIDEO) {
            AiVideoScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_MUSIC) {
            AiMusicScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_WRITER) {
            AiWriterScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_TRANSLATOR) {
            AiTranslatorScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_AI_VOICE) {
            AiVoiceScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_CHAT_HISTORY) {
            ChatHistoryScreen(
                historyState = chatHistoryState,
                actionMessage = actionMessage,
                onClearActionMessage = { accountViewModel.clearActionMessage() },
                onDeleteEntry = { chatId -> accountViewModel.deleteChatEntry(chatId) },
                onOpenAiChat = {
                    navController.navigate(FutureAiRoutes.ROUTE_AI_CHAT)
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_PREMIUM) {
            PremiumScreen(
                onOpenAiChat = {
                    navController.navigate(FutureAiRoutes.ROUTE_AI_CHAT)
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_SETTINGS) {
            SettingsScreen(
                userProfile = userProfile,
                fallbackName = fallbackName,
                fallbackEmail = fallbackEmail,
                isSaving = isSavingSettings,
                isDeletingAccount = isDeletingAccount,
                actionMessage = actionMessage,
                onClearActionMessage = { accountViewModel.clearActionMessage() },
                onSaveSettings = { name, language, glow ->
                    accountViewModel.updateSettings(name, language, glow)
                },
                onReplayWelcome = onReplayWelcome,
                onSignOut = onSignOut,
                onDeleteAccount = { accountViewModel.deleteAccount(context) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_VOICE_ASSISTANT) {
            VoiceWelcomeAssistantScreen(
                onContinueToApp = { navController.popBackStack() },
                showBackArrow = true,
                onExchangeCompleted = { prompt, reply ->
                    accountViewModel.saveChatExchange(prompt, reply, "ai_voice_assistant")
                },
            )
        }

        composable(route = FutureAiRoutes.ROUTE_GEMINI_STATUS) {
            GeminiStatusScreen(
                userEmail = fallbackEmail,
                usageState = usageState,
                connectionTestState = connectionTestState,
                onRunConnectionTest = { accountViewModel.runGeminiConnectionTest() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FutureAiHomePreview() {
    FutureAiTheme {
        HomeScreen(onOpenTool = {}, onReplayWelcome = {})
    }
}
