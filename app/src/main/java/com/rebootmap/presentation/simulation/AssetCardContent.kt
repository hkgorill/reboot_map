package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.RealEstateProjection
import com.rebootmap.domain.model.PensionDefaults
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.InvestmentReturnRate
import com.rebootmap.presentation.components.InvestmentReturnSlider
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.PercentInputField
import com.rebootmap.presentation.components.coerceIntPreservingZero
import com.rebootmap.presentation.components.coercePercentPreservingZero
import com.rebootmap.presentation.components.formatKoreanMan
import java.time.Year

private fun isCompleteRangedIncomeAge(age: Int): Boolean = age == 0 || age in 18..100

@Composable
private fun RangedIncomeFields(
    labelMonthly: String,
    monthlyHint: String,
    endAgeHint: String,
    monthlyAmount: Long,
    startAge: Int,
    endAge: Int,
    referenceMonthly: Long?,
    referenceStartAge: Int?,
    referenceEndAge: Int?,
    onMonthlyChange: (Long) -> Unit,
    onStartAgeChange: (Int) -> Unit,
    onEndAgeChange: (Int) -> Unit,
) {
    ManWonInputField(
        label = labelMonthly,
        valueInWon = monthlyAmount,
        onValueChange = onMonthlyChange,
        supportingText = PresetHints.withBase(
            monthlyHint,
            referenceMonthly?.let { PresetHints.manWon(it) },
        ),
    )
    IntInputField(
        label = "수입 시작 연령",
        value = startAge,
        validRange = 18..100,
        onValueChange = onStartAgeChange,
        onCommit = { age ->
            if (isCompleteRangedIncomeAge(age)) onStartAgeChange(age)
        },
        supportingText = referenceStartAge?.let { PresetHints.age(it) },
    )
    IntInputField(
        label = "수입 종료 연령",
        value = endAge,
        validRange = 18..100,
        onValueChange = onEndAgeChange,
        onCommit = { age ->
            if (isCompleteRangedIncomeAge(age)) onEndAgeChange(age)
        },
        supportingText = PresetHints.withBase(
            endAgeHint,
            referenceStartAge?.let { start ->
                referenceEndAge?.let { end -> PresetHints.ageRange(start, end) }
            },
        ),
    )
}

