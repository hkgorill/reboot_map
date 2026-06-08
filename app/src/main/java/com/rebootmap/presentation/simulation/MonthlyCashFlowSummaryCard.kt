package com.rebootmap.presentation.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rebootmap.domain.advisory.CashFlowHighlightPlanner
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.YearSnapshot
import com.rebootmap.domain.tax.AnnualHoldingCost
import com.rebootmap.domain.tax.AnnualIncomeBreakdown
import com.rebootmap.domain.tax.AnnualTaxBreakdown
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.theme.SuccessGreen
import com.rebootmap.presentation.theme.WarningRed

@Composable
fun MonthlyCashFlowSummaryCard(
    projection: CashFlowProjection,
    profile: com.rebootmap.domain.model.UserProfile,
    assets: List<Asset> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val snapshots = projection.yearlySnapshots
    if (snapshots.isEmpty()) return

    val highlightRows = remember(projection, profile, assets) {
        CashFlowHighlightPlanner.highlights(projection, profile, assets)
    }
    val highlightAges = remember(highlightRows) { highlightRows.map { it.snapshot.age }.toSet() }
    val yearlyDetails = remember(projection, highlightAges) {
        CashFlowHighlightPlanner.yearlyDetailSnapshots(projection, highlightAges)
    }
    val preRetirementDetailCount = remember(yearlyDetails, profile.retirementAge) {
        yearlyDetails.count { it.age < profile.retirementAge }
    }
    val postRetirementDetailCount = yearlyDetails.size - preRetirementDetailCount

    var expandedIncomeAges by remember { mutableStateOf(setOf<Int>()) }
    var expandedTaxAges by remember { mutableStateOf(setOf<Int>()) }
    var yearlyDetailExpanded by remember { mutableStateOf(false) }

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
                text = "월 수입·월 순현금은 연금·근로 등 정기 수입 기준(부동산 매각·만기 일시금 제외) · 월 세금은 양도세 제외",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "아래는 은퇴·연금·매각 등 전환 시점 위주 요약입니다. " +
                    "나머지 연도는 「연도별 상세」에서 확인할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MonthlyCashFlowHeaderRow()

            var hasAssetDeltaNote = false
            highlightRows.forEach { highlight ->
                val age = highlight.snapshot.age
                val index = snapshots.indexOfFirst { it.age == age }
                val previous = snapshots.getOrNull(index - 1)
                if (previous != null && highlight.snapshot.totalAssets != previous.totalAssets) {
                    hasAssetDeltaNote = true
                }
                Text(
                    text = highlight.labels.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                MonthlyCashFlowBlock(
                    snapshot = highlight.snapshot,
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
            if (hasAssetDeltaNote) {
                Text(
                    text = "※ 월 순현금×12와 다를 수 있음 (투자·연금 운용수익, 부동산 시세, 연금 인출 반영)",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (yearlyDetails.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = { yearlyDetailExpanded = !yearlyDetailExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (yearlyDetailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                    Text(
                        text = yearlyDetailToggleLabel(
                            expanded = yearlyDetailExpanded,
                            preCount = preRetirementDetailCount,
                            postCount = postRetirementDetailCount,
                        ),
                    )
                }
                AnimatedVisibility(visible = yearlyDetailExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        YearlyDetailSnapshotList(
                            details = yearlyDetails,
                            snapshots = snapshots,
                            retirementAge = profile.retirementAge,
                            expandedIncomeAges = expandedIncomeAges,
                            expandedTaxAges = expandedTaxAges,
                            onToggleIncomeBreakdown = { age ->
                                expandedIncomeAges = if (age in expandedIncomeAges) {
                                    expandedIncomeAges - age
                                } else {
                                    expandedIncomeAges + age
                                }
                            },
                            onToggleTaxBreakdown = { age ->
                                expandedTaxAges = if (age in expandedTaxAges) {
                                    expandedTaxAges - age
                                } else {
                                    expandedTaxAges + age
                                }
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "총자산(아래 차트) = 유동 + 비유동(부동산) − 신용·차용 부채 잔액. " +
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

private fun yearlyDetailToggleLabel(expanded: Boolean, preCount: Int, postCount: Int): String {
    val action = if (expanded) "접기" else "펼치기"
    val span = buildList {
        if (preCount > 0) add("은퇴 전 ${preCount}년")
        if (postCount > 0) add("은퇴 후 ${postCount}년")
    }.joinToString(" · ")
    return "연도별 상세 $action ($span)"
}

@Composable
private fun YearlyDetailSnapshotList(
    details: List<YearSnapshot>,
    snapshots: List<YearSnapshot>,
    retirementAge: Int,
    expandedIncomeAges: Set<Int>,
    expandedTaxAges: Set<Int>,
    onToggleIncomeBreakdown: (Int) -> Unit,
    onToggleTaxBreakdown: (Int) -> Unit,
) {
    val preRetirement = details.filter { it.age < retirementAge }
    val postRetirement = details.filter { it.age >= retirementAge }

    if (preRetirement.isNotEmpty()) {
        Text(
            text = "은퇴 전",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        preRetirement.forEach { snapshot ->
            YearlyDetailRow(
                snapshot = snapshot,
                snapshots = snapshots,
                expandedIncomeAges = expandedIncomeAges,
                expandedTaxAges = expandedTaxAges,
                onToggleIncomeBreakdown = onToggleIncomeBreakdown,
                onToggleTaxBreakdown = onToggleTaxBreakdown,
            )
        }
    }
    if (preRetirement.isNotEmpty() && postRetirement.isNotEmpty()) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
    if (postRetirement.isNotEmpty()) {
        Text(
            text = "은퇴 후",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        postRetirement.forEach { snapshot ->
            YearlyDetailRow(
                snapshot = snapshot,
                snapshots = snapshots,
                expandedIncomeAges = expandedIncomeAges,
                expandedTaxAges = expandedTaxAges,
                onToggleIncomeBreakdown = onToggleIncomeBreakdown,
                onToggleTaxBreakdown = onToggleTaxBreakdown,
            )
        }
    }
}

@Composable
private fun YearlyDetailRow(
    snapshot: YearSnapshot,
    snapshots: List<YearSnapshot>,
    expandedIncomeAges: Set<Int>,
    expandedTaxAges: Set<Int>,
    onToggleIncomeBreakdown: (Int) -> Unit,
    onToggleTaxBreakdown: (Int) -> Unit,
) {
    val age = snapshot.age
    val index = snapshots.indexOfFirst { it.age == age }
    MonthlyCashFlowBlock(
        snapshot = snapshot,
        previousSnapshot = snapshots.getOrNull(index - 1),
        incomeExpanded = age in expandedIncomeAges,
        taxExpanded = age in expandedTaxAges,
        onToggleIncomeBreakdown = { onToggleIncomeBreakdown(age) },
        onToggleTaxBreakdown = { onToggleTaxBreakdown(age) },
    )
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
                loanRepayment = snapshot.annualLoanRepayment,
            )
        }
        if (snapshot.incomeBreakdown.lumpSumTotal > 0) {
            Text(
                text = "일시 유입 ${formatKoreanMan(snapshot.incomeBreakdown.lumpSumTotal)}/년 (월 수입·순현금 제외)",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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
    val recurring = buildList {
        if (income.employmentIncome > 0) add("직장 소득" to income.employmentIncome)
        if (income.businessIncome > 0) add("사업 소득" to income.businessIncome)
        if (income.otherFixedIncome > 0) add("기타 고정수입" to income.otherFixedIncome)
        if (income.nationalPension > 0) add("국민연금" to income.nationalPension)
        if (income.severancePension > 0) add("퇴직연금" to income.severancePension)
        if (income.personalPension > 0) add("개인연금" to income.personalPension)
        if (income.housingPension > 0) add("주택연금" to income.housingPension)
    }
    val lumpSum = buildList {
        if (income.realEstateSale > 0) add("부동산 매각 (일시)" to income.realEstateSale)
        if (income.cashSavingsMaturity > 0) add("현금·적금 만기 (일시)" to income.cashSavingsMaturity)
        if (income.yellowUmbrellaPayout > 0) add("노랑우산 일시금" to income.yellowUmbrellaPayout)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (recurring.isNotEmpty()) {
            BreakdownDetailLines(recurring)
        }
        if (lumpSum.isNotEmpty()) {
            Text(
                text = "일시 유입 (월 수입 합계 제외)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BreakdownDetailLines(lumpSum)
        }
    }
}

@Composable
private fun CombinedTaxBreakdownDetail(
    tax: AnnualTaxBreakdown,
    holding: AnnualHoldingCost,
    loanRepayment: Long = 0L,
) {
    val lines = buildTaxBreakdownLines(tax, holding).toMutableList()
    if (loanRepayment > 0) {
        lines += "대출 상환" to loanRepayment
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BreakdownDetailLines(lines.filter { it.first != "양도소득세" })
        if (tax.capitalGainsTax > 0) {
            Text(
                text = "양도소득세 (월 세금 합계 제외)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BreakdownDetailLines(listOf("양도소득세" to tax.capitalGainsTax))
        }
    }
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
    val monthlyIncome = snapshot.incomeBreakdown.recurringTotal / 12
    val monthlyLiving = snapshot.annualLivingExpense / 12
    val monthlyTax = snapshot.recurringAnnualTaxBurden / 12
    val monthlyNet = snapshot.recurringNetCashFlow / 12
    val netColor = if (monthlyNet >= 0) SuccessGreen else WarningRed
    val hasLumpSum = snapshot.incomeBreakdown.lumpSumTotal > 0
    val hasFlow = monthlyLiving > 0 || monthlyIncome > 0 || monthlyTax > 0 || hasLumpSum

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val ageLabel = buildString {
            append("${snapshot.age}세")
            when {
                snapshot.relocationFlags.isTwoHomeOverlap -> append(" ·2주택")
                snapshot.relocationFlags.isGapPeriod -> append(" ·무주택")
            }
        }
        Text(
            text = ageLabel,
            modifier = Modifier.weight(0.65f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        ValueCell(
            text = if (monthlyIncome > 0) formatKoreanMan(monthlyIncome) else if (hasFlow) "0" else "-",
            modifier = Modifier
                .weight(0.95f)
                .clickable(enabled = hasFlow && (monthlyIncome > 0 || hasLumpSum), onClick = onToggleIncomeBreakdown),
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
