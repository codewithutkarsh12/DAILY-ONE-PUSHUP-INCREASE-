package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiCoachViewModel
import com.example.ai.configureAppCheck
import com.example.notifications.ReminderScheduler
import com.example.ui.RepRiseViewModel
import com.example.ui.coach.CoachScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.RepRiseTheme
import java.time.LocalDate

private enum class BottomNavTab {
    HOME,
    COACH,
    HISTORY,
    SETTINGS
}

@SuppressLint("NewApi")
class MainActivity : ComponentActivity() {

    private val viewModel: RepRiseViewModel by viewModels {
        val app = application as RepRiseApplication
        RepRiseViewModel.Factory(
            appContext = applicationContext,
            repository = app.repository,
            settingsStore = app.settingsStore
        )
    }

    private val coachViewModel: AiCoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch Intent before UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val coachUiState by coachViewModel.uiState.collectAsStateWithLifecycle()
            var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }
            var hasNotifPermission by remember {
                mutableStateOf(ReminderScheduler.hasNotificationPermission(this))
            }
            val snackbarHostState = remember { SnackbarHostState() }

            // Permission launcher for Android 13+ POST_NOTIFICATIONS
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                hasNotifPermission = ReminderScheduler.hasNotificationPermission(this)
            }

            // SAF CreateDocument launcher for local CSV export
            val csvExportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("text/csv")
            ) { uri: Uri? ->
                if (uri != null) {
                    runCatching {
                        val csv = viewModel.exportCsvContent()
                        contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(csv.toByteArray(Charsets.UTF_8))
                        }
                        Toast.makeText(this, "CSV exported successfully.", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this, "Failed to export CSV.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Refresh date/streak calculation and notification permission on resume
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNow()
                hasNotifPermission = ReminderScheduler.hasNotificationPermission(this)
            }

            // Request notification permission once on Android 13+ if reminders are enabled
            var promptedOnce by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(uiState.isLoading) {
                if (!uiState.isLoading && !promptedOnce && uiState.settings.reminderEnabled) {
                    promptedOnce = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissError()
                }
            }

            RepRiseTheme(themeMode = uiState.settings.themeMode) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == BottomNavTab.HOME,
                                onClick = {
                                    if (selectedTab != BottomNavTab.HOME) {
                                        selectedTab = BottomNavTab.HOME
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = stringResource(R.string.nav_home)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_home)) },
                                modifier = Modifier.testTag("nav_home")
                            )
                            NavigationBarItem(
                                selected = selectedTab == BottomNavTab.COACH,
                                onClick = {
                                    if (selectedTab != BottomNavTab.COACH) {
                                        selectedTab = BottomNavTab.COACH
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = stringResource(R.string.nav_coach)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_coach)) },
                                modifier = Modifier.testTag("nav_coach")
                            )
                            NavigationBarItem(
                                selected = selectedTab == BottomNavTab.HISTORY,
                                onClick = {
                                    if (selectedTab != BottomNavTab.HISTORY) {
                                        selectedTab = BottomNavTab.HISTORY
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.nav_history)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_history)) },
                                modifier = Modifier.testTag("nav_history")
                            )
                            NavigationBarItem(
                                selected = selectedTab == BottomNavTab.SETTINGS,
                                onClick = {
                                    if (selectedTab != BottomNavTab.SETTINGS) {
                                        selectedTab = BottomNavTab.SETTINGS
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.nav_settings)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_settings)) },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    val snapshot = uiState.dashboard
                    if (uiState.isLoading || snapshot == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Crossfade(
                            targetState = selectedTab,
                            animationSpec = tween(durationMillis = 180),
                            label = "bottomNavCrossfade",
                            modifier = Modifier.padding(innerPadding)
                        ) { tab ->
                            when (tab) {
                                BottomNavTab.HOME -> HomeScreen(
                                    snapshot = snapshot,
                                    hapticEnabled = uiState.settings.hapticFeedbackEnabled,
                                    canUndoToday = uiState.canUndoTodayCompletion,
                                    notificationsEnabledInSettings = uiState.settings.reminderEnabled,
                                    hasNotificationPermission = hasNotifPermission,
                                    onMarkComplete = { actualReps ->
                                        viewModel.markTodayComplete(actualReps)
                                    },
                                    onSavePartialProgress = { partialReps ->
                                        viewModel.recordPartialProgress(partialReps)
                                    },
                                    onUndoToday = { viewModel.undoTodayCompletion() },
                                    onOpenNotificationSettings = { openAppNotificationSettings() },
                                    onOpenCoach = { selectedTab = BottomNavTab.COACH }
                                )

                                BottomNavTab.COACH -> CoachScreen(
                                    snapshot = snapshot,
                                    uiState = coachUiState,
                                    onSendPrompt = { prompt, ctx ->
                                        coachViewModel.sendPrompt(prompt, ctx)
                                    },
                                    onGeneratePacing = { target, streak, fatigue ->
                                        coachViewModel.generatePacingPlan(target, streak, fatigue)
                                    },
                                    onRetry = { coachViewModel.retryLastPrompt() },
                                    onDismissError = { coachViewModel.dismissError() },
                                    onClearChat = { coachViewModel.clearConversation() },
                                    onNavigateHome = { selectedTab = BottomNavTab.HOME }
                                )

                                BottomNavTab.HISTORY -> HistoryScreen(
                                    snapshot = snapshot,
                                    history = uiState.history,
                                    chartPoints = uiState.last30DaysChart,
                                    settings = uiState.settings,
                                    onNavigateHome = { selectedTab = BottomNavTab.HOME }
                                )

                                BottomNavTab.SETTINGS -> SettingsScreen(
                                    settings = uiState.settings,
                                    hasNotificationPermission = hasNotifPermission,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            openAppNotificationSettings()
                                        }
                                    },
                                    onOpenNotificationSettings = { openAppNotificationSettings() },
                                    onSaveChallengeConfig = { start, inc, capOn, cap, missed ->
                                        viewModel.updateChallengeSettings(start, inc, capOn, cap, missed)
                                    },
                                    onUpdatePrimaryReminder = { enabled, hour, minute ->
                                        viewModel.updatePrimaryReminder(enabled, hour, minute)
                                    },
                                    onUpdateSecondReminder = { enabled, hour, minute ->
                                        viewModel.updateSecondReminder(enabled, hour, minute)
                                    },
                                    onUpdateTheme = { mode -> viewModel.updateThemeMode(mode) },
                                    onUpdateHaptic = { enabled -> viewModel.updateHapticFeedback(enabled) },
                                    onExportCsv = {
                                        csvExportLauncher.launch("reprise_history_${LocalDate.now()}.csv")
                                    },
                                    onClearAllData = { viewModel.clearAllData() },
                                    onNavigateHome = { selectedTab = BottomNavTab.HOME }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            }
        }
        runCatching { startActivity(intent) }
    }
}
