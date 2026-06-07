package com.rebootmap.presentation.chart

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.presentation.theme.AccentCoral
import com.rebootmap.presentation.theme.PrimaryBlue
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.TextSecondary
import com.rebootmap.presentation.theme.WarningRed

private enum class TimelineSegment {
    PRE_RETIREMENT,
    SURPLUS,
    DEFICIT,
}

@Composable
fun CashFlowChartCard(
    projection: CashFlowProjection,
    retirementAge: Int,
    baselineProjection: CashFlowProjection? = null,
    modifier: Modifier = Modifier,
) {
    val snapshots = projection.yearlySnapshots
    if (snapshots.isEmpty()) return

    val assetDeclineYearSet = projection.assetDeclineYears(retirementAge).toSet()
    val incomeDeficitYearSet = projection.deficitYears.toSet()
    val baselineSnapshots = baselineProjection?.yearlySnapshots.orEmpty()
    val showComparison = baselineSnapshots.isNotEmpty()
    val scenariosDiffer = showComparison && snapshots.indices.any { index ->
        index < baselineSnapshots.size &&
            snapshots[index].endingBalance != baselineSnapshots[index].endingBalance
    }
    val modelProducer = remember { ChartEntryModelProducer() }
    var chartEntriesReady by remember { mutableStateOf(false) }

    LaunchedEffect(snapshots, showComparison) {
        if (showComparison) {
            chartEntriesReady = true
            return@LaunchedEffect
        }
        if (snapshots.isEmpty()) {
            chartEntriesReady = false
            return@LaunchedEffect
        }
        chartEntriesReady = false
        val totalEntries = snapshots.mapIndexed { index, snapshot ->
            entryOf(index.toFloat(), snapshot.totalAssets / 10_000f)
        }
        val illiquidEntries = snapshots.mapIndexed { index, snapshot ->
            entryOf(index.toFloat(), snapshot.illiquidAssets / 10_000f)
        }
        val liquidEntries = snapshots.mapIndexed { index, snapshot ->
            entryOf(index.toFloat(), snapshot.liquidAssets / 10_000f)
        }
        // 총자산 → 비유동 → 유동 순으로 그려 유동선이 겹칠 때 위에 보이게 한다.
        modelProducer.setEntriesSuspending(listOf(totalEntries, illiquidEntries, liquidEntries))
        chartEntriesReady = true
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (showComparison) "시나리오 A/B 비교" else "연도별 자산 추이",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (showComparison) {
                    "주황(●) = A 현재 입력 유지 · 파란 실선 = B 주거 로드맵 적용"
                } else {
                    "총자산 = 현금+투자+연금 적립 잔액+부동산 · 연금 인출 시 잔액 감소 반영"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!showComparison) {
                AssetBreakdownLegend()
            }

            if (showComparison) {
                ComparisonLegend()
                if (!scenariosDiffer) {
                    Text(
                        text = "두 시나리오 결과가 동일합니다. 신규 주택 시세·구입 시점을 확인하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (showComparison) {
                ComparisonCashFlowChart(
                    maintainSnapshots = baselineSnapshots,
                    relocationSnapshots = snapshots,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            } else if (chartEntriesReady) {
                ProvideChartStyle {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = PrimaryBlue.toArgb(),
                                    pointSizeDp = 4f,
                                ),
                                LineChart.LineSpec(
                                    lineColor = AccentCoral.toArgb(),
                                    pointSizeDp = 4f,
                                ),
                                LineChart.LineSpec(
                                    lineColor = SuccessGreen.toArgb(),
                                    pointSizeDp = 5f,
                                ),
                            ),
                            spacing = 4.dp,
                        ),
                        chartModelProducer = modelProducer,
                        startAxis = rememberStartAxis(
                            title = "만원",
                            valueFormatter = { value, _ -> formatChartManAxis(value) },
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ ->
                                snapshots.getOrNull(value.toInt())?.let { "${it.age}세" } ?: ""
                            },
                        ),
                        chartScrollState = rememberChartScrollState(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "차트 갱신 중…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DeficitAgeTimeline(
                projection = projection,
                snapshots = snapshots,
                assetDeclineYearSet = assetDeclineYearSet,
                incomeDeficitYearSet = incomeDeficitYearSet,
                retirementAge = retirementAge,
            )
        }
    }
}

