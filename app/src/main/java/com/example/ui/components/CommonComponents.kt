package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.StreakCalculator
import com.example.domain.StreakMilestone
import com.example.ui.theme.EmberAccent

/**
 * Compact Dashboard card displayed at the top of the Home screen:
 * Top row:
 * 1. Longest Streak
 * 2. Total Push-ups Logged
 * 3. Current Active Streak
 *
 * Bottom section:
 * Stylized 'Milestones' icon badges unlocked based on [longestStreak] (7d, 14d, 30d, 100d)
 * and total completed days summary.
 */
@Composable
fun CompactDashboardCard(
    longestStreak: Int,
    totalPushUps: Int,
    activeStreak: Int,
    totalCompletedDays: Int = 0,
    modifier: Modifier = Modifier
) {
    val milestones = remember(longestStreak) {
        StreakCalculator.evaluateMilestones(longestStreak)
    }
    val unlockedCount = milestones.count { it.isUnlocked }

    val animatedLongest by animateIntAsState(
        targetValue = longestStreak,
        animationSpec = tween(durationMillis = 240),
        label = "animatedLongestStreak"
    )
    val animatedTotal by animateIntAsState(
        targetValue = totalPushUps,
        animationSpec = tween(durationMillis = 240),
        label = "animatedTotalPushUps"
    )
    val animatedActive by animateIntAsState(
        targetValue = activeStreak,
        animationSpec = tween(durationMillis = 240),
        label = "animatedActiveStreak"
    )
    val animatedCompletedDays by animateIntAsState(
        targetValue = totalCompletedDays,
        animationSpec = tween(durationMillis = 240),
        label = "animatedCompletedDays"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_compact_dashboard_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    testTagName = "dashboard_longest_streak",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
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
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                DashboardStatColumn(
                    label = "Active Streak",
                    value = "$animatedActive",
                    unit = if (activeStreak == 1) "day" else "days",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = if (activeStreak > 0) EmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTagName = "dashboard_active_streak",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Milestones Section + Completed Days Counter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_milestones_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Milestones",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$unlockedCount/${milestones.size} unlocked • $animatedCompletedDays ${if (totalCompletedDays == 1) "day" else "days"} done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unlockedCount > 0) EmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier
) {
    val icon = when (milestone.thresholdDays) {
        7 -> Icons.Default.LocalFireDepartment
        14 -> Icons.Default.Bolt
        30 -> Icons.Default.WorkspacePremium
        else -> Icons.Default.MilitaryTech
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (milestone.isUnlocked) 1.0f else 0.97f,
        animationSpec = tween(durationMillis = 220),
        label = "milestoneBadgeScale"
    )

    val containerBg by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            EmberAccent.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneBadgeBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            EmberAccent.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneBadgeBorder"
    )

    val iconTint by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            EmberAccent
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneIconTint"
    )

    val textColor by animateColorAsState(
        targetValue = if (milestone.isUnlocked) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "milestoneTextColor"
    )

    val statusDesc = if (milestone.isUnlocked) "Unlocked" else "Locked"

    Row(
        modifier = modifier
            .testTag("milestone_badge_${milestone.thresholdDays}")
            .graphicsLayer {
                scaleX = badgeScale
                scaleY = badgeScale
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${milestone.title} (${milestone.badgeLabel}): $statusDesc"
            }
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (milestone.isUnlocked) {
                        EmberAccent.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = milestone.badgeLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (milestone.isUnlocked) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SafetyAndSelfReportNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_notice_card")
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
            style = MaterialTheme.typography.bodyMedium,
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
