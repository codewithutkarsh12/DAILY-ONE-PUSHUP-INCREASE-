package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.StreakCalculator
import com.example.domain.StreakMilestone
import com.example.ui.theme.AthleticLime
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmberAccent
import com.example.ui.theme.EmeraldSuccess

/**
 * Compact Athletic Performance Dashboard card displayed on the Home screen:
 * Top 3-column performance metrics:
 * 1. Longest Streak (Personal Best)
 * 2. Total Push-ups Logged
 * 3. Current Active Streak
 *
 * Plus Today's Progress metric pill and the interactive Milestones badge bar (7d, 30d, 100d).
 */
@Composable
fun CompactDashboardCard(
    longestStreak: Int,
    totalPushUps: Int,
    activeStreak: Int,
    totalCompletedDays: Int = 0,
    todayCompletionPercent: Int? = null,
    modifier: Modifier = Modifier
) {
    val milestones = remember(longestStreak) {
        StreakCalculator.evaluateMilestones(longestStreak)
    }
    val unlockedCount = milestones.count { it.isUnlocked }

    val animatedLongest by animateIntAsState(
        targetValue = longestStreak,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "animatedLongestStreak"
    )
    val animatedTotal by animateIntAsState(
        targetValue = totalPushUps,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "animatedTotalPushUps"
    )
    val animatedActive by animateIntAsState(
        targetValue = activeStreak,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "animatedActiveStreak"
    )
    val animatedCompletedDays by animateIntAsState(
        targetValue = totalCompletedDays,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "animatedCompletedDays"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_compact_dashboard_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section Header inside Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "PERFORMANCE METRICS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (todayCompletionPercent != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (todayCompletionPercent >= 100) {
                            EmeraldSuccess.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = "TODAY: $todayCompletionPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (todayCompletionPercent >= 100) {
                                EmeraldSuccess
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Three-column primary stats grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardStatColumn(
                    label = "Longest Streak",
                    value = "$animatedLongest",
                    unit = if (longestStreak == 1) "day" else "days",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = CyanSecondary,
                    testTagName = "dashboard_longest_streak",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )

                DashboardStatColumn(
                    label = "Total Logged",
                    value = "$animatedTotal",
                    unit = "push-ups",
                    icon = Icons.Default.FitnessCenter,
                    iconTint = MaterialTheme.colorScheme.primary,
                    testTagName = "dashboard_total_pushups",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )

                DashboardStatColumn(
                    label = "Active Streak",
                    value = "$animatedActive",
                    unit = if (activeStreak == 1) "day" else "days",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = if (activeStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTagName = "dashboard_active_streak",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

            // Milestones Section + Completed Days Counter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_milestones_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STREAK MILESTONES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$unlockedCount/${milestones.size} unlocked • $animatedCompletedDays ${if (totalCompletedDays == 1) "day" else "days"} done",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (unlockedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("dashboard_completed_days")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    milestones.forEach { milestone ->
                        MilestoneBadgeItem(
                            milestone = milestone,
                            longestStreak = longestStreak,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneBadgeItem(
    milestone: StreakMilestone,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by rememberSaveable { mutableStateOf(false) }

    val icon = when (milestone.thresholdDays) {
        7 -> Icons.Default.LocalFireDepartment
        30 -> Icons.Default.WorkspacePremium
        else -> Icons.Default.MilitaryTech
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (milestone.isUnlocked) 1.0f else 0.98f,
        animationSpec = tween(durationMillis = 200),
        label = "milestoneBadgeScale"
    )

    val containerBg by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneBadgeBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneBadgeBorder"
    )

    val iconTint by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneIconTint"
    )

    val textColor by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneTextColor"
    )

    val statusDesc = if (milestone.isUnlocked) "Unlocked" else "Locked (${milestone.progressText})"

    Row(
        modifier = modifier
            .testTag("milestone_badge_${milestone.thresholdDays}")
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = badgeScale
                scaleY = badgeScale
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${milestone.title} (${milestone.badgeLabel}): $statusDesc"
            }
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable { showDetailDialog = true }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (milestone.isUnlocked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (milestone.isUnlocked) icon else Icons.Default.Lock,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = milestone.badgeLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = if (milestone.isUnlocked) "Unlocked" else milestone.progressText,
                style = MaterialTheme.typography.labelSmall,
                color = if (milestone.isUnlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (milestone.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Achieve a consecutive push-up streak of at least ${milestone.thresholdDays} days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (milestone.isUnlocked) "Achievement Unlocked!" else "Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (milestone.isUnlocked) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${longestStreak.coerceAtMost(milestone.thresholdDays)} / ${milestone.thresholdDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { milestone.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (milestone.isUnlocked) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (milestone.isUnlocked) {
                        Text(
                            text = "Permanent achievement earned with your personal best streak of $longestStreak days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    } else {
                        val needed = milestone.thresholdDays - longestStreak
                        Text(
                            text = "$needed more consecutive completed ${if (needed == 1) "day" else "days"} needed to unlock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailDialog = false },
                    modifier = Modifier.testTag("dismiss_milestone_dialog_button")
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun DashboardStatColumn(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag(testTagName)
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value $unit"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Reusable athletic metric card for compact KPI displays.
 */
@Composable
fun PremiumMetricCard(
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Circular + Linear athletic progress ring for the main daily challenge card.
 */
@Composable
fun AnimatedAthleticProgressRing(
    progress: Float,
    isCompleted: Boolean,
    ringSize: Dp = 168.dp,
    strokeWidth: Dp = 10.dp,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "athleticRingProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = CyanSecondary

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            val arcDiameter = size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)

            // Background track ring
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress arc
            if (animatedProgress > 0.005f) {
                val brush = if (isCompleted) {
                    Brush.sweepGradient(
                        listOf(EmeraldSuccess, AthleticLime, EmeraldSuccess)
                    )
                } else {
                    Brush.sweepGradient(
                        listOf(primaryColor, secondaryColor, primaryColor)
                    )
                }
                drawArc(
                    brush = brush,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        centerContent()
    }
}

/**
 * Primary athletic action button with tactile press-scale microinteraction.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.975f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "primaryButtonPressScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = EmeraldSuccess.copy(alpha = 0.18f),
            disabledContentColor = EmeraldSuccess
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Restrained completion celebration banner displayed when today's target is met.
 */
@Composable
fun CompletionCelebration(
    completedTimeFormatted: String,
    nextDayTarget: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = EmeraldSuccess.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.40f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("completed_confirmation_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )
            }
            Text(
                text = "Tomorrow's target: $nextDayTarget",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("next_day_target_text")
            )
        }
    }
}

/**
 * Reusable athletic section header.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable polished EmptyState card.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SafetyAndSelfReportNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_notice_card")
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.HealthAndSafety,
            contentDescription = "Safety Notice",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.safety_self_report_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotificationPermissionBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_permission_banner"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = "Notifications disabled",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.notifications_disabled_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.notifications_disabled_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_notification_settings_button")
            ) {
                Text(stringResource(R.string.open_notification_settings))
            }
        }
    }
}
