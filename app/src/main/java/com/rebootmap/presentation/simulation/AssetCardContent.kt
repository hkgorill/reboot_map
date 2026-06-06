package com.rebootmap.presentation.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.PensionDefaults
import com.rebootmap.presentation.components.IntInputField
import com.rebootmap.presentation.components.InvestmentReturnRate
import com.rebootmap.presentation.components.ManWonInputField
import com.rebootmap.presentation.components.PercentInputField
import com.rebootmap.presentation.components.formatKoreanMan
import java.time.Year

@Composable
fun AssetCardFields(
    asset: Asset,
    referenceAsset: Asset?,
    onAssetChange: (Asset) -> Unit,
) {
    when (asset) {
        is Asset.RealEstate -> {
            val ref = referenceAsset as? Asset.RealEstate
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
                onValueChange = { onAssetChange(asset.copy(holdingYears = it)) },
                onCommit = { onAssetChange(asset.copy(holdingYears = it.coerceIn(0, 50))) },
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
                Text(text = "1세대 1주택", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = asset.isPrimaryResidence,
                    onCheckedChange = { onAssetChange(asset.copy(isPrimaryResidence = it)) },
                )
            }
            IntInputField(
                label = "매각 예정 연도",
                value = asset.saleYear ?: 0,
                onValueChange = { year ->
                    onAssetChange(asset.copy(saleYear = year.takeIf { it > Year.now().value }))
                },
                supportingText = PresetHints.withBase(
                    "비워두면 매각 없음",
                    ref?.saleYear?.let { PresetHints.year(it) },
                ),
            )
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
                onValueChange = { onAssetChange(asset.copy(startAge = it)) },
                onCommit = { onAssetChange(asset.copy(startAge = it.coerceIn(55, 75))) },
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
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = { onAssetChange(asset.copy(contributionEndAge = it.coerceIn(18, 100))) },
                supportingText = PresetHints.withBase(
                    "은퇴 시점 전후로 설정",
                    ref?.let { PresetHints.age(it.contributionEndAge) },
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
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = { onAssetChange(asset.copy(contributionEndAge = it.coerceIn(18, 100))) },
                supportingText = ref?.let { PresetHints.age(it.contributionEndAge) },
            )
            IntInputField(
                label = "연금 수령 개시 연령",
                value = asset.payoutStartAge,
                onValueChange = { onAssetChange(asset.copy(payoutStartAge = it)) },
                onCommit = { onAssetChange(asset.copy(payoutStartAge = it.coerceIn(55, 70))) },
                supportingText = PresetHints.withBase(
                    "연금저축 최소 ${PensionDefaults.PERSONAL_MIN_PAYOUT_AGE}세",
                    ref?.let { PresetHints.age(it.payoutStartAge) },
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
                onValueChange = { onAssetChange(asset.copy(contributionEndAge = it)) },
                onCommit = { onAssetChange(asset.copy(contributionEndAge = it.coerceIn(18, 100))) },
                supportingText = ref?.let { PresetHints.age(it.contributionEndAge) },
            )
            IntInputField(
                label = "일시금 수령 연령",
                value = asset.payoutAge,
                onValueChange = { onAssetChange(asset.copy(payoutAge = it)) },
                onCommit = { onAssetChange(asset.copy(payoutAge = it.coerceIn(55, 70))) },
                supportingText = PresetHints.withBase(
                    "공제이자 ${(PensionDefaults.YELLOW_UMBRELLA_RETURN_RATE * 100).let { if (it % 1.0 == 0.0) it.toInt() else "%.1f".format(it) }}% 복리 적용",
                    ref?.let { PresetHints.age(it.payoutAge) },
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
                onValueChange = { onAssetChange(asset.copy(annualReturnRate = it)) },
                onCommit = { onAssetChange(asset.copy(annualReturnRate = it.coerceIn(-0.5, 1.0))) },
                supportingText = ref?.let { PresetHints.percent(it.annualReturnRate) },
            )
        }

        is Asset.CashSavings -> {
            val ref = referenceAsset as? Asset.CashSavings
            ManWonInputField(
                label = "만기 금액",
                valueInWon = asset.maturityAmount,
                onValueChange = { onAssetChange(asset.copy(maturityAmount = it)) },
                supportingText = ref?.let { PresetHints.manWon(it.maturityAmount) },
            )
            IntInputField(
                label = "만기 연도",
                value = asset.maturityYear,
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
                onValueChange = { onAssetChange(asset.copy(startAge = it)) },
                onCommit = { onAssetChange(asset.copy(startAge = it.coerceIn(55, 80))) },
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

        is Asset.FixedIncome -> {
            val ref = referenceAsset as? Asset.FixedIncome
            ManWonInputField(
                label = "월 고정수입",
                valueInWon = asset.monthlyAmount,
                onValueChange = { onAssetChange(asset.copy(monthlyAmount = it)) },
                supportingText = PresetHints.withBase(
                    "임대료·급여·퇴직 후 근로소득 등",
                    ref?.let { PresetHints.manWon(it.monthlyAmount) },
                ),
            )
            IntInputField(
                label = "수입 시작 연령",
                value = asset.startAge,
                onValueChange = { onAssetChange(asset.copy(startAge = it)) },
                onCommit = { onAssetChange(asset.copy(startAge = it.coerceIn(18, 100))) },
                supportingText = ref?.let { PresetHints.age(it.startAge) },
            )
            IntInputField(
                label = "수입 종료 연령",
                value = asset.endAge,
                onValueChange = { onAssetChange(asset.copy(endAge = it)) },
                onCommit = { onAssetChange(asset.copy(endAge = it.coerceIn(18, 100))) },
                supportingText = PresetHints.withBase(
                    "월급=은퇴 연령 · 임대료=기대 수명",
                    ref?.let { PresetHints.ageRange(it.startAge, it.endAge) },
                ),
            )
        }
    }
}

fun Asset.summaryText(): String = when (this) {
    is Asset.RealEstate -> if (currentValue > 0 || debtAmount > 0) "순자산 ${formatKoreanMan(netEquity)}" else "미입력"
    is Asset.NationalPension -> if (monthlyPayout > 0) "월 ${formatKoreanMan(monthlyPayout)}" else "미입력"
    is Asset.SeverancePension -> if (balance > 0 || monthlyContribution > 0) formatKoreanMan(balance) else "미입력"
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
    is Asset.FixedIncome -> if (monthlyAmount > 0) {
        if (startAge > 0 && endAge > 0) "월 ${formatKoreanMan(monthlyAmount)} · ${startAge}~${endAge}세"
        else "월 ${formatKoreanMan(monthlyAmount)}"
    } else {
        "미입력"
    }
    is Asset.HousingPension -> if (enabled && startAge > 0) "${startAge}세 개시" else "미입력"
}

fun Asset.displayTitle(): String = when (this) {
    is Asset.RealEstate -> "부동산"
    is Asset.NationalPension -> "국민연금"
    is Asset.SeverancePension -> "퇴직연금"
    is Asset.PersonalPension -> "개인연금"
    is Asset.YellowUmbrella -> "노랑우산공제"
    is Asset.Investment -> "주식·재테크"
    is Asset.CashSavings -> "현금·적금"
    is Asset.FixedIncome -> "고정수입"
    is Asset.HousingPension -> "주택연금"
}
