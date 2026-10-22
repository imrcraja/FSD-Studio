package com.fsdetc.fsdstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fsdetc.fsdstudio.ui.FsdNavHost
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import com.fsdetc.fsdstudio.ui.theme.FsdStudioTheme
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import com.fsdetc.fsdstudio.viewmodel.MainViewModel

/** Single activity; all screens are Compose destinations (see ui/Nav.kt). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FsdStudioApp).container
        setContent {
            val theme by container.userSettings.theme
                .collectAsState(initial = AppTheme.FSD_PURPLE)
            FsdStudioTheme(theme) {
                // Activity-scoped ViewModels, shared by every destination.
                val mainVm: MainViewModel = viewModel()
                val aiVm: AiViewModel = viewModel()
                val historyVm: HistoryViewModel = viewModel()
                FsdNavHost(mainVm = mainVm, aiVm = aiVm, historyVm = historyVm)
            }
        }
    }
}
