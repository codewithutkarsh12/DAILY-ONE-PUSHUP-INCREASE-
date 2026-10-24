package com.example.ui.history

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PushUpDayEntity
import com.example.data.preferences.UserSettings
import com.example.domain.ChallengeManager
import com.example.domain.ChartDayPoint
import com.example.domain.DailyChallengeSnapshot
import com.example.domain.DayChallengeStatus
import com.example.domain.TargetCalculator
import com.example.ui.components.EmptyState
import com.example.ui.components.PremiumMetricCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonMissed
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("NewApi")
@Composable
fun HistoryScreen(
    snapshot: DailyChallengeSnapshot,
    history: List<PushUpDayEntity>,
    chartPoints: List<ChartDayPoint>,
    settings: UserSettings,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateHome()
    }

    val view = LocalView.current
    val hapticEnabled = settings.hapticFeedbackEnabled
    val today = snapshot.todayDate

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "historyEntranceAlpha"
    )

    var displayedYearMonthStr by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedDateStr by rememberSaveable { mutableStateOf(today.toString()) }

    val displayedMonth = YearMonth.parse(displayedYearMonthStr)
    val selectedDate = LocalDate.parse(selectedDateStr)

    val historyByDate = remember(history) {
        history.associateBy { LocalDate.parse(it.localDate) }
    }
    val firstRecordedDate = remember(history) {
        history.filter { it.isCompleted || (it.actualReps ?: 0) > 0 }
            .minOfOrNull { LocalDate.parse(it.localDate) }
    }

    val selectedDayTarget = remember(selectedDate, history, settings) {
        val existing = historyByDate[selectedDate]
        if (existing != null && existing.isCompleted) {
            existing.targetReps
        } else {
            TargetCalculator.calculateTargetForDate(
                targetDate = selectedDate,
                history = history.filter { !LocalDate.parse(it.localDate).isAfter(selectedDate) },
                settings = settings
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
            // 1. Header Bar
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${snapshot.totalCompletedDays} ${if (snapshot.totalCompletedDays == 1) "day" else "days"} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("history_completed_days_summary")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${snapshot.totalLoggedPushUps} TOTAL REPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Compact Athletic KPI Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumMetricCard(
                    label = "ACTIVE STREAK",
                    value = "${snapshot.activeStreak}",
                    subtitle = if (snapshot.activeStreak == 1) "day" else "days",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                PremiumMetricCard(
                    label = "PERSONAL BEST",
                    value = "${snapshot.longestStreak}",
                    subtitle = if (snapshot.longestStreak == 1) "day" else "days",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = CyanSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Monthly Activity Calendar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_calendar_card"),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                displayedYearMonthStr = displayedMonth.minusMonths(1).toString()
                            },
                            modifier = Modifier.testTag("calendar_prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month"
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayedMonth.format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (displayedMonth != YearMonth.from(today)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        displayedYearMonthStr = YearMonth.from(today).toString()
                                        selectedDateStr = today.toString()
                                    },
                                    modifier = Modifier.testTag("calendar_jump_today_button")
                                ) {
                                    Text(
                                        text = "Today",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                displayedYearMonthStr = displayedMonth.plusMonths(1).toString()
                            },
                            modifier = Modifier.testTag("calendar_next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month"
                            )
                        }
                    }

                    // Day of week headers (SUN..SAT)
                    val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Calendar Grid
                    val firstOfMonth = displayedMonth.atDay(1)
                    val leadingEmptyCells = firstOfMonth.dayOfWeek.value % 7
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7

                    for (weekIndex in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (dayOfWeekIndex in 0 until 7) {
                                val cellIndex = weekIndex * 7 + dayOfWeekIndex
                                val dayNumber = cellIndex - leadingEmptyCells + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = displayedMonth.atDay(dayNumber)
                                    val status = ChallengeManager.getDayStatus(
                                        date = cellDate,
                                        today = today,
                                        historyByDate = historyByDate,
                                        firstRecordedDate = firstRecordedDate
                                    )
                                    CalendarDayCell(
                                        dayNumber = dayNumber,
                                        status = status,
                                        isToday = cellDate == today,
                                        isSelected = cellDate == selectedDate,
                                        onClick = {
                                            if (hapticEnabled && cellDate != selectedDate) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }
                                            selectedDateStr = cellDate.toString()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("calendar_day_$dayNumber")
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChartLegendDot(color = EmeraldSuccess, label = "Completed")
                        ChartLegendDot(color = CrimsonMissed, label = "Missed")
                        ChartLegendDot(color = AmberWarning, label = "Pending")
                    }
                }
            }

            // 4. Selected Date Detail Card
            SelectedDayDetailCard(
                selectedDate = selectedDate,
                record = historyByDate[selectedDate],
                status = ChallengeManager.getDayStatus(
                    date = selectedDate,
                    today = today,
                    historyByDate = historyByDate,
                    firstRecordedDate = firstRecordedDate
                ),
                dayTarget = selectedDayTarget
            )

            // 5. Last 30 Days Performance Bar Chart Card
            Last30DaysPerformanceChartCard(
                chartPoints = chartPoints,
                selectedDate = selectedDate,
                hapticEnabled = hapticEnabled,
                onSelectDate = { date ->
                    selectedDateStr = date.toString()
                    displayedYearMonthStr = YearMonth.from(date).toString()
                }
            )

            // 6. Empty state when no history exists
            if (history.none { it.isCompleted || (it.actualReps ?: 0) > 0 }) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.history_empty_title),
                    message = stringResource(R.string.history_empty_body),
                    modifier = Modifier.testTag("history_empty_state_card")
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    status: DayChallengeStatus,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBgColor = when (status) {
        DayChallengeStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.20f)
        DayChallengeStatus.MISSED -> CrimsonMissed.copy(alpha = 0.16f)
        DayChallengeStatus.PENDING_TODAY -> AmberWarning.copy(alpha = 0.20f)
        DayChallengeStatus.FUTURE, DayChallengeStatus.BEFORE_START ->
            if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    }

    val targetTextColor = when (status) {
        DayChallengeStatus.COMPLETED -> EmeraldSuccess
        DayChallengeStatus.MISSED -> CrimsonMissed
        DayChallengeStatus.PENDING_TODAY -> AmberWarning
        DayChallengeStatus.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        DayChallengeStatus.BEFORE_START -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 160),
        label = "calendarCellBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.outline
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "calendarCellBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 160),
        label = "calendarCellScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else if (isToday) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (status == DayChallengeStatus.COMPLETED || isSelected || isToday) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = targetTextColor
            )
            if (status == DayChallengeStatus.COMPLETED) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess)
                )
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
private fun SelectedDayDetailCard(
    selectedDate: LocalDate,
    record: PushUpDayEntity?,
    status: DayChallengeStatus,
    dayTarget: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_day_detail_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = Triple(selectedDate, status, record),
            transitionSpec = {
                fadeIn(tween(150)) togetherWith fadeOut(tween(120))
            },
            label = "selectedDayDetailAnimation"
        ) { (date, currentStatus, currentRecord) ->
            val dateLabel = date.format(
                DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
            )
            val completedTimeStr = currentRecord?.completedAtEpochMillis?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val badgeInfo = when (currentStatus) {
                        DayChallengeStatus.COMPLETED -> "COMPLETED" to EmeraldSuccess
                        DayChallengeStatus.MISSED -> "MISSED" to CrimsonMissed
                        DayChallengeStatus.PENDING_TODAY -> "PENDING TODAY" to AmberWarning
                        DayChallengeStatus.BEFORE_START -> "NO RECORD" to MaterialTheme.colorScheme.onSurfaceVariant
                        DayChallengeStatus.FUTURE -> "UPCOMING" to CyanSecondary
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeInfo.second.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badgeInfo.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeInfo.second,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                when (currentStatus) {
                    DayChallengeStatus.COMPLETED -> {
                        val target = currentRecord?.targetReps ?: dayTarget
                        val actual = currentRecord?.effectiveLoggedReps ?: target
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Completed • Target: $target reps • Logged: $actual reps" +
                                    if (completedTimeStr != null) " • Time: $completedTimeStr" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    DayChallengeStatus.MISSED -> {
                        val logged = currentRecord?.effectiveLoggedReps ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = CrimsonMissed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Missed • Target: $dayTarget reps • Logged: $logged reps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonMissed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    DayChallengeStatus.PENDING_TODAY -> {
                        val logged = currentRecord?.effectiveLoggedReps ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pending Today • Target: $dayTarget reps • Logged: $logged reps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberWarning,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    DayChallengeStatus.BEFORE_START -> {
                        Text(
                            text = "No challenge recorded on this date (prior to your first workout).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DayChallengeStatus.FUTURE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Future date • Not yet open for logging.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
