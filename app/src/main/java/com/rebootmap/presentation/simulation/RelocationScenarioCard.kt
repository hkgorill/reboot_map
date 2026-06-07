package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.rebootmap.domain.scenario.PurchaseTiming
import com.rebootmap.domain.scenario.RelocationPlan
import com.rebootmap.presentation.components.ExpandableCard
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.formatKoreanMan
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelocationScenarioCard(
    plan: RelocationPlan,
    saleYear: Int?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlanChange: (RelocationPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = when {
        !plan.enabled -> "비활성"
        plan.newHomeValue <= 0 -> "신규 주택 미입력"
        else -> "신규 ${formatKoreanMan(plan.newHomeEquity)} · ${timingLabel(plan.purchaseTiming)}"
    }

    ExpandableCard(
        title = "거주지 이동 시나리오",
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

        if (saleYear == null) {
            Text(
                text = "부동산 카드에서 매각 예정 연도를 먼저 설정하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                text = "기존 주택 매각 연도: ${saleYear}년",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
    }
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

private fun timingLabel(timing: PurchaseTiming): String = when (timing) {
    is PurchaseTiming.SameYearAsSale -> "매각 동시 구입"
    is PurchaseTiming.BeforeSale -> "매각 ${timing.years}년 전 (2주택)"
    is PurchaseTiming.AfterSale -> "매각 ${timing.years}년 후 구입"
}
