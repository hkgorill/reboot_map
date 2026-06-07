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
import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualIncomeBreakdown
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed

private fun YearSnapshot.monthlyTaxTotal(): Long =
    (annualTax + annualHoldingCost.total) / 12

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

    var expandedIncomeAges by remember { mutableStateOf(setOf<Int>()) }
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
                text = "월 수입은 세전 · 월 세금은 소득세·건보·보유세(재산세·종부세) 포함 · 월 순현금은 생활비·세금 차감 후",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "월 수입·월 세금 열 탭 시 항목별 표시",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MonthlyCashFlowHeaderRow()

            var hasAssetDeltaNote = false
            milestoneAges.forEach { age ->
                val index = snapshots.indexOfFirst { it.age == age }
                if (index >= 0) {
                    val previous = snapshots.getOrNull(index - 1)
                    if (previous != null && snapshots[index].totalAssets != previous.totalAssets) {
                        hasAssetDeltaNote = true
                    }
                    MonthlyCashFlowBlock(
                        snapshot = snapshots[index],
                        previousSnapshot = previous,
                        incomeExpanded = age in expandedIncomeAges,
                        taxExpanded = age in expandedTaxAges,
                        onToggleIncomeBreakdown = {
                            expandedIncomeAges = if (age in expandedIncomeAges) {
                                expandedIncomeAges - age
                            } else {
                                expandedIncomeAges + age
                            }
                        },
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
            if (hasAssetDeltaNote) {
                Text(
                    text = "※ 월 순현금×12와 다를 수 있음 (투자·연금 운용수익, 부동산 시세, 연금 인출 반영)",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
                    incomeExpanded = firstRetired.age in expandedIncomeAges,
                    taxExpanded = firstRetired.age in expandedTaxAges,
                    onToggleIncomeBreakdown = {
                        val age = firstRetired.age
                        expandedIncomeAges = if (age in expandedIncomeAges) {
                            expandedIncomeAges - age
                        } else {
                            expandedIncomeAges + age
                        }
                    },
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
        HeaderCell("나이", Modifier.weight(0.65f))
        HeaderCell("월 수입", Modifier.weight(0.95f))
        HeaderCell("월 생활비", Modifier.weight(0.95f))
        HeaderCell("월 세금", Modifier.weight(0.95f))
        HeaderCell("월 순현금", Modifier.weight(1f))
    }
}

@Composable
private fun MonthlyCashFlowBlock(
    snapshot: YearSnapshot,
    previousSnapshot: YearSnapshot?,
    incomeExpanded: Boolean,
    taxExpanded: Boolean,
    onToggleIncomeBreakdown: () -> Unit,
    onToggleTaxBreakdown: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MonthlyCashFlowRow(
            snapshot = snapshot,
            onToggleIncomeBreakdown = onToggleIncomeBreakdown,
            onToggleTaxBreakdown = onToggleTaxBreakdown,
        )
        AnimatedVisibility(visible = incomeExpanded) {
            IncomeBreakdownDetail(snapshot.incomeBreakdown)
        }
        AnimatedVisibility(visible = taxExpanded) {
            CombinedTaxBreakdownDetail(
                tax = snapshot.taxBreakdown,
                holding = snapshot.annualHoldingCost,
            )
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
            }
        }
    }
}

@Composable
private fun IncomeBreakdownDetail(income: AnnualIncomeBreakdown) {
    val lines = buildList {
        if (income.employmentIncome > 0) add("직장 소득" to income.employmentIncome)
        if (income.businessIncome > 0) add("사업 소득" to income.businessIncome)
        if (income.otherFixedIncome > 0) add("기타 고정수입" to income.otherFixedIncome)
        if (income.nationalPension > 0) add("국민연금" to income.nationalPension)
        if (income.severancePension > 0) add("퇴직연금" to income.severancePension)
        if (income.personalPension > 0) add("개인연금" to income.personalPension)
        if (income.housingPension > 0) add("주택연금" to income.housingPension)
        if (income.realEstateSale > 0) add("부동산 매각" to income.realEstateSale)
        if (income.cashSavingsMaturity > 0) add("현금·적금 만기" to income.cashSavingsMaturity)
        if (income.yellowUmbrellaPayout > 0) add("노랑우산 일시금" to income.yellowUmbrellaPayout)
    }
    BreakdownDetailLines(lines)
}

@Composable
private fun CombinedTaxBreakdownDetail(
    tax: AnnualTaxBreakdown,
    holding: AnnualHoldingCost,
) {
    val lines = buildTaxBreakdownLines(tax, holding)
    BreakdownDetailLines(lines)
}

private fun buildTaxBreakdownLines(
    tax: AnnualTaxBreakdown,
    holding: AnnualHoldingCost,
): List<Pair<String, Long>> = buildList {
    if (tax.employmentIncomeTax > 0) add("근로소득세" to tax.employmentIncomeTax)
    if (tax.businessIncomeTax > 0) add("사업소득세" to tax.businessIncomeTax)
    if (tax.pensionIncomeTax > 0) add("연금소득세" to tax.pensionIncomeTax)
    if (tax.otherIncomeTax > 0) add("기타소득세" to tax.otherIncomeTax)
    if (tax.capitalGainsTax > 0) add("양도소득세" to tax.capitalGainsTax)
    if (tax.healthInsurance > 0) add("건강보험료" to tax.healthInsurance)
    if (tax.longTermCare > 0) add("장기요양보험" to tax.longTermCare)
    if (holding.residentialPropertyTax > 0) add("재산세(주거용)" to holding.residentialPropertyTax)
    if (holding.nonResidentialPropertyTax > 0) add("재산세(비주거용)" to holding.nonResidentialPropertyTax)
    if (holding.propertyTax > 0 &&
        holding.residentialPropertyTax == 0L &&
        holding.nonResidentialPropertyTax == 0L
    ) {
        add("재산세" to holding.propertyTax)
    }
    if (holding.comprehensiveRealEstateTax > 0) add("종부세" to holding.comprehensiveRealEstateTax)
}

@Composable
private fun BreakdownDetailLines(lines: List<Pair<String, Long>>) {
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
    onToggleIncomeBreakdown: () -> Unit,
    onToggleTaxBreakdown: () -> Unit,
) {
    val monthlyIncome = snapshot.annualIncome / 12
    val monthlyLiving = snapshot.annualLivingExpense / 12
    val monthlyTax = snapshot.monthlyTaxTotal()
    val monthlyNet = snapshot.netCashFlow / 12
    val netColor = if (monthlyNet >= 0) SuccessGreen else WarningRed
    val hasFlow = monthlyLiving > 0 || monthlyIncome > 0 || monthlyTax > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${snapshot.age}세",
            modifier = Modifier.weight(0.65f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        ValueCell(
            text = if (monthlyIncome > 0) formatKoreanMan(monthlyIncome) else if (hasFlow) "0" else "-",
            modifier = Modifier
                .weight(0.95f)
                .clickable(enabled = hasFlow && monthlyIncome > 0, onClick = onToggleIncomeBreakdown),
            small = true,
        )
        ValueCell(
            if (monthlyLiving > 0) formatKoreanMan(monthlyLiving) else "-",
            Modifier.weight(0.95f),
            small = true,
        )
        ValueCell(
            text = if (hasFlow && monthlyTax > 0) formatKoreanMan(monthlyTax) else if (hasFlow) "0" else "-",
            modifier = Modifier
                .weight(0.95f)
                .clickable(enabled = hasFlow && monthlyTax > 0, onClick = onToggleTaxBreakdown),
            small = true,
        )
        Text(
            text = if (hasFlow) formatKoreanMan(monthlyNet) else "-",
            modifier = Modifier.weight(1f),
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
