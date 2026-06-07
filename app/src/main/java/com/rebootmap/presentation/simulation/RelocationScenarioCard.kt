package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.scenario.PurchaseTiming
import com.rebootmap.domain.scenario.RelocationPlan
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.formatKoreanMan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelocationScenarioCard(
    plan: RelocationPlan,
    realEstates: List<Asset.RealEstate>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlanChange: (RelocationPlan) -> Unit,
    onAddBuyEstate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sellEstate = plan.resolveSellEstate(realEstates)
    val saleYear = sellEstate?.saleYear
    val summary = relocationCardSummary(plan, realEstates)

    ExpandableCard(
        title = "주거 로드맵",
        summary = summary,
        icon = Icons.Outlined.SwapHoriz,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "시나리오 활성화", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = plan.enabled,
                onCheckedChange = { onPlanChange(plan.copy(enabled = it)) },
            )
        }

        if (realEstates.isEmpty()) {
            Text(
                text = "부동산 카드에서 자산을 먼저 입력하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            return@ExpandableCard
        }

        Text(
            text = "매각할 부동산",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            realEstates.forEachIndexed { index, estate ->
                EstateChip(
                    label = estate.displayTitle(estateOrdinal = index, estateCount = realEstates.size),
                    selected = plan.sellEstateId == estate.id ||
                        (plan.sellEstateId.isBlank() && sellEstate?.id == estate.id),
                    onClick = { onPlanChange(plan.copy(sellEstateId = estate.id)) },
                )
            }
        }

        if (saleYear == null) {
            Text(
                text = "선택한 부동산에 매각 예정 연도를 설정하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                text = "매각 연도: ${saleYear}년",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = "이주 후 거주",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstateChip(
                label = "신규 주택 입력",
                selected = !plan.usesLinkedBuyEstate,
                onClick = {
                    onPlanChange(plan.copy(buyEstateId = ""))
                },
            )
            realEstates
                .filter { it.id != plan.sellEstateId && it.id != sellEstate?.id }
                .forEachIndexed { _, estate ->
                    val index = realEstates.indexOfFirst { it.id == estate.id }
                    EstateChip(
                        label = estate.displayTitle(estateOrdinal = index, estateCount = realEstates.size),
                        selected = plan.buyEstateId == estate.id,
                        onClick = {
                            onPlanChange(
                                plan.copy(
                                    buyEstateId = estate.id,
                                    newHomeValue = 0L,
                                    newHomeDebt = 0L,
                                ),
                            )
                        },
                    )
                }
            OutlinedButton(onClick = onAddBuyEstate) {
                Text("+ 이주 후 주택 추가")
            }
        }

        if (!plan.usesLinkedBuyEstate) {
            ManWonInputField(
                label = "신규 주택 시세",
                valueInWon = plan.newHomeValue,
                onValueChange = { value ->
                    val debt = plan.newHomeDebt.coerceAtMost(value)
                    onPlanChange(plan.copy(newHomeValue = value, newHomeDebt = debt))
                },
            )
            ManWonInputField(
                label = "신규 주택 대출",
                valueInWon = plan.newHomeDebt,
                onValueChange = { debt ->
                    onPlanChange(plan.copy(newHomeDebt = debt.coerceAtMost(plan.newHomeValue)))
                },
                supportingText = "필요 자기자본 = 시세 − 대출",
            )
            if (plan.newHomeValue > 0) {
                Text(
                    text = "신규 순자산: ${formatKoreanMan(plan.newHomeEquity)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val sellForPreset = sellEstate?.takeIf { it.currentValue > 0 }
            if (sellForPreset != null) {
                OutlinedButton(
                    onClick = { onPlanChange(plan.withDownsizingPreset(sellForPreset)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("다운사이징 프리셋 (매각가 60% → 신규 시세)")
                }
            }
        } else {
            plan.resolveBuyEstate(realEstates)?.let { buy ->
                val index = realEstates.indexOfFirst { it.id == buy.id }
                Text(
                    text = "연결: ${buy.displayTitle(estateOrdinal = index, estateCount = realEstates.size)} · 순자산 ${formatKoreanMan(buy.netEquity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "신규 주택 구입 시점",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimingChip(
                label = "매각 동시",
                selected = plan.purchaseTiming is PurchaseTiming.SameYearAsSale,
                onClick = { onPlanChange(plan.copy(purchaseTiming = PurchaseTiming.SameYearAsSale)) },
            )
            TimingChip(
                label = "매각 전 (2주택)",
                selected = plan.purchaseTiming is PurchaseTiming.BeforeSale,
                onClick = {
                    val years = (plan.purchaseTiming as? PurchaseTiming.BeforeSale)?.years ?: 1
                    onPlanChange(plan.copy(purchaseTiming = PurchaseTiming.BeforeSale(years)))
                },
            )
            TimingChip(
                label = "매각 후 (임대)",
                selected = plan.purchaseTiming is PurchaseTiming.AfterSale,
                onClick = {
                    val years = (plan.purchaseTiming as? PurchaseTiming.AfterSale)?.years ?: 1
                    onPlanChange(plan.copy(purchaseTiming = PurchaseTiming.AfterSale(years)))
                },
            )
        }

        when (val timing = plan.purchaseTiming) {
            is PurchaseTiming.BeforeSale, is PurchaseTiming.AfterSale -> {
                val years = when (timing) {
                    is PurchaseTiming.BeforeSale -> timing.years
                    is PurchaseTiming.AfterSale -> timing.years
                    else -> 1
                }
                IntInputField(
                    label = if (timing is PurchaseTiming.BeforeSale) "매각 N년 전 구입" else "매각 N년 후 구입",
                    value = years,
                    validRange = 1..5,
                    onValueChange = { count ->
                        val safe = count.coerceIn(1, 5)
                        val updated = when (timing) {
                            is PurchaseTiming.BeforeSale -> PurchaseTiming.BeforeSale(safe)
                            is PurchaseTiming.AfterSale -> PurchaseTiming.AfterSale(safe)
                            else -> timing
                        }
                        onPlanChange(plan.copy(purchaseTiming = updated))
                    },
                    onCommit = { count ->
                        val safe = count.coerceIn(1, 5)
                        val updated = when (timing) {
                            is PurchaseTiming.BeforeSale -> PurchaseTiming.BeforeSale(safe)
                            is PurchaseTiming.AfterSale -> PurchaseTiming.AfterSale(safe)
                            else -> timing
                        }
                        onPlanChange(plan.copy(purchaseTiming = updated))
                    },
                    supportingText = if (timing is PurchaseTiming.BeforeSale) {
                        "2주택 기간 — 기존 주택 양도세 비과세 상실 가능"
                    } else {
                        "무주택(임대) 기간 후 신규 주택 구입"
                    },
                )
            }
            else -> Unit
        }

        relocationTimelineSummary(plan, realEstates)?.let { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EstateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun TimingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

private fun relocationCardSummary(
    plan: RelocationPlan,
    estates: List<Asset.RealEstate>,
): String = when {
    !plan.enabled -> "비활성"
    plan.resolveSellEstate(estates)?.saleYear == null -> "매각 연도 미설정"
    !plan.usesLinkedBuyEstate && plan.newHomeValue <= 0 -> "이주 후 주택 미입력"
    else -> relocationTimelineSummary(plan, estates) ?: timingLabel(plan.purchaseTiming)
}

internal fun relocationTimelineSummary(
    plan: RelocationPlan,
    estates: List<Asset.RealEstate>,
): String? {
    if (!plan.enabled) return null
    val sell = plan.resolveSellEstate(estates) ?: return null
    val saleYear = sell.saleYear ?: return null
    val sellIndex = estates.indexOfFirst { it.id == sell.id }.coerceAtLeast(0)
    val sellLabel = sell.displayTitle(estateOrdinal = sellIndex, estateCount = estates.size)

    val purchaseYear = when (val timing = plan.purchaseTiming) {
        is PurchaseTiming.SameYearAsSale -> saleYear
        is PurchaseTiming.BeforeSale -> saleYear - timing.years
        is PurchaseTiming.AfterSale -> saleYear + timing.years
    }

    val buyLabel = when {
        plan.usesLinkedBuyEstate -> {
            val buy = plan.resolveBuyEstate(estates) ?: return null
            val buyIndex = estates.indexOfFirst { it.id == buy.id }.coerceAtLeast(0)
            buy.displayTitle(estateOrdinal = buyIndex, estateCount = estates.size)
        }
        plan.newHomeValue > 0 -> "신규 ${formatKoreanMan(plan.newHomeEquity)}"
        else -> return null
    }

    val timingNote = when (plan.purchaseTiming) {
        is PurchaseTiming.BeforeSale -> " · 2주택 ${plan.purchaseTiming.years}년"
        is PurchaseTiming.AfterSale -> " · 무주택 ${plan.purchaseTiming.years}년"
        else -> ""
    }

    return "${saleYear}년 $sellLabel 매각 → ${purchaseYear}년 $buyLabel 구입$timingNote"
}

private fun timingLabel(timing: PurchaseTiming): String = when (timing) {
    is PurchaseTiming.SameYearAsSale -> "매각 동시 구입"
    is PurchaseTiming.BeforeSale -> "매각 ${timing.years}년 전 (2주택)"
    is PurchaseTiming.AfterSale -> "매각 ${timing.years}년 후 구입"
}
