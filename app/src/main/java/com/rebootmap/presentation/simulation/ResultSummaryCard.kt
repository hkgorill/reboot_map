package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.YearSpanSummary
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed

@Composable
fun ResultSummaryCard(
    projection: CashFlowProjection,
    retirementAge: Int,
    modifier: Modifier = Modifier,
) {
    val isHealthy = projection.depletionYear == null
    val assetDeclineYears = projection.assetDeclineYears(retirementAge)
    val incomeShortfallYears = projection.deficitYears

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isHealthy) SuccessGreen else WarningRed,
                )
                Text(
                    text = if (isHealthy) "자산 유지 가능" else "자산 고갈 예상",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            SummaryRow(
                label = if (projection.finalBalance < 0) "예상 부채 (적자 누적)" else "최종 예상 자산",
                value = formatKoreanMan(projection.finalBalance),
            )

            SummaryRow(
                label = "자산 고갈 시점",
                value = projection.depletionYear?.let { "${it}년" } ?: "기대 수명까지 유지",
            )

            SummaryYearSpanRow(
                label = "실제 자산 감소",
                summary = projection.yearSpanSummary(assetDeclineYears),
                hint = "전년 대비 총자산 감소 (투자 수익·연금 잔액 반영)",
            )

            SummaryYearSpanRow(
                label = "수입 부족 연도",
                summary = projection.yearSpanSummary(incomeShortfallYears),
                hint = "연금·기타수입 < 생활비+세금",
            )

            if (incomeShortfallYears.isNotEmpty() && assetDeclineYears.size != incomeShortfallYears.size) {
                Text(
                    text = when {
                        assetDeclineYears.size > incomeShortfallYears.size ->
                            "연금·퇴직금 인출은 수입으로 잡히지만 적립 잔액이 줄면 자산 감소 연수가 더 길 수 있습니다."
                        else ->
                            "수입은 부족해도 투자 수익으로 총자산이 늘면 자산 감소 연수가 더 짧을 수 있습니다."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }

            SummaryRow(
                label = "시뮬레이션 기간",
                value = "${projection.yearlySnapshots.size}년",
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SummaryYearSpanRow(
    label: String,
    summary: YearSpanSummary,
    hint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .weight(0.42f, fill = false)
                    .padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Column(
                modifier = Modifier.weight(0.58f, fill = false),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.End,
                )
                summary.rangeLine?.let { range ->
                    Text(
                        text = range,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        )
    }
}
