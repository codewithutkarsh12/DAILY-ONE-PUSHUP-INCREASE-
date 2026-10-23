package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.MissedStreakBehavior
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsStore
import com.example.data.repository.PushUpRepository
import com.example.domain.ChallengeManager
import com.example.domain.ChartDayPoint
import com.example.domain.DailyChallengeSnapshot
import com.example.notifications.ReminderScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RepRiseUiState(
    val isLoading: Boolean = true,
    val dashboard: DailyChallengeSnapshot? = null,
    val history: List<PushUpDayEntity> = emptyList(),
    val last30DaysChart: List<ChartDayPoint> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val canUndoTodayCompletion: Boolean = false,
    val errorMessage: String? = null
)

private data class ReminderConfigKey(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val secondEnabled: Boolean,
    val secondHour: Int,
    val secondMinute: Int
)

@SuppressLint("NewApi")
class RepRiseViewModel(
    private val appContext: Context,
    private val repository: PushUpRepository,
    private val settingsStore: UserSettingsStore
) : ViewModel() {

    private val currentDateTimeFlow = MutableStateFlow(LocalDateTime.now())
    private val canUndoTodayFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val isSubmittingCompletion = AtomicBoolean(false)
    private var undoExpiryJob: Job? = null

    val uiState: StateFlow<RepRiseUiState> = combine(
        repository.allHistoryFlow,
        repository.settingsFlow,
        currentDateTimeFlow,
        canUndoTodayFlow,
        errorFlow
    ) { history, settings, now, canUndo, errorMsg ->
        val today = now.toLocalDate()
        val isTodayDone = history.any { it.localDate == today.toString() && it.isCompleted }
        val effectiveUndo = canUndo && isTodayDone

        val snapshot = ChallengeManager.evaluateDashboard(
            now = now,
            history = history,
            settings = settings
        )

        val chartPoints = ChallengeManager.buildLast30DaysChartData(
            today = today,
            history = history,
            settings = settings
        )

        RepRiseUiState(
            isLoading = false,
            dashboard = snapshot,
            history = history,
            last30DaysChart = chartPoints,
            settings = settings,
            canUndoTodayCompletion = effectiveUndo,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RepRiseUiState(isLoading = true)
    )

    init {
        // 1-second ticker to keep the countdown and local-date rollover accurate across midnight
        viewModelScope.launch {
            while (isActive) {
                currentDateTimeFlow.value = LocalDateTime.now()
                delay(1_000L)
            }
        }

        // Only reschedule AlarmManager when reminder-related settings actually change
        viewModelScope.launch {
            repository.settingsFlow
                .map { s ->
                    ReminderConfigKey(
                        enabled = s.reminderEnabled,
                        hour = s.reminderHour,
                        minute = s.reminderMinute,
                        secondEnabled = s.secondReminderEnabled,
                        secondHour = s.secondReminderHour,
                        secondMinute = s.secondReminderMinute
                    ) to s
                }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collect { (_, settings) ->
                    ReminderScheduler.syncReminders(appContext, settings)
                }
        }
    }

    fun refreshNow() {
        currentDateTimeFlow.value = LocalDateTime.now()
    }

    /**
     * Completes today's challenge after validating that today is still open and actualReps >= target.
     * Protected against rapid double-tap submissions.
     */
    fun markTodayComplete(
        actualRepsInput: Int?,
        onCompleted: (Boolean) -> Unit = {}
    ) {
        if (!isSubmittingCompletion.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                currentDateTimeFlow.value = now
                val today = now.toLocalDate()
                val currentState = uiState.value
                val target = currentState.dashboard?.todayTarget ?: currentState.settings.startTarget

                val wasNewlyCompleted = repository.completeChallengeForDate(
                    date = today,
                    targetReps = target,
                    actualReps = actualRepsInput,
                    completedAtMillis = System.currentTimeMillis()
                )

                if (wasNewlyCompleted) {
                    ReminderScheduler.cancelTodayNotifications(appContext)
                    undoExpiryJob?.cancel()
                    canUndoTodayFlow.value = true
                    undoExpiryJob = viewModelScope.launch {
                        delay(15_000L)
                        canUndoTodayFlow.value = false
                    }
                }
                onCompleted(wasNewlyCompleted)
            } catch (e: Exception) {
                errorFlow.value = "Could not save completion: ${e.localizedMessage ?: "Database error"}"
                onCompleted(false)
            } finally {
                isSubmittingCompletion.set(false)
            }
        }
    }

    /**
     * Saves partial reps toward today's challenge without marking the day complete.
     */
    fun recordPartialProgress(actualReps: Int) {
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                currentDateTimeFlow.value = now
                val today = now.toLocalDate()
                val currentState = uiState.value
                val target = currentState.dashboard?.todayTarget ?: currentState.settings.startTarget
                repository.recordPartialRepsForDate(
                    date = today,
                    targetReps = target,
                    actualReps = actualReps
                )
            } catch (e: Exception) {
                errorFlow.value = "Could not save progress: ${e.localizedMessage ?: "Database error"}"
            }
        }
    }

    fun undoTodayCompletion() {
        viewModelScope.launch {
            try {
                undoExpiryJob?.cancel()
                val today = LocalDate.now()
                repository.undoCompletionForDate(today)
                canUndoTodayFlow.value = false
                ReminderScheduler.syncReminders(appContext, uiState.value.settings)
            } catch (e: Exception) {
                errorFlow.value = "Could not undo completion: ${e.localizedMessage}"
            }
        }
    }

    fun updateChallengeSettings(
        startTarget: Int,
        dailyIncrement: Int,
        targetCapEnabled: Boolean,
        targetCap: Int,
        missedBehavior: MissedStreakBehavior
    ) {
        viewModelScope.launch {
            try {
                settingsStore.updateChallengeConfig(
                    startTarget = startTarget,
                    dailyIncrement = dailyIncrement,
                    targetCapEnabled = targetCapEnabled,
                    targetCap = targetCap,
                    missedStreakBehavior = missedBehavior
                )
            } catch (e: Exception) {
                errorFlow.value = "Failed to save challenge settings."
            }
        }
    }

    fun updatePrimaryReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsStore.updatePrimaryReminder(enabled, hour, minute)
        }
    }

    fun updateSecondReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsStore.updateSecondReminder(enabled, hour, minute)
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsStore.updateThemeMode(mode)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateHapticFeedback(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                undoExpiryJob?.cancel()
                repository.clearAllHistory()
                canUndoTodayFlow.value = false
                ReminderScheduler.syncReminders(appContext, uiState.value.settings)
            } catch (e: Exception) {
                errorFlow.value = "Could not clear history."
            }
        }
    }

    fun exportCsvContent(): String {
        return repository.buildCsvExport(uiState.value.history)
    }

    fun dismissError() {
        errorFlow.value = null
    }

    class Factory(
        private val appContext: Context,
        private val repository: PushUpRepository,
        private val settingsStore: UserSettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RepRiseViewModel(appContext, repository, settingsStore) as T
        }
    }
}
