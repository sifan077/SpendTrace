package com.sifan.spendtrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sifan.spendtrace.data.ThemeMode
import com.sifan.spendtrace.ui.SpendTraceApp
import com.sifan.spendtrace.ui.home.SpendTraceViewModel
import com.sifan.spendtrace.ui.theme.SpendTraceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as SpendTraceApplication).container
        setContent {
            val viewModel: SpendTraceViewModel = viewModel(
                factory = SpendTraceViewModel.factory(container)
            )
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SpendTraceTheme(darkTheme = darkTheme) {
                SpendTraceApp(
                    viewModel = viewModel,
                    backupService = container.backupService,
                )
            }
        }
    }
}
