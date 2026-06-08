package com.rebootmap.presentation.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.portfolio.RealEstatePortfolioEngine
import com.rebootmap.domain.portfolio.RealEstateTimingReport
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed
import java.time.Year
import kotlin.math.roundToInt

@Composable
fun RealEstateTimingConsultCard(
    estates: List<Asset.RealEstate>,
    report: RealEstateTimingReport,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSaleYearChange: (estateId: String, saleYear: Int) -> Unit,
    onApplySuggestions: () -> Unit,
    estimatedTaxWon: Long,
    modifier: Modifier = Modifier,
) {
    val currentYear = Year.now().value
    val configured = estates.filter { it.currentValue > 0 || it.debtAmount > 0 }
    val summary = buildString {
        append(report.headline)
        if (report.overlapYears.isNotEmpty()) {
            append(" · 2주택 ${report.overlapYears.size}년")
        }
        if (estimatedTaxWon > 0) {
            append(" · 거래세금(간이) ${formatKoreanMan(estimatedTaxWon)}")
        }
    }

    ExpandableCard(
        title = "부동산 최적 보유 타이밍 컨설팅",
        summary = summary,
        icon = Icons.Outlined.Schedule,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        Text(
            text = report.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = report.headline,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        report.strengths.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall, color = SuccessGreen) }
        report.weaknesses.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall, color = WarningRed) }
        report.watchPoints.forEach {
            Text("· $it", style = MaterialTheme.typography.bodySmall)
        }

        if (report.suggestedSaleYears.isNotEmpty()) {
            OutlinedButton(onClick = onApplySuggestions, modifier = Modifier.fillMaxWidth()) {
                Text("제안 매각 시점 적용 (${report.suggestedSaleYears.size}건)")
            }
        }

        if (configured.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "타임라인 — 매각 핸들(●)을 드래그해 연도 조정",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val endYear = (currentYear + 20).coerceAtLeast(
                configured.mapNotNull { it.saleYear }.maxOrNull() ?: currentYear,
            )
            configured
                .filter { it.saleYear != null && it.saleYear > currentYear }
                .forEachIndexed { index, estate ->
                    val label = estate.displayTitle(
                        configured.indexOfFirst { it.id == estate.id },
                        configured.size,
                    )
                    EstateTimelineRow(
                        estate = estate,
                        label = label,
                        startYear = currentYear,
                        endYear = endYear,
                        onSaleYearChange = { year -> onSaleYearChange(estate.id, year) },
                    )
                }
            if (configured.none { it.saleYear != null && it.saleYear > currentYear }) {
                Text(
                    text = "매각 예정 연도를 입력하면 타임라인에서 드래그로 조정할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EstateTimelineRow(
    estate: Asset.RealEstate,
    label: String,
    startYear: Int,
    endYear: Int,
    onSaleYearChange: (Int) -> Unit,
) {
    val yearSpan = (endYear - startYear).coerceAtLeast(1)
    val yearWidthDp = 36.dp
    val density = LocalDensity.current
    val yearWidthPx = with(density) { yearWidthDp.toPx() }

    val acqYear = estate.acquisitionYear ?: startYear
    val saleYear = estate.saleYear ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Text(
                text = "${acqYear}년~${saleYear}년 매각",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .height(48.dp),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (year in startYear..endYear) {
                    val owned = year in acqYear until saleYear
                    val isOverlap = owned && yearSpan > 0 // highlight in parent later
                    Box(
                        modifier = Modifier
                            .width(yearWidthDp)
                            .height(8.dp)
                            .background(
                                color = when {
                                    !owned -> MaterialTheme.colorScheme.surfaceVariant
                                    isOverlap -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }

            var dragAccum by remember(estate.id, saleYear) { mutableFloatStateOf(0f) }
            val handleOffsetPx = ((saleYear - startYear).coerceIn(0, yearSpan)) * yearWidthPx

            Box(
                modifier = Modifier
                    .offset { IntOffset(handleOffsetPx.roundToInt(), 0) }
                    .padding(top = 4.dp)
                    .width(28.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(estate.id, saleYear, startYear, endYear, acqYear) {
                        detectDragGestures(
                            onDragEnd = { dragAccum = 0f },
                        ) { _, dragAmount ->
                            dragAccum += dragAmount.x
                            val yearDelta = (dragAccum / yearWidthPx).roundToInt()
                            if (yearDelta != 0) {
                                val minSale = (acqYear + 1).coerceAtLeast(startYear + 1)
                                val newYear = (saleYear + yearDelta).coerceIn(minSale, endYear)
                                if (newYear != saleYear) {
                                    onSaleYearChange(newYear)
                                    dragAccum -= yearDelta * yearWidthPx
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "●",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
