package com.example.ui.settings

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.SectionHeader
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

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "settingsEntranceAlpha"
    )

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
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entranceAlpha },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Progression parameters, reminders, and local storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Challenge Progression Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = stringResource(R.string.settings_progression_header),
                        subtitle = "Configure starting repetitions and daily progressive overload",
                        icon = Icons.Default.Tune
                    )

                    Text(
                        text = "Your streak begins at the Initial Target (default 10) and adds the Daily Increment (default +1) after each completed day. Completed days in your history always preserve their original target.",
                        style = MaterialTheme.typography.bodySmall,
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
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
                                style = MaterialTheme.typography.bodySmall,
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
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_cap_input")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_progression_button")
                    ) {
                        Text(
                            text = "Save Progression Rules",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Reminders & Notifications Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = stringResource(R.string.settings_reminders_header),
                        subtitle = "Scheduled local alerts when today's session is incomplete",
                        icon = Icons.Default.Notifications
                    )

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
                                style = MaterialTheme.typography.bodySmall,
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
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("primary_reminder_time_button")
                                ) {
                                    Text(
                                        text = primaryFormatted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
                                        style = MaterialTheme.typography.bodySmall,
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
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("second_reminder_time_button")
                                    ) {
                                        Text(
                                            text = secondFormatted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Note: Reminders are scheduled locally on your device. Android battery optimizations may shift delivery by a few minutes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Appearance & Haptics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = stringResource(R.string.settings_appearance_header),
                        subtitle = "Theme mode and tactile completion feedback",
                        icon = Icons.Default.Palette
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
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
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
                                style = MaterialTheme.typography.bodySmall,
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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        title = stringResource(R.string.settings_data_header),
                        subtitle = "Offline Room database export and reset controls",
                        icon = Icons.Default.Storage
                    )

                    Text(
                        text = "All workout history is stored 100% offline on this device. No account or cloud server is required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.export_csv_cta),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { showClearConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.clear_data_cta),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. About RepRise Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_about_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "About RepRise",
                        subtitle = "Daily +1 Progressive Push-Up System",
                        icon = Icons.Default.Info
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Application Version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "v1.0.0",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Built for daily consistency and sustainable upper-body endurance through incremental progressive overload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
