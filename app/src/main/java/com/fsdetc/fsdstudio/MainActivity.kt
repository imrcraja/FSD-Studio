package com.fsdetc.fsdstudio

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fsdetc.fsdstudio.data.UserSettings
import com.fsdetc.fsdstudio.ui.FsdNavHost
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import com.fsdetc.fsdstudio.ui.theme.FsdStudioTheme
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import java.util.Locale

/** Single activity; all screens are Compose destinations (see ui/Nav.kt). */
class MainActivity : ComponentActivity() {

    /**
     * Apply the in-app language before anything is created. DataStore is
     * asynchronous, so the language is read from the SharedPreferences
     * mirror that UserSettings keeps in sync ("fsd_prefs"/"app_language").
     */
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(
            UserSettings.LANGUAGE_PREFS, Context.MODE_PRIVATE,
        )
        val code = prefs.getString(UserSettings.LANGUAGE_KEY, UserSettings.DEFAULT_LANGUAGE)
            ?: UserSettings.DEFAULT_LANGUAGE
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(code))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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
