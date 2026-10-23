package com.example.ui.settings

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.MissedStreakBehavior
import com.example.data.preferences.UserSettings
import com.example.ui.components.NotificationPermissionBanner
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("NewApi")
@Composable
fun SettingsScreen(
    settings: UserSettings,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSaveChallengeConfig: (
        startTarget: Int,
        dailyIncrement: Int,
        targetCapEnabled: Boolean,
        targetCap: Int,
        missedBehavior: MissedStreakBehavior
    ) -> Unit,
    onUpdatePrimaryReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onUpdateSecondReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateHaptic: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onClearAllData: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateHome()
    }

    val context = LocalContext.current
    val view = LocalView.current
    val hapticEnabled = settings.hapticFeedbackEnabled

    var startTargetText by rememberSaveable { mutableStateOf(settings.startTarget.toString()) }
    var incrementText by rememberSaveable { mutableStateOf(settings.dailyIncrement.toString()) }
    var capEnabled by rememberSaveable { mutableStateOf(settings.targetCapEnabled) }
    var capText by rememberSaveable { mutableStateOf(settings.targetCap.toString()) }
    var missedBehavior by rememberSaveable { mutableStateOf(settings.missedStreakBehavior) }
    var configMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings) {
        startTargetText = settings.startTarget.toString()
        incrementText = settings.dailyIncrement.toString()
        capEnabled = settings.targetCapEnabled
        capText = settings.targetCap.toString()
        missedBehavior = settings.missedStreakBehavior
    }

    fun tryAutoSaveIfValid(
        newCapEnabled: Boolean = capEnabled,
        newMissedBehavior: MissedStreakBehavior = missedBehavior
    ) {
        val parsedStart = startTargetText.toIntOrNull()
        val parsedInc = incrementText.toIntOrNull()
        val parsedCap = capText.toIntOrNull() ?: 100
        if (
            parsedStart != null && parsedStart in 1..1000 &&
            parsedInc != null && parsedInc in 1..100 &&
            (!newCapEnabled || parsedCap >= parsedStart)
        ) {
            onSaveChallengeConfig(
                parsedStart,
                parsedInc,
                newCapEnabled,
                parsedCap,
                newMissedBehavior
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Challenge Progression Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_progression_header),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Text(
                        text = "How progression works: Your streak starts at the Initial Target (default 10) and adds the Daily Increment (default +1) after each completed day. Completed days in your history always keep their original target.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTargetText,
                            onValueChange = {
                                startTargetText = it.filter { ch -> ch.isDigit() }
                                configMessage = null
                            },
                            label = { Text("Start Target") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_target_input")
                        )

                        OutlinedTextField(
                            value = incrementText,
                            onValueChange = {
                                incrementText = it.filter { ch -> ch.isDigit() }
                                configMessage = null
                            },
                            label = { Text("Daily Increment") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("daily_increment_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Maximum Target Cap",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Limits how high the daily target can grow.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = capEnabled,
                            onCheckedChange = { enabled ->
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                capEnabled = enabled
                                configMessage = null
                                tryAutoSaveIfValid(newCapEnabled = enabled)
                            },
                            modifier = Modifier.testTag("target_cap_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = capEnabled,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        OutlinedTextField(
                            value = capText,
                            onValueChange = {
                                capText = it.filter { ch -> ch.isDigit() }
                                configMessage = null
                            },
                            label = { Text("Maximum Daily Target Cap") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_cap_input")
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Behavior After a Broken Streak",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                missedBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
                                configMessage = null
                                tryAutoSaveIfValid(
                                    newMissedBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
                                )
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = missedBehavior == MissedStreakBehavior.RESTART_AT_START_TARGET,
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                missedBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
                                configMessage = null
                                tryAutoSaveIfValid(
                                    newMissedBehavior = MissedStreakBehavior.RESTART_AT_START_TARGET
                                )
                            },
                            modifier = Modifier.testTag("radio_restart_at_start")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restart at Initial Target (Default: ${startTargetText.ifBlank { "10" }} push-ups)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                missedBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
                                configMessage = null
                                tryAutoSaveIfValid(
                                    newMissedBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
                                )
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = missedBehavior == MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET,
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                missedBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
                                configMessage = null
                                tryAutoSaveIfValid(
                                    newMissedBehavior = MissedStreakBehavior.RESUME_FROM_PREVIOUS_TARGET
                                )
                            },
                            modifier = Modifier.testTag("radio_resume_previous")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resume from previous completed target",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    AnimatedVisibility(
                        visible = configMessage != null,
                        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
                    ) {
                        Text(
                            text = configMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("progression_config_message")
                        )
                    }

                    Button(
                        onClick = {
                            val parsedStart = startTargetText.toIntOrNull()
                            val parsedInc = incrementText.toIntOrNull()
                            val parsedCap = capText.toIntOrNull() ?: 100

                            when {
                                parsedStart == null || parsedStart !in 1..1000 -> {
                                    configMessage = "Start Target must be between 1 and 1,000."
                                }
                                parsedInc == null || parsedInc !in 1..100 -> {
                                    configMessage = "Daily Increment must be between 1 and 100."
                                }
                                capEnabled && parsedCap < parsedStart -> {
                                    configMessage = "Target Cap must be greater than or equal to Start Target ($parsedStart)."
                                }
                                else -> {
                                    if (hapticEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    }
                                    onSaveChallengeConfig(
                                        parsedStart,
                                        parsedInc,
                                        capEnabled,
                                        parsedCap,
                                        missedBehavior
                                    )
                                    configMessage = "Progression settings saved."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_progression_button")
                    ) {
                        Text("Save Progression Rules")
                    }
                }
            }

            // 2. Reminders & Notifications Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_reminders_header),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (!hasNotificationPermission) {
                        NotificationPermissionBanner(onOpenSettings = onOpenNotificationSettings)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reminder",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Notifies you only if today's push-ups are not yet completed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                if (enabled && !hasNotificationPermission) {
                                    onRequestNotificationPermission()
                                }
                                onUpdatePrimaryReminder(
                                    enabled,
                                    settings.reminderHour,
                                    settings.reminderMinute
                                )
                            },
                            modifier = Modifier.testTag("primary_reminder_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.reminderEnabled,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val primaryFormatted = LocalTime.of(settings.reminderHour, settings.reminderMinute)
                                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Primary Reminder Time",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, selectedHour, selectedMinute ->
                                                onUpdatePrimaryReminder(true, selectedHour, selectedMinute)
                                            },
                                            settings.reminderHour,
                                            settings.reminderMinute,
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("primary_reminder_time_button")
                                ) {
                                    Text(primaryFormatted)
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Second Reminder Before Deadline",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Optional late-evening follow-up if still incomplete.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.secondReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        onUpdateSecondReminder(
                                            enabled,
                                            settings.secondReminderHour,
                                            settings.secondReminderMinute
                                        )
                                    },
                                    modifier = Modifier.testTag("second_reminder_switch")
                                )
                            }

                            AnimatedVisibility(
                                visible = settings.secondReminderEnabled,
                                enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                            ) {
                                val secondFormatted = LocalTime.of(
                                    settings.secondReminderHour,
                                    settings.secondReminderMinute
                                ).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Second Reminder Time",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, selectedHour, selectedMinute ->
                                                    onUpdateSecondReminder(true, selectedHour, selectedMinute)
                                                },
                                                settings.secondReminderHour,
                                                settings.secondReminderMinute,
                                                false
                                            ).show()
                                        },
                                        modifier = Modifier.testTag("second_reminder_time_button")
                                    ) {
                                        Text(secondFormatted)
                                    }
                                }
                            }

                            Text(
                                text = "Note: Reminders are scheduled locally on your device. Android battery optimizations may shift delivery by a few minutes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Appearance & Haptics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_appearance_header),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    if (hapticEnabled && settings.themeMode != mode) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    onUpdateTheme(mode)
                                },
                                label = {
                                    Text(
                                        when (mode) {
                                            AppThemeMode.DARK -> "Dark"
                                            AppThemeMode.LIGHT -> "Light"
                                            AppThemeMode.SYSTEM -> "System"
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Haptic Feedback on Completion",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Subtle tactile confirmation when logging daily reps.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                onUpdateHaptic(enabled)
                            },
                            modifier = Modifier.testTag("haptic_feedback_switch")
                        )
                    }
                }
            }

            // 4. Data & Privacy (CSV Export & Clear Data)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_data_header),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "All workout history is stored 100% offline on this device. No account or cloud server is used.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_csv_cta))
                    }

                    Button(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.clear_data_cta))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_dialog_title)) },
            text = {
                Text(stringResource(R.string.clear_dialog_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text(stringResource(R.string.clear_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_clear_history_button")
                ) {
                    Text(stringResource(R.string.confirm_dialog_cancel))
                }
            }
        )
    }
}
