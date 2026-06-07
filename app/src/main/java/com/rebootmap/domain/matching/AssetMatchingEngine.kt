package com.rebootmap.domain.matching

import com.rebootmap.domain.milestone.LumpSumExpense
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateProjection
import kotlin.math.abs

data class AssetSuggestion(
    val assetId: String,
    val assetLabel: String,
    val availableAmount: Long,
    val timingNote: String,
    val matchScore: Int,
)

object AssetMatchingEngine {

    fun recommend(
        expense: LumpSumExpense,
        assets: List<Asset>,
        startYear: Int,
        currentAge: Int,
    ): List<AssetSuggestion> {
        val suggestions = mutableListOf<AssetSuggestion>()

        assets.forEach { asset ->
            when (asset) {
                is Asset.CashSavings -> {
                    if (asset.maturityAmount <= 0 || asset.maturityYear <= 0) return@forEach
                    val yearDiff = abs(asset.maturityYear - expense.year)
                    val score = when {
                        asset.maturityYear == expense.year -> 100
                        yearDiff == 1 -> 70
                        yearDiff <= 2 -> 40
                        else -> 10
                    }
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "현금·적금",
                        availableAmount = asset.maturityAmount,
                        timingNote = "${asset.maturityYear}년 만기",
                        matchScore = score,
                    )
                }
                is Asset.RealEstate -> {
                    val saleYear = asset.saleYear ?: return@forEach
                    val projectedNet = RealEstateProjection.projectedNetEquity(asset, saleYear, startYear)
                    if (projectedNet <= 0) return@forEach
                    val yearDiff = abs(saleYear - expense.year)
                    val score = when {
                        saleYear == expense.year -> 95
                        yearDiff == 1 -> 65
                        yearDiff <= 2 -> 35
                        else -> 5
                    }
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "부동산 매각",
                        availableAmount = projectedNet,
                        timingNote = "${saleYear}년 매각 예정",
                        matchScore = score,
                    )
                }
                is Asset.Investment -> {
                    if (asset.currentValue <= 0) return@forEach
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "투자 자산",
                        availableAmount = asset.currentValue,
                        timingNote = "즉시 인출 가능",
                        matchScore = 60,
                    )
                }
                is Asset.SeverancePension -> {
                    if (asset.balance <= 0) return@forEach
                    val expenseAge = currentAge + (expense.year - startYear)
                    if (asset.contributionEndAge > 0 && expenseAge < asset.contributionEndAge) return@forEach
                    if (asset.payoutStartAge > 0 && expenseAge < asset.payoutStartAge) return@forEach
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "퇴직연금",
                        availableAmount = asset.balance,
                        timingNote = "적립 잔액 인출",
                        matchScore = 45,
                    )
                }
                is Asset.PersonalPension -> {
                    if (asset.balance <= 0) return@forEach
                    val expenseAge = currentAge + (expense.year - startYear)
                    if (asset.payoutStartAge > 0 && expenseAge < asset.payoutStartAge) return@forEach
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "개인연금",
                        availableAmount = asset.balance,
                        timingNote = "적립 잔액 인출",
                        matchScore = 45,
                    )
                }
                is Asset.YellowUmbrella -> {
                    if (asset.balance <= 0) return@forEach
                    val expenseAge = currentAge + (expense.year - startYear)
                    if (asset.payoutAge > 0 && expenseAge < asset.payoutAge) return@forEach
                    suggestions += AssetSuggestion(
                        assetId = asset.id,
                        assetLabel = "노랑우산공제",
                        availableAmount = asset.balance,
                        timingNote = "공제금 수령 후",
                        matchScore = 40,
                    )
                }
                else -> Unit
            }
        }

        return suggestions
            .sortedWith(
                compareByDescending<AssetSuggestion> { it.matchScore }
                    .thenByDescending { it.availableAmount >= expense.amount }
                    .thenByDescending { it.availableAmount },
            )
            .take(3)
    }
}
