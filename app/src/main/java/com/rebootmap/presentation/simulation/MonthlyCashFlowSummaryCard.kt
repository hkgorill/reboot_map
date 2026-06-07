package com.rebootmap.presentation.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed

@Composable
fun MonthlyCashFlowSummaryCard(
    projection: CashFlowProjection,
    retirementAge: Int,
    modifier: Modifier = Modifier,
) {
    val snapshots = projection.yearlySnapshots
    if (snapshots.isEmpty()) return

    val milestoneAges = buildList {
        add(retirementAge)
        add(65)
        add(70)
        add(80)
    }.distinct().sorted().filter { age ->
        snapshots.any { it.age == age }
    }

    var expandedTaxAges by remember { mutableStateOf(setOf<Int>()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "월 순수입 vs 생활비",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "월 수입은 세전 · 월 부과는 재산세·종부세(보유) · 월 순현금은 생활비·부과·세금 차감 후",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "월 세금 열 탭 시 세목별 표시",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MonthlyCashFlowHeaderRow()

            milestoneAges.forEach { age ->
                val index = snapshots.indexOfFirst { it.age == age }
                if (index >= 0) {
                    MonthlyCashFlowBlock(
                        snapshot = snapshots[index],
                        previousSnapshot = snapshots.getOrNull(index - 1),
                        taxExpanded = age in expandedTaxAges,
                        onToggleTaxBreakdown = {
                            expandedTaxAges = if (age in expandedTaxAges) {
                                expandedTaxAges - age
                            } else {
                                expandedTaxAges + age
                            }
                        },
                    )
                }
            }

            val firstRetired = snapshots.firstOrNull { it.age >= retirementAge && it.annualExpense > 0 }
            if (firstRetired != null && firstRetired.age !in milestoneAges) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "은퇴 직후 (${firstRetired.age}세)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val index = snapshots.indexOfFirst { it.age == firstRetired.age }
                MonthlyCashFlowBlock(
                    snapshot = firstRetired,
                    previousSnapshot = snapshots.getOrNull(index - 1),
                    taxExpanded = firstRetired.age in expandedTaxAges,
                    onToggleTaxBreakdown = {
                        val age = firstRetired.age
                        expandedTaxAges = if (age in expandedTaxAges) {
                            expandedTaxAges - age
                        } else {
                            expandedTaxAges + age
                        }
                    },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "총자산(아래 차트) = 유동(현금·투자·연금 잔액) + 비유동(부동산). " +
                    "월 순현금은 현금흐름만 보여 주며, 투자·연금 운용수익·부동산 시세 변동은 총자산 증감에만 반영됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "연금 인출은 월 수입에 포함되지만 적립 잔액은 줄어, 순현금이 흑자(+)여도 총자산이 감소할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthlyCashFlowHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HeaderCell("나이", Modifier.weight(0.6f))
        HeaderCell("월 수입", Modifier.weight(0.85f))
        HeaderCell("월 생활비", Modifier.weight(0.85f))
        HeaderCell("월 부과", Modifier.weight(0.75f))
        HeaderCell("월 세금", Modifier.weight(0.75f))
        HeaderCell("월 순현금", Modifier.weight(0.9f))
    }
}

@Composable
private fun MonthlyCashFlowBlock(
    snapshot: YearSnapshot,
    previousSnapshot: YearSnapshot?,
    taxExpanded: Boolean,
    onToggleTaxBreakdown: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MonthlyCashFlowRow(
            snapshot = snapshot,
            onToggleTaxBreakdown = onToggleTaxBreakdown,
        )
        AnimatedVisibility(visible = taxExpanded) {
            TaxBreakdownDetail(snapshot.taxBreakdown)
        }
        previousSnapshot?.let { previous ->
            val assetDelta = snapshot.totalAssets - previous.totalAssets
            if (assetDelta != 0L) {
                val deltaColor = if (assetDelta > 0) SuccessGreen else WarningRed
                val sign = if (assetDelta > 0) "+" else ""
                Text(
                    text = "총자산 전년 대비 $sign${formatKoreanMan(assetDelta)}/년",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = deltaColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "※ 월 순현금×12와 다를 수 있음 (투자·연금 운용수익, 부동산 시세, 연금 인출 반영)",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TaxBreakdownDetail(breakdown: AnnualTaxBreakdown) {
    val lines = buildList {
        if (breakdown.employmentIncomeTax > 0) add("근로" to breakdown.employmentIncomeTax)
        if (breakdown.businessIncomeTax > 0) add("사업" to breakdown.businessIncomeTax)
        if (breakdown.pensionIncomeTax > 0) add("연금" to breakdown.pensionIncomeTax)
        if (breakdown.otherIncomeTax > 0) add("기타" to breakdown.otherIncomeTax)
        if (breakdown.capitalGainsTax > 0) add("양도" to breakdown.capitalGainsTax)
        if (breakdown.healthInsurance > 0) add("건보" to breakdown.healthInsurance)
        if (breakdown.longTermCare > 0) add("장기요양" to breakdown.longTermCare)
    }
    if (lines.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { (label, annual) ->
            Text(
                text = "$label ${formatKoreanMan(annual / 12)}/월",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MonthlyCashFlowRow(
    snapshot: YearSnapshot,
    onToggleTaxBreakdown: () -> Unit,
) {
    val monthlyIncome = snapshot.annualIncome / 12
    val monthlyLiving = snapshot.annualLivingExpense / 12
    val monthlyHolding = snapshot.annualHoldingCost.total / 12
    val monthlyTax = snapshot.annualTax / 12
    val monthlyNet = snapshot.netCashFlow / 12
    val netColor = if (monthlyNet >= 0) SuccessGreen else WarningRed
    val hasFlow = monthlyLiving > 0 || monthlyIncome > 0 || monthlyHolding > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${snapshot.age}세",
            modifier = Modifier.weight(0.6f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        ValueCell(formatKoreanMan(monthlyIncome), Modifier.weight(0.85f), small = true)
        ValueCell(
            if (monthlyLiving > 0) formatKoreanMan(monthlyLiving) else "-",
            Modifier.weight(0.85f),
            small = true,
        )
        ValueCell(
            if (monthlyHolding > 0) formatKoreanMan(monthlyHolding) else if (hasFlow) "0" else "-",
            Modifier.weight(0.75f),
            small = true,
        )
        ValueCell(
            text = if (hasFlow && monthlyTax > 0) formatKoreanMan(monthlyTax) else if (hasFlow) "0" else "-",
            modifier = Modifier
                .weight(0.75f)
                .clickable(enabled = hasFlow && monthlyTax > 0, onClick = onToggleTaxBreakdown),
            small = true,
        )
        Text(
            text = if (hasFlow) formatKoreanMan(monthlyNet) else "-",
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = netColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ValueCell(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

