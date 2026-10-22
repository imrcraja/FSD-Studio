package com.fsdetc.fsdstudio

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fsdetc.fsdstudio.data.UserSettings
import com.fsdetc.fsdstudio.ui.FsdNavHost
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import com.fsdetc.fsdstudio.ui.theme.FsdStudioTheme
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

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
        val welcomePrefs = getSharedPreferences("rc_raja_welcome", Context.MODE_PRIVATE)
        setContent {
            val theme by container.userSettings.theme
                .collectAsState(initial = AppTheme.FSD_PURPLE)
            FsdStudioTheme(theme) {
                val mainVm: MainViewModel = viewModel()
                val aiVm: AiViewModel = viewModel()
                val historyVm: HistoryViewModel = viewModel()
                val editorVm: com.fsdetc.fsdstudio.viewmodel.SkinEditorViewModel = viewModel()
                val totemVm: com.fsdetc.fsdstudio.viewmodel.TotemViewModel = viewModel()
                FsdNavHost(
                    mainVm = mainVm,
                    aiVm = aiVm,
                    historyVm = historyVm,
                    editorVm = editorVm,
                    totemVm = totemVm,
                )

                var showWelcome by remember {
                    mutableStateOf(!welcomePrefs.getBoolean("shown", false))
                }
                if (showWelcome) {
                    AlertDialog(
                        onDismissRequest = {
                            showWelcome = false
                            welcomePrefs.edit().putBoolean("shown", true).apply()
                        },
                        title = { Text("Unlocked by RC RAJA GAMER 2.0") },
                        text = { Text("All premium features, downloads and AI edits are free and unlimited in this build.") },
                        confirmButton = {
                            Button(onClick = {
                                showWelcome = false
                                welcomePrefs.edit().putBoolean("shown", true).apply()
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://youtube.com/@RCRAJAGAMER2.0"),
                                    ),
                                )
                            }) { Text("Subscribe") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showWelcome = false
                                welcomePrefs.edit().putBoolean("shown", true).apply()
                            }) { Text("Close") }
                        },
                    )
                }
            }
        }
    }
}
