package com.rebootmap.presentation.chart

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
) {
    val snapshots = projection.yearlySnapshots
    if (snapshots.isEmpty()) return

    val assetDeclineYearSet = projection.assetDeclineYears(retirementAge).toSet()
    val incomeDeficitYearSet = projection.deficitYears.toSet()
    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(projection) {
        modelProducer.setEntries(
            snapshots.mapIndexed { index, snapshot ->
                entryOf(index.toFloat(), snapshot.endingBalance / 10_000f)
            },
        )
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
                text = "연도별 자산 추이",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "가로축 = 나이(세) · 세로축 = 총자산(만원)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ProvideChartStyle {
                Chart(
                    chart = lineChart(
                        lines = listOf(
                            LineChart.LineSpec(
                                lineColor = PrimaryBlue.toArgb(),
                                pointSizeDp = 6f,
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
                "투자 수익률 변경에 따라 함께 갱신됩니다.",
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