@Composable
private fun ComparisonCashFlowChart(
    maintainSnapshots: List<YearSnapshot>,
    relocationSnapshots: List<YearSnapshot>,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val count = minOf(maintainSnapshots.size, relocationSnapshots.size)
        if (count == 0) return@Canvas

        val maintainValues = maintainSnapshots.take(count).map { it.endingBalance / 10_000f }
        val relocationValues = relocationSnapshots.take(count).map { it.endingBalance / 10_000f }
        val allValues = maintainValues + relocationValues
        val minValue = minOf(0f, allValues.minOrNull() ?: 0f)
        val maxValue = maxOf(0f, allValues.maxOrNull() ?: 0f)
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val leftPadding = 42.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 22.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        fun xAt(index: Int): Float =
            leftPadding + if (count == 1) 0f else chartWidth * index / (count - 1)

        fun yAt(value: Float): Float =
            topPadding + (maxValue - value) / range * chartHeight

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = 11.dp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val horizontalLines = 5
        repeat(horizontalLines + 1) { step ->
            val fraction = step / horizontalLines.toFloat()
            val y = topPadding + chartHeight * fraction
            val value = maxValue - range * fraction
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatChartManAxis(value),
                0f,
                y + 4.dp.toPx(),
                textPaint,
            )
        }

        val verticalLines = 10
        repeat(verticalLines + 1) { step ->
            val x = leftPadding + chartWidth * step / verticalLines
            drawLine(
                color = gridColor.copy(alpha = 0.7f),
                start = Offset(x, topPadding),
                end = Offset(x, topPadding + chartHeight),
                strokeWidth = 1.dp.toPx(),
            )
        }

        fun buildPath(values: List<Float>): Path = Path().apply {
            values.forEachIndexed { index, value ->
                val x = xAt(index)
                val y = yAt(value)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        val relocationPath = buildPath(relocationValues)
        val maintainPath = buildPath(maintainValues)

        // B를 먼저 그리고 A 점선을 위에 올려, 두 값이 같아도 두 시나리오가 모두 보이게 한다.
        drawPath(
            path = relocationPath,
            color = PrimaryBlue,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = maintainPath,
            color = AccentCoral,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())),
            ),
        )

        val markerStep = (count / 8).coerceAtLeast(1)
        maintainValues.forEachIndexed { index, value ->
            if (index % markerStep == 0 || index == count - 1) {
                drawCircle(
                    color = AccentCoral,
                    radius = 3.5.dp.toPx(),
                    center = Offset(xAt(index), yAt(value)),
                )
            }
        }
        relocationValues.forEachIndexed { index, value ->
            if (index % markerStep == 0 || index == count - 1) {
                drawCircle(
                    color = PrimaryBlue,
                    radius = 2.5.dp.toPx(),
                    center = Offset(xAt(index), yAt(value)),
                )
            }
        }

        val firstAge = relocationSnapshots.firstOrNull()?.age
        val lastAge = relocationSnapshots.getOrNull(count - 1)?.age
        if (firstAge != null && lastAge != null) {
            val labelY = size.height - 4.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText("${firstAge}세", leftPadding, labelY, textPaint)
            val lastLabel = "${lastAge}세"
            drawContext.canvas.nativeCanvas.drawText(
                lastLabel,
                size.width - rightPadding - textPaint.measureText(lastLabel),
                labelY,
                textPaint,
            )
        }
    }
}

@Composable
private fun DeficitAgeTimeline(
    projection: CashFlowProjection,
    snapshots: List<YearSnapshot>,
    assetDeclineYearSet: Set<Int>,
    incomeDeficitYearSet: Set<Int>,
    retirementAge: Int,
) {
    val firstAge = snapshots.firstOrNull()?.age
    val lastAge = snapshots.lastOrNull()?.age

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "현금흐름 타임라인",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "위 그래프와 같은 나이 순서입니다. 막대 한 칸 = 1년",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "은퇴(${retirementAge}세) 이후, 총자산이 전년보다 줄어든 해를 빨간색으로 표시합니다. " +
                "월 순현금이 흑자여도 연금 적립 잔액이 소진되면 빨간색이 될 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TimelineLegend()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            snapshots.forEach { snapshot ->
                val segment = timelineSegment(snapshot, assetDeclineYearSet, retirementAge)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(segment.color()),
                )
            }
        }

        if (firstAge != null && lastAge != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${firstAge}세",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${lastAge}세",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (assetDeclineYearSet.isEmpty()) {
            Text(
                text = "은퇴 후 자산 감소 연도 없음 — 총자산이 유지되거나 증가합니다",
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(
                text = "자산 감소 ${projection.formatYearSpan(assetDeclineYearSet.toList())}",
                style = MaterialTheme.typography.bodySmall,
                color = WarningRed,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (incomeDeficitYearSet.isNotEmpty()) {
            Text(
                text = "수입 부족 ${projection.formatYearSpan(incomeDeficitYearSet.toList())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssetBreakdownLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineLegendItem(color = SuccessGreen, label = "유동자산")
        TimelineLegendItem(color = AccentCoral, label = "비유동(부동산)")
        TimelineLegendItem(color = PrimaryBlue, label = "총자산")
    }
}

@Composable
private fun ComparisonLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineLegendItem(color = AccentCoral, label = "A · 현재 입력")
        TimelineLegendItem(color = PrimaryBlue, label = "B · 주거 로드맵")
    }
}

@Composable
private fun TimelineLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineLegendItem(color = TimelineSegment.PRE_RETIREMENT.color(), label = "은퇴 전")
        TimelineLegendItem(color = TimelineSegment.SURPLUS.color(), label = "자산 유지·증가")
        TimelineLegendItem(color = TimelineSegment.DEFICIT.color(), label = "자산 감소")
    }
}

@Composable
private fun TimelineLegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun timelineSegment(
    snapshot: YearSnapshot,
    assetDeclineYearSet: Set<Int>,
    retirementAge: Int,
): TimelineSegment = when {
    snapshot.age < retirementAge -> TimelineSegment.PRE_RETIREMENT
    snapshot.year in assetDeclineYearSet -> TimelineSegment.DEFICIT
    else -> TimelineSegment.SURPLUS
}

private fun TimelineSegment.color(): Color = when (this) {
    TimelineSegment.PRE_RETIREMENT -> TextSecondary.copy(alpha = 0.35f)
    TimelineSegment.SURPLUS -> SuccessGreen.copy(alpha = 0.55f)
    TimelineSegment.DEFICIT -> WarningRed.copy(alpha = 0.9f)
}

private fun formatChartManAxis(value: Float): String {
    val prefix = if (value < 0f) "-" else ""
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 10_000f -> "${prefix}${(abs / 10_000f).toInt()}억"
        else -> "${prefix}${abs.toInt()}"
    }
}
