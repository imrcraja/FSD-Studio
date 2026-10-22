package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import com.fsdetc.fsdstudio.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val POSES = "poses"
    const val STUDIO = "studio"
    const val AI = "ai"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PRO = "pro"
    const val DETAIL = "detail"
    const val LIBRARY = "library"
    const val EDITOR = "editor"
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.POSES, R.string.tab_poses, Icons.Filled.AccessibilityNew),
    Tab(Routes.STUDIO, R.string.tab_studio, Icons.Filled.Tune),
    Tab(Routes.AI, R.string.tab_ai, Icons.Filled.AutoAwesome),
    Tab(Routes.HISTORY, R.string.tab_history, Icons.Filled.History),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
)

@Composable
fun FsdNavHost(
    mainVm: MainViewModel,
    aiVm: AiViewModel,
    historyVm: HistoryViewModel,
    editorVm: com.fsdetc.fsdstudio.viewmodel.SkinEditorViewModel,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val seenTutorial by mainVm.seenTutorial.collectAsState()
    val tutorialActive = seenTutorial != true
    val showBar = !tutorialActive && currentRoute in TABS.map { it.route }

    // Transient app messages (saved / errors / limits) appear in this
    // bottom-anchored host, above the navigation bar — never as top banners
    // covering content (e.g. the Max plan card on the Plans screen).
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        mainVm.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun goHome() {
        nav.navigate(Routes.HOME) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (seenTutorial) {
                // Still loading the flag: blank for a frame, never a flash of app.
                null -> Box(Modifier.fillMaxSize())
                // First run (or a Settings replay): tutorial instead of the app.
                false -> TutorialScreen(onDone = { mainVm.markTutorialSeen() })
                else -> NavHost(navController = nav, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            mainVm = mainVm,
                            onOpenDetail = { req ->
                                mainVm.openDetail(req)
                                nav.navigate(Routes.DETAIL)
                            },
                            onOpenPro = { nav.navigate(Routes.PRO) },
                            onOpenLibrary = { nav.navigate(Routes.LIBRARY) },
                            onOpenEditor = { nav.navigate(Routes.EDITOR) },
                        )
                    }
                    composable(Routes.POSES) {
                        PosesScreen(
                            mainVm = mainVm,
                            onOpenDetail = { req ->
                                mainVm.openDetail(req)
                                nav.navigate(Routes.DETAIL)
                            },
                            onOpenPro = { nav.navigate(Routes.PRO) },
                        )
                    }
                    composable(Routes.STUDIO) {
                        StudioScreen(
                            mainVm = mainVm,
                            onOpenPro = { nav.navigate(Routes.PRO) },
                            onOpenHome = { goHome() },
                            onOpenLibrary = { nav.navigate(Routes.LIBRARY) },
                        )
                    }
                    composable(Routes.AI) {
                        AiScreen(
                            aiVm = aiVm,
                            onOpenPro = { nav.navigate(Routes.PRO) },
                        )
                    }
                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            historyVm = historyVm,
                            onOpenDetail = { req ->
                                mainVm.openDetail(req)
                                nav.navigate(Routes.DETAIL)
                            },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onOpenPro = { nav.navigate(Routes.PRO) },
                            onReplayTutorial = { mainVm.replayTutorial() },
                        )
                    }
                    composable(Routes.PRO) {
                        SubscriptionScreen(onBack = { nav.popBackStack() })
                    }
                    composable(Routes.LIBRARY) {
                        LibraryScreen(
                            onBack = { nav.popBackStack() },
                            onSelectPlayer = { player ->
                                player.character?.let { mainVm.selectCharacter(it) }
                                player.searchName?.let { mainVm.search(it) }
                                goHome()
                            },
                            onSelectMob = { mob ->
                                mainVm.openDetail(
                                    DetailRequest.MobRender(
                                        mobId = mob.id,
                                        mobLabel = mob.label,
                                        poses = mob.poses,
                                    ),
                                )
                                nav.navigate(Routes.DETAIL)
                            },
                        )
                    }
                    composable(Routes.DETAIL) {
                        RenderDetailScreen(
                            mainVm = mainVm,
                            onBack = { nav.popBackStack() },
                            onOpenPro = { nav.navigate(Routes.PRO) },
                            onOpenEditor = { nav.navigate(Routes.EDITOR) },
                        )
                    }
                    composable(Routes.EDITOR) {
                        SkinEditorScreen(
                            mainVm = mainVm,
                            editorVm = editorVm,
                            onBack = { nav.popBackStack() },
                            onOpenPro = { nav.navigate(Routes.PRO) },
                        )
                    }
                }
            }
        }
    }

    // The one gentle upgrade pop-up: fired by the 3rd successful free download
    // of the day (the gate marks it shown before emitting, so ≤ once per day,
    // and it never appears on app start).
    val showPopup by mainVm.showUpgradePopup.collectAsState()
    if (showPopup && !tutorialActive) {
        AlertDialog(
            onDismissRequest = { mainVm.dismissUpgradePopup() },
            title = { Text(stringResource(R.string.popup_title)) },
            text = { Text(stringResource(R.string.popup_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainVm.dismissUpgradePopup()
                        nav.navigate(Routes.PRO)
                    },
                ) { Text(stringResource(R.string.popup_see_plans)) }
            },
            dismissButton = {
                TextButton(onClick = { mainVm.dismissUpgradePopup() }) {
                    Text(stringResource(R.string.popup_not_now))
                }
            },
        )
    }
}
