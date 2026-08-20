package com.zenlabs.msg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zenlabs.msg.data.ZenRepository
import com.zenlabs.msg.nav.Dest
import com.zenlabs.msg.ui.ChatScreen
import com.zenlabs.msg.ui.ChatViewModel
import com.zenlabs.msg.ui.ConversationListScreen
import com.zenlabs.msg.ui.ConversationListViewModel
import com.zenlabs.msg.ui.NewConversationScreen
import com.zenlabs.msg.ui.NewConversationViewModel
import com.zenlabs.msg.ui.theme.ZenMsgTheme

class MainActivity : ComponentActivity() {

    private lateinit var repo: ZenRepository

    private val requiredPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ZenRepository(applicationContext)

        ensurePermissions()

        val initialConversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1L) ?: -1L

        setContent {
            ZenMsgTheme {
                val navController = rememberNavController()
                var startRoute by remember { mutableStateOf(Dest.Conversations.route) }

                LaunchedEffect(initialConversationId) {
                    if (initialConversationId > 0) {
                        navController.navigate(Dest.Chat.build(initialConversationId)) {
                            popUpTo(Dest.Conversations.route)
                        }
                    }
                }

                NavHost(navController, startDestination = startRoute) {
                    composable(Dest.Conversations.route) {
                        val vm: ConversationListViewModel = viewModel(
                            factory = ConversationListViewModel.factory(repo)
                        )
                        ConversationListScreen(
                            viewModel = vm,
                            onOpenConversation = { id -> navController.navigate(Dest.Chat.build(id)) },
                            onNewConversation = { navController.navigate(Dest.New.route) }
                        )
                    }
                    composable(Dest.New.route) {
                        val vm: NewConversationViewModel = viewModel(
                            factory = NewConversationViewModel.factory(repo)
                        )
                        NewConversationScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() },
                            onCreated = { id ->
                                navController.navigate(Dest.Chat.build(id)) {
                                    popUpTo(Dest.Conversations.route)
                                }
                            }
                        )
                    }
                    composable(
                        route = Dest.Chat.route,
                        arguments = listOf(
                            navArgument(Dest.Chat.ARG) { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val conversationId =
                            backStackEntry.arguments?.getLong(Dest.Chat.ARG) ?: 0L
                        val vm: ChatViewModel = viewModel(
                            factory = ChatViewModel.factory(repo, conversationId)
                        )
                        ChatScreen(viewModel = vm, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    private fun ensurePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
