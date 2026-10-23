package com.example.ui.home

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ChallengeManager
import com.example.domain.DailyChallengeSnapshot
import com.example.ui.components.CompactDashboardCard
import com.example.ui.components.CompleteChallengeDialog
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.SafetyAndSelfReportNote
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("NewApi")
@Composable
fun HomeScreen(
    snapshot: DailyChallengeSnapshot,
    hapticEnabled: Boolean,
    canUndoToday: Boolean,
    notificationsEnabledInSettings: Boolean,
    hasNotificationPermission: Boolean,
    onMarkComplete: (actualReps: Int?) -> Unit,
    onSavePartialProgress: (partialReps: Int) -> Unit,
    onUndoToday: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }

    val dateFormatted = snapshot.todayDate.format(
        DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    )

    val actualRecordedToday = snapshot.todayActualReps ?: if (snapshot.isTodayCompleted) snapshot.todayTarget else 0
    val rawProgress = if (snapshot.todayTarget > 0) {
        (actualRecordedToday.toFloat() / snapshot.todayTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (snapshot.isTodayCompleted) 1f else rawProgress,
        animationSpec = tween(durationMillis = 280),
        label = "dailyChallengeProgress"
    )

    val animatedTarget by animateIntAsState(
        targetValue = snapshot.todayTarget,
        animationSpec = tween(durationMillis = 220),
        label = "animatedTodayTarget"
    )

    val targetScale by animateFloatAsState(
        targetValue = if (snapshot.isTodayCompleted) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 240),
        label = "targetScaleAnimation"
    )

    val progressBarColor by animateColorAsState(
        targetValue = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 240),
        label = "progressBarColor"
    )

    val heroBorderColor by animateColorAsState(
        targetValue = if (snapshot.isTodayCompleted) {
            EmeraldSuccess.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 240),
        label = "heroCardBorderColor"
    )

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
            // App Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("today_date_text")
                )
            }

            // 1. Compact Three-Item Dashboard Card + Milestones + Completed Days
            CompactDashboardCard(
                longestStreak = snapshot.longestStreak,
                totalPushUps = snapshot.totalLoggedPushUps,
                activeStreak = snapshot.activeStreak,
                totalCompletedDays = snapshot.totalCompletedDays
            )

            // 2. Today's Challenge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_target_card"),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, heroBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Pill & Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (snapshot.isTodayCompleted) {
                                stringResource(R.string.today_completed_header)
                            } else {
                                stringResource(R.string.today_target_header)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )

                        AnimatedContent(
                            targetState = snapshot.isTodayCompleted,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                            },
                            label = "statusPillTransition"
                        ) { completed ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (completed) {
                                    EmeraldSuccess.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (completed) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = if (completed) "Completed" else "Pending",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (completed) EmeraldSuccess else AmberWarning
                                    )
                                }
                            }
                        }
                    }

                    // Large Readable Target Display with subtle completion scale animation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .graphicsLayer {
                                scaleX = targetScale
                                scaleY = targetScale
                            }
                    ) {
                        Text(
                            text = "$animatedTarget",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("today_target_text")
                        )
                        Text(
                            text = stringResource(R.string.push_ups_unit),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Simple Progress Indicator showing actual reps against target
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_progress_section"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress: $actualRecordedToday / ${snapshot.todayTarget} reps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = progressBarColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Deadline Reminder (when incomplete) or Completed Timestamp & Next Target
                    AnimatedContent(
                        targetState = snapshot.isTodayCompleted,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "completionBannerTransition"
                    ) { isCompleted ->
                        if (!isCompleted) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deadline_countdown_chip"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Time remaining today",
                                        tint = AmberWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Deadline in ${ChallengeManager.formatCountdown(snapshot.remainingMillisToday)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Next: ${snapshot.nextDayTarget}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("next_day_target_text")
                                    )
                                }
                            }
                        } else {
                            val completedTimeFormatted = snapshot.completedAtMillis?.let { millis ->
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalTime()
                                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                            } ?: "Today"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldSuccess.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("completed_confirmation_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Done at $completedTimeFormatted",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Text(
                                        text = "Tomorrow's target: ${snapshot.nextDayTarget}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("next_day_target_text")
                                    )
                                }
                            }
                        }
                    }

                    // Primary CTA Button to Log or Complete Today's Push-ups
                    Button(
                        onClick = { showCompleteDialog = true },
                        enabled = !snapshot.isTodayCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("mark_done_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = EmeraldSuccess.copy(alpha = 0.20f),
                            disabledContentColor = EmeraldSuccess
                        )
                    ) {
                        Icon(
                            imageVector = if (snapshot.isTodayCompleted) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (snapshot.isTodayCompleted) {
                                stringResource(R.string.completed_cta)
                            } else {
                                stringResource(R.string.mark_done_cta)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Smoothly animated Undo action immediately after completion
                    AnimatedVisibility(
                        visible = snapshot.isTodayCompleted && canUndoToday,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                onUndoToday()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("undo_completion_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo completion",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.undo_completion_cta))
                        }
                    }

                    Text(
                        text = snapshot.motivationalMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("motivational_message_text")
                    )
                }
            }

            // Notification permission banner if reminders are enabled in settings but blocked at OS level
            if (notificationsEnabledInSettings && !hasNotificationPermission) {
                NotificationPermissionBanner(onOpenSettings = onOpenNotificationSettings)
            }

            SafetyAndSelfReportNote()
        }
    }

    if (showCompleteDialog) {
        CompleteChallengeDialog(
            targetReps = snapshot.todayTarget,
            currentPartialReps = snapshot.todayActualReps,
            onDismiss = { showCompleteDialog = false },
            onConfirmComplete = { actualReps ->
                showCompleteDialog = false
                if (hapticEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
                onMarkComplete(actualReps)
            },
            onSavePartialProgress = { partialReps ->
                showCompleteDialog = false
                if (hapticEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                onSavePartialProgress(partialReps)
            }
        )
    }
}
