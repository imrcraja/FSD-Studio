package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.ui.theme.FsdSurface
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import com.fsdetc.fsdstudio.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val POSES = "poses"
    const val AI = "ai"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PRO = "pro"
    const val DETAIL = "detail"
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.POSES, R.string.tab_poses, Icons.Filled.AccessibilityNew),
    Tab(Routes.AI, R.string.tab_ai, Icons.Filled.AutoAwesome),
    Tab(Routes.HISTORY, R.string.tab_history, Icons.Filled.History),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
)

@Composable
fun FsdNavHost(mainVm: MainViewModel, aiVm: AiViewModel, historyVm: HistoryViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = FsdSurface) {
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
            NavHost(navController = nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        mainVm = mainVm,
                        onOpenDetail = { req ->
                            mainVm.openDetail(req)
                            nav.navigate(Routes.DETAIL)
                        },
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
                composable(Routes.AI) {
                    AiScreen(aiVm = aiVm)
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
                    SettingsScreen(onOpenPro = { nav.navigate(Routes.PRO) })
                }
                composable(Routes.PRO) {
                    ProScreen(onBack = { nav.popBackStack() })
                }
                composable(Routes.DETAIL) {
                    RenderDetailScreen(
                        mainVm = mainVm,
                        onBack = { nav.popBackStack() },
                        onOpenPro = { nav.navigate(Routes.PRO) },
                    )
                }
            }
        }
    }
}
