package com.example.ui.history

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ChartDayPoint
import com.example.domain.DayChallengeStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonMissed
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("NewApi")
@Composable
fun Last30DaysPerformanceChartCard(
    chartPoints: List<ChartDayPoint>,
    selectedDate: LocalDate,
    hapticEnabled: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chartPoints.isEmpty()) return

    val view = LocalView.current

    var chartRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        chartRevealed = true
    }
    val barGrowthFraction by animateFloatAsState(
        targetValue = if (chartRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "chartBarGrowthFraction"
    )

    var selectedBarIndex by rememberSaveable(chartPoints.size) {
        mutableIntStateOf(chartPoints.lastIndex.coerceAtLeast(0))
    }

    // Keep chart selection synchronized when the user selects a date inside the 30-day window from the calendar
    LaunchedEffect(selectedDate, chartPoints) {
        val matchingIndex = chartPoints.indexOfFirst { it.date == selectedDate }
        if (matchingIndex >= 0) {
            selectedBarIndex = matchingIndex
        }
    }

    val safeIndex = selectedBarIndex.coerceIn(0, chartPoints.lastIndex)
    val selectedPoint = chartPoints[safeIndex]

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = tween(durationMillis = 180),
        label = "chartSelectedColumnIndex"
    )

    val maxChartValue = remember(chartPoints) {
        val rawMax = chartPoints.maxOfOrNull { maxOf(it.actualReps, it.targetReps) } ?: 10
        ((rawMax + 4) / 5) * 5
    }.coerceAtLeast(10)

    val midChartValue = maxChartValue / 2

    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val emptyTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val highlightColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("last_30_days_chart_card"),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Last 30 Days Performance",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Tap or slide across bars to inspect daily repetitions vs. target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selected Bar Tooltip / Callout with smooth transition
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_selected_tooltip")
            ) {
                AnimatedContent(
                    targetState = selectedPoint,
                    transitionSpec = {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                    },
                    label = "chartTooltipTransition"
                ) { point ->
                    val tooltipDate = point.date.format(
                        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    )
                    val statusText = when (point.status) {
                        DayChallengeStatus.COMPLETED -> "Completed"
                        DayChallengeStatus.MISSED -> "Missed"
                        DayChallengeStatus.PENDING_TODAY -> "Pending Today"
                        DayChallengeStatus.BEFORE_START -> "No Record"
                        DayChallengeStatus.FUTURE -> "Future"
                    }
                    val statusColor = when (point.status) {
                        DayChallengeStatus.COMPLETED -> EmeraldSuccess
                        DayChallengeStatus.MISSED -> CrimsonMissed
                        DayChallengeStatus.PENDING_TODAY -> AmberWarning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tooltipDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ACTUAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${point.actualReps} reps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (point.actualReps >= point.targetReps && point.status == DayChallengeStatus.COMPLETED) {
                                        EmeraldSuccess
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TARGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${point.targetReps} reps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Chart Canvas with Y-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .height(150.dp)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$maxChartValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$midChartValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive Bar & Target Line Canvas
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .testTag("last_30_days_canvas")
                        .pointerInput(chartPoints, hapticEnabled) {
                            detectTapGestures { tapOffset ->
                                if (size.width > 0 && chartPoints.isNotEmpty()) {
                                    val slotWidth = size.width.toFloat() / chartPoints.size
                                    val tappedIdx = (tapOffset.x / slotWidth)
                                        .toInt()
                                        .coerceIn(0, chartPoints.lastIndex)
                                    if (tappedIdx != selectedBarIndex && hapticEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    selectedBarIndex = tappedIdx
                                    onSelectDate(chartPoints[tappedIdx].date)
                                }
                            }
                        }
                        .pointerInput(chartPoints, hapticEnabled) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                if (size.width > 0 && chartPoints.isNotEmpty()) {
                                    val slotWidth = size.width.toFloat() / chartPoints.size
                                    val draggedIdx = (change.position.x / slotWidth)
                                        .toInt()
                                        .coerceIn(0, chartPoints.lastIndex)
                                    if (draggedIdx != selectedBarIndex) {
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        selectedBarIndex = draggedIdx
                                        onSelectDate(chartPoints[draggedIdx].date)
                                    }
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val count = chartPoints.size
                    if (count == 0) return@Canvas

                    // Horizontal reference grid lines (top, mid, bottom)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    listOf(0f, canvasHeight * 0.5f, canvasHeight).forEach { y ->
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (y < canvasHeight) dashEffect else null
                        )
                    }

                    val slotWidth = canvasWidth / count
                    val barWidth = (slotWidth * 0.62f).coerceAtLeast(3.dp.toPx())

                    // Smoothly animated selection column highlight
                    drawRoundRect(
                        color = highlightColor.copy(alpha = 0.12f),
                        topLeft = Offset(animatedSelectedIndex * slotWidth, 0f),
                        size = Size(slotWidth, canvasHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // 1. Draw Bars for each of the 30 days
                    chartPoints.forEachIndexed { index, point ->
                        val centerX = index * slotWidth + slotWidth / 2f
                        val barLeft = centerX - barWidth / 2f

                        if (point.actualReps > 0) {
                            val ratio = (point.actualReps.toFloat() / maxChartValue.toFloat()).coerceIn(0f, 1f)
                            val barHeight = (ratio * canvasHeight * barGrowthFraction).coerceAtLeast(4.dp.toPx())
                            val barTop = canvasHeight - barHeight
                            val barColor = when {
                                point.status == DayChallengeStatus.COMPLETED -> EmeraldSuccess
                                else -> AmberWarning
                            }
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        } else {
                            // Zero-activity baseline tick (red if missed after start, muted otherwise)
                            val tickHeight = 3.dp.toPx()
                            val tickColor = if (point.status == DayChallengeStatus.MISSED) {
                                CrimsonMissed.copy(alpha = 0.70f)
                            } else {
                                emptyTickColor
                            }
                            drawRoundRect(
                                color = tickColor,
                                topLeft = Offset(barLeft, canvasHeight - tickHeight),
                                size = Size(barWidth, tickHeight),
                                cornerRadius = CornerRadius(1.5.dp.toPx())
                            )
                        }
                    }

                    // 2. Draw Daily Target Reference Line across the 30 days
                    val targetPath = Path()
                    chartPoints.forEachIndexed { index, point ->
                        val centerX = index * slotWidth + slotWidth / 2f
                        val targetRatio = (point.targetReps.toFloat() / maxChartValue.toFloat()).coerceIn(0f, 1f)
                        val targetY = canvasHeight - (targetRatio * canvasHeight)
                        if (index == 0) {
                            targetPath.moveTo(centerX, targetY)
                        } else {
                            targetPath.lineTo(centerX, targetY)
                        }
                    }
                    drawPath(
                        path = targetPath,
                        color = CyanSecondary,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )
                }
            }

            // X-Axis Date Labels (Start, Middle, Today)
            val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = chartPoints.first().date.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = chartPoints[chartPoints.size / 2].date.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = chartPoints.last().date.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendDot(color = EmeraldSuccess, label = "Actual (Done)")
                ChartLegendDot(color = CyanSecondary, label = "Daily Target")
                ChartLegendDot(color = CrimsonMissed, label = "Missed (0)")
            }
        }
    }
}

@Composable
internal fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
