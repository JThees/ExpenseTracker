package com.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.expensetracker.data.local.entity.SettingsEntity
import com.expensetracker.data.repository.SettingsRepository
import com.expensetracker.ui.navigation.AppNavigation
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by settingsRepository.getSettings()
                .map { it?.darkMode ?: false }
                .collectAsState(initial = false)

            ExpenseTrackerTheme(darkTheme = darkMode) {
                AppNavigation()
            }
        }
    }
}