@Composable
fun AssetCardFields(
    asset: Asset,
    referenceAsset: Asset?,
    onAssetChange: (Asset) -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    when (asset) {
        is Asset.RealEstate -> {
            val ref = referenceAsset as? Asset.RealEstate
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "유형", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = asset.category == RealEstateCategory.PRIMARY_RESIDENCE,
                        onClick = {
                            onAssetChange(
                                asset.copy(
                                    category = RealEstateCategory.PRIMARY_RESIDENCE,
                                    isPrimaryResidence = true,
                                ),
                            )
                        },
                        label = { Text("거주 주택") },
                    )
                    FilterChip(
                        selected = asset.category == RealEstateCategory.NON_RESIDENTIAL,
                        onClick = {
                            onAssetChange(
                                asset.copy(
                                    category = RealEstateCategory.NON_RESIDENTIAL,
                                    isPrimaryResidence = false,
                                ),
                            )
                        },
                        label = { Text("비주택") },
                    )
                }
                Text(
                    text = "재산세: ${RealEstateCategory.PRIMARY_RESIDENCE.label()} 0.25% · ${RealEstateCategory.NON_RESIDENTIAL.label()} 0.4% (간이)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ManWonInputField(
                label = "현재 시세 (총 자산가치)",
                valueInWon = asset.currentValue,
                onValueChange = { value ->
                    val debt = asset.debtAmount.coerceAtMost(value)
                    onAssetChange(asset.copy(currentValue = value, debtAmount = debt))
                },
                supportingText = PresetHints.withBase(
                    "아파트·주택 시세 등 총 가치",
                    ref?.let { PresetHints.manWon(it.currentValue) },
                ),
            )
            ManWonInputField(
                label = "부채 (대출·보증금 등)",
                valueInWon = asset.debtAmount,
                onValueChange = { debt ->
                    onAssetChange(asset.copy(debtAmount = debt.coerceAtMost(asset.currentValue)))
                },
                supportingText = PresetHints.withBase(
                    "없으면 비워두세요 · 순자산 = 시세 − 부채",
                    ref?.let { PresetHints.manWon(it.debtAmount) },
                ),
            )
            if (asset.currentValue > 0) {
                Text(
                    text = "순자산: ${formatKoreanMan(asset.netEquity)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ManWonInputField(
                label = "취득가액 (양도세)",
                valueInWon = asset.acquisitionCost,
                onValueChange = { onAssetChange(asset.copy(acquisitionCost = it)) },
                supportingText = PresetHints.withBase(
                    "미입력 시 순자산 기준(차익 0) 가정",
                    ref?.let { PresetHints.manWon(it.acquisitionCost) },
                ),
            )
            IntInputField(
                label = "보유 연수",
                value = asset.holdingYears,
                validRange = 0..50,
                onValueChange = { onAssetChange(asset.copy(holdingYears = it)) },
                onCommit = { onAssetChange(asset.copy(holdingYears = coerceIntPreservingZero(it, 0..50))) },
                supportingText = PresetHints.withBase(
                    "2년 이상+1주택 시 비과세 검토",
                    ref?.let { PresetHints.age(it.holdingYears) },
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "1세대 1주택 (양도세)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = asset.isPrimaryResidence,
                    onCheckedChange = { checked ->
                        val category = if (checked) {
                            RealEstateCategory.PRIMARY_RESIDENCE
                        } else {
                            RealEstateCategory.NON_RESIDENTIAL
                        }
                        onAssetChange(asset.copy(isPrimaryResidence = checked, category = category))
                    },
                )
            }
            val currentYear = Year.now().value
            IntInputField(
                label = "매각 예정 연도",
                value = asset.saleYear ?: 0,
                validRange = currentYear..(currentYear + 50),
                onValueChange = { year ->
                    val saleYear = year.takeIf { it > currentYear }
                    onAssetChange(
                        asset.copy(
                            saleYear = saleYear,
                            expectedSalePrice = if (saleYear == null) 0L else asset.expectedSalePrice,
                        ),
                    )
                },
                supportingText = PresetHints.withBase(
                    "비워두면 매각 없음",
                    ref?.saleYear?.let { PresetHints.year(it) },
                ),
            )
            if (asset.saleYear != null && asset.saleYear > currentYear) {
                ManWonInputField(
                    label = "예상 매각 가격 (총 자산가치)",
                    valueInWon = asset.expectedSalePrice,
                    onValueChange = { price ->
                        onAssetChange(asset.copy(expectedSalePrice = price.coerceAtLeast(0L)))
                    },
                    supportingText = buildString {
                        append("미입력 시 현재 시세 유지")
                        if (asset.currentValue > 0 && asset.expectedSalePrice > 0) {
                            val rate = RealEstateProjection.annualRate(asset, currentYear)
                            append(" · 연평균 ${RealEstateProjection.formatAnnualRate(rate)}")
                        }
                    },
                )
                if (asset.currentValue > 0 && asset.expectedSalePrice > 0) {
                    val projectedNet = RealEstateProjection.projectedNetEquity(
                        asset,
                        asset.saleYear,
                        currentYear,
                    )
                    Text(
                        text = "매각 시 예상 순자산: ${formatKoreanMan(projectedNet)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            onRemove?.let { remove ->
                TextButton(onClick = remove) {
                    Text("부동산 삭제")
                }
            }
        }

        is Asset.NationalPension -> {
            val ref = referenceAsset as? Asset.NationalPension
            ManWonInputField(
                label = "예상 월 수령액",
                valueInWon = asset.monthlyPayout,
                onValueChange = { onAssetChange(asset.copy(monthlyPayout = it)) },
                supportingText = ref?.let { PresetHints.manWon(it.monthlyPayout) },
            )
            IntInputField(
                label = "수령 시작 연령",
                value = asset.startAge,
                validRange = 55..75,
                onValueChange = { onAssetChange(asset.copy(startAge = it)) },
                onCommit = { onAssetChange(asset.copy(startAge = coerceIntPreservingZero(it, 55..75))) },
                supportingText = ref?.let { PresetHints.age(it.startAge) },
            )
        }

        is Asset.SeverancePension -> {
            val ref = referenceAsset as? Asset.SeverancePension
            ManWonInputField(
                label = "현재 잔액",
                valueInWon = asset.balance,
                onValueChange = { onAssetChange(asset.copy(balance = it)) },
                supportingText = PresetHints.withBase(
                    "DC·DB·IRP 퇴직금 적립액",
                    ref?.let { PresetHints.manWon(it.balance) },
                ),
            )
            ManWonInputField(
                label = "월 납입액",
                valueInWon = asset.monthlyContribution,
                onValueChange = { onAssetChange(asset.copy(monthlyContribution = it)) },
                supportingText = PresetHints.withBase(
                    "회사·개인 부담 합산",
                    ref?.let { PresetHints.manWon(it.monthlyContribution) },
                ),
            )
            IntInputField(
                label = "납입 종료 연령",
                value = asset.contributionEndAge,
                validRange = 18..100,
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = {
                    onAssetChange(asset.copy(contributionEndAge = coerceIntPreservingZero(it, 18..100)))
                },
                supportingText = PresetHints.withBase(
                    "은퇴 시점 전후로 설정",
                    ref?.let { PresetHints.age(it.contributionEndAge) },
                ),
            )
            IntInputField(
                label = "연금 수령 개시 연령",
                value = asset.payoutStartAge,
                validRange = 55..70,
                onValueChange = { onAssetChange(asset.copy(payoutStartAge = it)) },
                onCommit = { onAssetChange(asset.copy(payoutStartAge = coerceIntPreservingZero(it, 55..70))) },
                supportingText = PresetHints.withBase(
                    "미입력 시 목표 은퇴 연령부터 균등 인출",
                    ref?.let { PresetHints.age(it.payoutStartAge) },
                ),
            )
            PercentInputField(
                label = "예상 연 운용 수익률 (%)",
                value = asset.annualReturnRate,
                validRange = -0.2..0.2,
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it)) },
                onCommit = {
                    onAssetChange(asset.copy(annualReturnRate = coercePercentPreservingZero(it, -0.2..0.2)))
                },
                supportingText = PresetHints.withBase(
                    "납입·대기·수령 중 잔액에 복리 적용 (물가상승과 별도)",
                    ref?.let { PresetHints.percent(it.annualReturnRate) },
                ),
            )
        }

        is Asset.PersonalPension -> {
            val ref = referenceAsset as? Asset.PersonalPension
            ManWonInputField(
                label = "현재 잔액",
                valueInWon = asset.balance,
                onValueChange = { onAssetChange(asset.copy(balance = it)) },
                supportingText = PresetHints.withBase(
                    "연금저축·개인 IRP 적립액",
                    ref?.let { PresetHints.manWon(it.balance) },
                ),
            )
            ManWonInputField(
                label = "월 납입액",
                valueInWon = asset.monthlyContribution,
                onValueChange = { onAssetChange(asset.copy(monthlyContribution = it)) },
                supportingText = PresetHints.withBase(
                    "연 600만원 한도 내 세액공제",
                    ref?.let { PresetHints.manWon(it.monthlyContribution) },
                ),
            )
            IntInputField(
                label = "납입 종료 연령",
                value = asset.contributionEndAge,
                validRange = 18..100,
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = {
                    onAssetChange(asset.copy(contributionEndAge = coerceIntPreservingZero(it, 18..100)))
                },
                supportingText = ref?.let { PresetHints.age(it.contributionEndAge) },
            )
            IntInputField(
                label = "연금 수령 개시 연령",
                value = asset.payoutStartAge,
                validRange = 55..70,
                onValueChange = { onAssetChange(asset.copy(payoutStartAge = it)) },
                onCommit = { onAssetChange(asset.copy(payoutStartAge = coerceIntPreservingZero(it, 55..70))) },
                supportingText = PresetHints.withBase(
                    "연금저축 최소 ${PensionDefaults.PERSONAL_MIN_PAYOUT_AGE}세",
                    ref?.let { PresetHints.age(it.payoutStartAge) },
                ),
            )
            PercentInputField(
                label = "예상 연 운용 수익률 (%)",
                value = asset.annualReturnRate,
                validRange = -0.2..0.2,
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it)) },
                onCommit = {
                    onAssetChange(asset.copy(annualReturnRate = coercePercentPreservingZero(it, -0.2..0.2)))
                },
                supportingText = PresetHints.withBase(
                    "납입·대기·수령 중 잔액에 복리 적용 (물가상승과 별도)",
                    ref?.let { PresetHints.percent(it.annualReturnRate) },
                ),
            )
        }

        is Asset.YellowUmbrella -> {
            val ref = referenceAsset as? Asset.YellowUmbrella
            ManWonInputField(
                label = "현재 공제부금 잔액",
                valueInWon = asset.balance,
                onValueChange = { onAssetChange(asset.copy(balance = it)) },
                supportingText = PresetHints.withBase(
                    "소기업·소상공인 공제 적립액",
                    ref?.let { PresetHints.manWon(it.balance) },
                ),
            )
            ManWonInputField(
                label = "월 공제부금",
                valueInWon = asset.monthlyContribution,
                onValueChange = { onAssetChange(asset.copy(monthlyContribution = it)) },
                supportingText = ref?.let { PresetHints.manWon(it.monthlyContribution) },
            )
            IntInputField(
                label = "납입 종료 연령",
                value = asset.contributionEndAge,
                validRange = 18..100,
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = {
                    onAssetChange(asset.copy(contributionEndAge = coerceIntPreservingZero(it, 18..100)))
                },
                supportingText = ref?.let { PresetHints.age(it.contributionEndAge) },
            )
            IntInputField(
                label = "일시금 수령 연령",
                value = asset.payoutAge,
                validRange = 55..70,
                onValueChange = { onAssetChange(asset.copy(payoutAge = it)) },
                onCommit = { onAssetChange(asset.copy(payoutAge = coerceIntPreservingZero(it, 55..70))) },
                supportingText = PresetHints.withBase(
                    "수령 시 잔액+공제이자 일시금 (불입액만이 아님)",
                    ref?.let { PresetHints.age(it.payoutAge) },
                ),
            )
            PercentInputField(
                label = "공제이자율 (%)",
                value = asset.annualReturnRate,
                validRange = 0.0..0.1,
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it)) },
                onCommit = {
                    onAssetChange(asset.copy(annualReturnRate = coercePercentPreservingZero(it, 0.0..0.1)))
                },
                supportingText = PresetHints.withBase(
                    "중기부 고시 공제이자 복리 · 물가상승과 무관",
                    ref?.let { PresetHints.percent(it.annualReturnRate) },
                ),
            )
        }

        is Asset.Investment -> {
            val ref = referenceAsset as? Asset.Investment
            ManWonInputField(
                label = "현재 평가 자산",
                valueInWon = asset.currentValue,
                onValueChange = { onAssetChange(asset.copy(currentValue = it)) },
                supportingText = ref?.let { PresetHints.manWon(it.currentValue) },
            )
            PercentInputField(
                label = "예상 연 수익률 (%)",
                value = asset.annualReturnRate,
                validRange = -0.5..1.0,
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it)) },
                onCommit = {
                    onAssetChange(asset.copy(annualReturnRate = coercePercentPreservingZero(it, -0.5..1.0)))
                },
                supportingText = ref?.let { PresetHints.percent(it.annualReturnRate) },
            )
            if (asset.currentValue > 0) {
                Text(
                    text = "수익률 빠른 조절: ${InvestmentReturnRate.formatPercent(asset.annualReturnRate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                InvestmentReturnSlider(
                    returnRate = asset.annualReturnRate,
                    onReturnRateChange = { rate ->
                        onAssetChange(asset.copy(annualReturnRate = InvestmentReturnRate.snap(rate)))
                    },
                )
            }
        }

        is Asset.CashSavings -> {
            val ref = referenceAsset as? Asset.CashSavings
            ManWonInputField(
                label = "만기 금액",
                valueInWon = asset.maturityAmount,
                onValueChange = { onAssetChange(asset.copy(maturityAmount = it)) },
                supportingText = ref?.let { PresetHints.manWon(it.maturityAmount) },
            )
            val currentYear = Year.now().value
            IntInputField(
                label = "만기 연도",
                value = asset.maturityYear,
                validRange = currentYear..(currentYear + 50),
                onValueChange = { onAssetChange(asset.copy(maturityYear = it)) },
                supportingText = ref?.let { PresetHints.year(it.maturityYear) },
            )
        }

        is Asset.HousingPension -> {
            val ref = referenceAsset as? Asset.HousingPension
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "주택연금 활성화", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = asset.enabled,
                    onCheckedChange = { onAssetChange(asset.copy(enabled = it)) },
                )
            }
            IntInputField(
                label = "수령 개시 연령",
                value = asset.startAge,
                validRange = 55..80,
                onValueChange = { onAssetChange(asset.copy(startAge = it)) },
                onCommit = { onAssetChange(asset.copy(startAge = coerceIntPreservingZero(it, 55..80))) },
                supportingText = PresetHints.withBase(
                    "55세 이상, 주택 담보 역모기지",
                    ref?.let { PresetHints.age(it.startAge) },
                ),
            )
            ManWonInputField(
                label = "담보 주택 가치 (선택)",
                valueInWon = asset.homeEquityOverride,
                onValueChange = { onAssetChange(asset.copy(homeEquityOverride = it)) },
                supportingText = "미입력 시 부동산 순자산 사용",
            )
        }

        is Asset.EmploymentIncome -> {
            val ref = referenceAsset as? Asset.EmploymentIncome
            RangedIncomeFields(
                labelMonthly = "월 직장 소득",
                monthlyHint = "급여·상여 근사 (세전)",
                endAgeHint = "보통 목표 은퇴 연령까지",
                monthlyAmount = asset.monthlyAmount,
                startAge = asset.startAge,
                endAge = asset.endAge,
                referenceMonthly = ref?.monthlyAmount,
                referenceStartAge = ref?.startAge,
                referenceEndAge = ref?.endAge,
                onMonthlyChange = { onAssetChange(asset.copy(monthlyAmount = it)) },
                onStartAgeChange = { onAssetChange(asset.copy(startAge = it)) },
                onEndAgeChange = { onAssetChange(asset.copy(endAge = it)) },
            )
        }

        is Asset.BusinessIncome -> {
            val ref = referenceAsset as? Asset.BusinessIncome
            RangedIncomeFields(
                labelMonthly = "월 사업 소득",
                monthlyHint = "프리랜서·자영업 순수익 근사",
                endAgeHint = "사업 종료 또는 은퇴 연령",
                monthlyAmount = asset.monthlyAmount,
                startAge = asset.startAge,
                endAge = asset.endAge,
                referenceMonthly = ref?.monthlyAmount,
                referenceStartAge = ref?.startAge,
                referenceEndAge = ref?.endAge,
                onMonthlyChange = { onAssetChange(asset.copy(monthlyAmount = it)) },
                onStartAgeChange = { onAssetChange(asset.copy(startAge = it)) },
                onEndAgeChange = { onAssetChange(asset.copy(endAge = it)) },
            )
        }

        is Asset.OtherFixedIncome -> {
            val ref = referenceAsset as? Asset.OtherFixedIncome
            RangedIncomeFields(
                labelMonthly = "월 기타 수입",
                monthlyHint = "임대료·퇴직 후 아르바이트 등",
                endAgeHint = "임대료는 기대 수명까지",
                monthlyAmount = asset.monthlyAmount,
                startAge = asset.startAge,
                endAge = asset.endAge,
                referenceMonthly = ref?.monthlyAmount,
                referenceStartAge = ref?.startAge,
                referenceEndAge = ref?.endAge,
                onMonthlyChange = { onAssetChange(asset.copy(monthlyAmount = it)) },
                onStartAgeChange = { onAssetChange(asset.copy(startAge = it)) },
                onEndAgeChange = { onAssetChange(asset.copy(endAge = it)) },
            )
        }
    }
}

