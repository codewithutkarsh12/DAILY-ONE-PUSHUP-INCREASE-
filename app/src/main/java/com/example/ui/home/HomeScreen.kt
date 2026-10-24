package com.example.ui.home

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.AnimatedAthleticProgressRing
import com.example.ui.components.CompactDashboardCard
import com.example.ui.components.CompleteChallengeDialog
import com.example.ui.components.CompletionCelebration
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SafetyAndSelfReportNote
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanSecondary
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
    onOpenCoach: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }

    // Subtle card entrance motion
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "homeEntranceAlpha"
    )

    val dateFormatted = snapshot.todayDate.format(
        DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    )

    val actualRecordedToday = snapshot.todayActualReps ?: if (snapshot.isTodayCompleted) snapshot.todayTarget else 0
    val remainingReps = (snapshot.todayTarget - actualRecordedToday).coerceAtLeast(0)
    val rawProgress = if (snapshot.todayTarget > 0) {
        (actualRecordedToday.toFloat() / snapshot.todayTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (snapshot.isTodayCompleted) 1f else rawProgress,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "dailyChallengeProgress"
    )
    val completionPercent = (animatedProgress * 100).toInt()

    val animatedTarget by animateIntAsState(
        targetValue = snapshot.todayTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "animatedTodayTarget"
    )
    val animatedCompletedReps by animateIntAsState(
        targetValue = actualRecordedToday,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "animatedCompletedReps"
    )
    val animatedRemainingReps by animateIntAsState(
        targetValue = remainingReps,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "animatedRemainingReps"
    )

    val targetScale by animateFloatAsState(
        targetValue = if (snapshot.isTodayCompleted) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 220),
        label = "targetScaleAnimation"
    )

    val progressBarColor by animateColorAsState(
        targetValue = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 240),
        label = "progressBarColor"
    )

    val heroBorderColor by animateColorAsState(
        targetValue = if (snapshot.isTodayCompleted) {
            EmeraldSuccess.copy(alpha = 0.50f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)
        },
        animationSpec = tween(durationMillis = 240),
        label = "heroCardBorderColor"
    )

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
            // 1. Premium Athletic Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "+1 DAILY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("today_date_text")
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_header_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Compact Three-Item Dashboard Card + Milestones + Completed Days
            CompactDashboardCard(
                longestStreak = snapshot.longestStreak,
                totalPushUps = snapshot.totalLoggedPushUps,
                activeStreak = snapshot.activeStreak,
                totalCompletedDays = snapshot.totalCompletedDays,
                todayCompletionPercent = completionPercent
            )

            // 3. Main Daily Challenge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_target_card"),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, heroBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (snapshot.isTodayCompleted) {
                                        EmeraldSuccess.copy(alpha = 0.06f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    },
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Pill & Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (snapshot.isTodayCompleted) {
                                    stringResource(R.string.today_completed_header)
                                } else {
                                    stringResource(R.string.today_target_header)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (snapshot.activeStreak > 0) {
                                    "Current Streak: ${snapshot.activeStreak} ${if (snapshot.activeStreak == 1) "day" else "days"}"
                                } else {
                                    "Day 1 Progression Session"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedContent(
                            targetState = Triple(snapshot.isTodayCompleted, actualRecordedToday > 0, snapshot.activeStreak),
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                            },
                            label = "statusPillTransition"
                        ) { (completed, hasPartial, _) ->
                            val statusText = when {
                                completed -> "Completed"
                                hasPartial -> "In Progress"
                                else -> "Pending"
                            }
                            val statusColor = when {
                                completed -> EmeraldSuccess
                                hasPartial -> CyanSecondary
                                else -> AmberWarning
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }

                    // Circular Athletic Progress Ring + Large Target Display
                    AnimatedAthleticProgressRing(
                        progress = animatedProgress,
                        isCompleted = snapshot.isTodayCompleted,
                        ringSize = 166.dp,
                        strokeWidth = 10.dp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = targetScale
                            scaleY = targetScale
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
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
                    }

                    // 3-item Daily Breakdown Row: Completed | Remaining | Completion %
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DailySubMetricItem(
                                label = "COMPLETED",
                                value = "$animatedCompletedReps reps",
                                valueColor = if (actualRecordedToday > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            DailySubMetricItem(
                                label = "REMAINING",
                                value = "$animatedRemainingReps reps",
                                valueColor = if (remainingReps == 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            DailySubMetricItem(
                                label = "PROGRESS",
                                value = "$completionPercent%",
                                valueColor = if (snapshot.isTodayCompleted) EmeraldSuccess else CyanSecondary
                            )
                        }
                    }

                    // Linear Progress Bar Section showing actual reps against target
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
                                text = "$completionPercent%",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (snapshot.isTodayCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressBarColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Deadline Reminder (when incomplete) or CompletionCelebration Banner
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
                                        style = MaterialTheme.typography.bodySmall,
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
                                        tint = CyanSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Next: ${snapshot.nextDayTarget}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
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

                            CompletionCelebration(
                                completedTimeFormatted = completedTimeFormatted,
                                nextDayTarget = snapshot.nextDayTarget
                            )
                        }
                    }

                    // Primary CTA Button to Log or Complete Today's Push-ups
                    PrimaryActionButton(
                        text = if (snapshot.isTodayCompleted) {
                            stringResource(R.string.completed_cta)
                        } else {
                            stringResource(R.string.mark_done_cta)
                        },
                        icon = if (snapshot.isTodayCompleted) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                        onClick = { showCompleteDialog = true },
                        enabled = !snapshot.isTodayCompleted,
                        modifier = Modifier.testTag("mark_done_button")
                    )

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

                    // AI Coach Pacing & Form Button
                    OutlinedButton(
                        onClick = onOpenCoach,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_ai_coach_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach",
                            tint = CyanSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask Gemini Coach for Pacing & Form",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Subtle Athletic Motivation Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = snapshot.motivationalMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("motivational_message_text")
                        )
                    }
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

@Composable
private fun DailySubMetricItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