private fun rangedIncomeSummary(monthlyAmount: Long, startAge: Int, endAge: Int): String =
    if (monthlyAmount > 0) {
        if (startAge > 0 && endAge > 0) "월 ${formatKoreanMan(monthlyAmount)} · ${startAge}~${endAge}세"
        else "월 ${formatKoreanMan(monthlyAmount)}"
    } else {
        "미입력"
    }

fun Asset.summaryText(): String = when (this) {
    is Asset.RealEstate -> {
        if (currentValue <= 0 && debtAmount <= 0) {
            "미입력"
        } else {
            val typeLabel = if (category == RealEstateCategory.NON_RESIDENTIAL) " · 비주택" else ""
            val base = "순자산 ${formatKoreanMan(netEquity)}$typeLabel"
            val currentYear = java.time.Year.now().value
            if (saleYear != null && saleYear > currentYear && expectedSalePrice > 0 && currentValue > 0) {
                val rate = RealEstateProjection.formatAnnualRate(
                    RealEstateProjection.annualRate(this, currentYear),
                )
                "$base · 매각 $rate"
            } else {
                base
            }
        }
    }
    is Asset.NationalPension -> if (monthlyPayout > 0) "월 ${formatKoreanMan(monthlyPayout)}" else "미입력"
    is Asset.SeverancePension -> if (balance > 0 || monthlyContribution > 0) {
        if (payoutStartAge > 0) "${formatKoreanMan(balance)} · ${payoutStartAge}세 수령" else formatKoreanMan(balance)
    } else {
        "미입력"
    }
    is Asset.PersonalPension -> if (balance > 0 || monthlyContribution > 0) {
        if (payoutStartAge > 0) "${formatKoreanMan(balance)} · ${payoutStartAge}세 수령" else formatKoreanMan(balance)
    } else {
        "미입력"
    }
    is Asset.YellowUmbrella -> if (balance > 0 || monthlyContribution > 0) {
        if (payoutAge > 0) "${formatKoreanMan(balance)} · ${payoutAge}세 일시금" else formatKoreanMan(balance)
    } else {
        "미입력"
    }
    is Asset.Investment -> if (currentValue > 0) {
        if (annualReturnRate > 0) {
            "${formatKoreanMan(currentValue)} · ${InvestmentReturnRate.formatPercent(annualReturnRate)}"
        } else {
            formatKoreanMan(currentValue)
        }
    } else {
        "미입력"
    }
    is Asset.CashSavings -> if (maturityAmount > 0) {
        if (maturityYear > 0) "${maturityYear}년 ${formatKoreanMan(maturityAmount)}" else formatKoreanMan(maturityAmount)
    } else {
        "미입력"
    }
    is Asset.EmploymentIncome -> rangedIncomeSummary(monthlyAmount, startAge, endAge)
    is Asset.BusinessIncome -> rangedIncomeSummary(monthlyAmount, startAge, endAge)
    is Asset.OtherFixedIncome -> rangedIncomeSummary(monthlyAmount, startAge, endAge)
    is Asset.HousingPension -> if (enabled && startAge > 0) "${startAge}세 개시" else "미입력"
}

fun Asset.displayTitle(estateOrdinal: Int = 0, estateCount: Int = 1): String = when (this) {
    is Asset.RealEstate -> if (estateCount > 1) "부동산 ${estateOrdinal + 1}" else "부동산"
    is Asset.NationalPension -> "국민연금"
    is Asset.SeverancePension -> "퇴직연금"
    is Asset.PersonalPension -> "개인연금"
    is Asset.YellowUmbrella -> "노랑우산공제"
    is Asset.Investment -> "주식·재테크"
    is Asset.CashSavings -> "현금·적금"
    is Asset.EmploymentIncome -> "직장 소득"
    is Asset.BusinessIncome -> "사업 소득"
    is Asset.OtherFixedIncome -> "기타 고정수입"
    is Asset.HousingPension -> "주택연금"
}
