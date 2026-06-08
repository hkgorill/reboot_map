package com.rebootmap.domain.advisory

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.UserProfile
import kotlin.math.roundToInt

object AssetAdvisoryEngine {

    fun evaluate(
        projection: CashFlowProjection?,
        profile: UserProfile,
        assets: List<Asset>,
        personalLoans: List<PersonalLoan> = emptyList(),
    ): AssetAdvisoryReport {
        if (projection == null || projection.yearlySnapshots.isEmpty()) {
            return AssetAdvisoryReport(
                score = 0,
                gradeLabel = "미산정",
                headline = "입력을 완료해 주세요",
                summary = "자산·연금·생활비 입력 후 총평이 생성됩니다.",
                strengths = emptyList(),
                weaknesses = emptyList(),
                watchPoints = listOf("온보딩과 자산 카드 입력을 마친 뒤 다시 확인하세요."),
            )
        }

        var score = 70
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val watchPoints = mutableListOf<String>()

        val snapshots = projection.yearlySnapshots
        val startAge = snapshots.first().age
        val retiredYears = snapshots.count { it.age >= profile.retirementAge }.coerceAtLeast(1)
        val deficitRatio = projection.deficitYears.size.toDouble() / retiredYears
        val finalBalance = projection.finalBalance
        val hasDepletion = projection.depletionYear != null

        if (!hasDepletion) {
            score += 20
            strengths += "기대 수명까지 순자산이 0 이하로 떨어지지 않는 시나리오입니다."
        } else {
            val depletionAge = snapshots.firstOrNull { it.year == projection.depletionYear }?.age
            val yearsAfterRetirement = depletionAge?.minus(profile.retirementAge) ?: 0
            score -= when {
                yearsAfterRetirement < 5 -> 35
                yearsAfterRetirement < 10 -> 25
                yearsAfterRetirement < 15 -> 15
                else -> 8
            }
            weaknesses += "자산 고갈이 ${projection.depletionYear}년(약 ${depletionAge}세)에 예상됩니다."
            watchPoints += "은퇴 후 ${yearsAfterRetirement.coerceAtLeast(0)}년 만에 고갈 — 생활비·연금 수령 시점 재검토가 필요합니다."
        }

        if (finalBalance < 0) {
            score -= 15
            weaknesses += "기대 수명 시점에 적자 누적(예상 부채)이 남습니다."
        } else if (finalBalance > profile.monthlyLivingExpense * 12 * 5) {
            score += 5
            strengths += "기대 수명 시점에도 ${formatManBrief(finalBalance)} 상당의 여유가 남습니다."
        }

        when {
            deficitRatio <= 0.0 -> strengths += "은퇴 후 매년 정기 수입이 생활비·세금을 덮는 구간입니다."
            deficitRatio < 0.3 -> {
                score -= 5
                watchPoints += "은퇴 후 일부 연도에 수입 부족이 있습니다. (${projection.deficitYears.size}년)"
            }
            deficitRatio < 0.6 -> {
                score -= 12
                weaknesses += "은퇴 기간의 ${(deficitRatio * 100).roundToInt()}%에서 수입이 생활비·세금보다 부족합니다."
            }
            else -> {
                score -= 20
                weaknesses += "은퇴 후 대부분의 기간에서 수입 부족이 지속됩니다."
            }
        }

        val national = assets.filterIsInstance<Asset.NationalPension>().firstOrNull()
        if (national == null || national.monthlyPayout <= 0 || national.startAge <= 0) {
            score -= 5
            watchPoints += "국민연금 수령액·개시 연령이 비어 있으면 보수적으로 잡힐 수 있습니다."
        } else {
            strengths += "국민연금 ${national.startAge}세 개시가 반영되어 있습니다."
        }

        val liquidTypes = assets.count {
            it is Asset.Investment || it is Asset.SeverancePension ||
                it is Asset.PersonalPension || it is Asset.CashSavings
        }
        if (liquidTypes >= 2) {
            strengths += "투자·연금·적금 등 유동 자원이 복수로 입력되어 있습니다."
        } else {
            watchPoints += "유동 자산(투자·연금·적금)이 한두 종류뿐이면 리스크가 집중될 수 있습니다."
        }

        val loanBalance = personalLoans.filter { it.balance > 0 }.sumOf { it.balance }
        val firstAssets = snapshots.first().totalAssets + loanBalance
        if (loanBalance > 0) {
            val ratio = loanBalance.toDouble() / firstAssets.coerceAtLeast(1L)
            if (ratio > 0.3) {
                score -= 10
                weaknesses += "신용·차용 부채가 초기 순자산의 ${(ratio * 100).roundToInt()}%를 차지합니다."
            } else {
                watchPoints += "개인 부채 상환액이 은퇴 후 현금흐름을 줄일 수 있습니다."
            }
        }

        val realEstates = assets.filterIsInstance<Asset.RealEstate>()
        if (realEstates.size > 1) {
            watchPoints += "복수 부동산 보유 시 매각 순서·2주택 구간이 현금흐름에 큰 영향을 줍니다."
        }

        if (profile.lifeExpectancy <= profile.retirementAge + 10) {
            watchPoints += "기대 수명이 짧게 잡혀 있으면 결과가 낙관적으로 보일 수 있습니다."
        }

        val clamped = score.coerceIn(0, 100)
        val grade = gradeFor(clamped)
        val headline = when (grade) {
            "양호" -> "전반적으로 안정적인 노후 시나리오"
            "보통" -> "유지 가능하나 일부 구간 점검 필요"
            "주의" -> "은퇴 후 특정 시점부터 부담 증가 예상"
            else -> "구조적 개선이 필요한 시나리오"
        }
        val summary = buildString {
            append("종합 ${clamped}점($grade). ")
            append(if (hasDepletion) "자산 고갈 전에 " else "")
            append("정기 수입·일시 유입·세금을 반영한 교육용 시뮬입니다. ")
            append("실제 세무·투자 결정은 전문가 상담을 권합니다.")
        }

        return AssetAdvisoryReport(
            score = clamped,
            gradeLabel = grade,
            headline = headline,
            summary = summary,
            strengths = strengths.distinct(),
            weaknesses = weaknesses.distinct(),
            watchPoints = watchPoints.distinct(),
        )
    }

    private fun gradeFor(score: Int): String = when {
        score >= 80 -> "양호"
        score >= 60 -> "보통"
        score >= 40 -> "주의"
        else -> "위험"
    }

    private fun formatManBrief(won: Long): String {
        val man = won / 10_000
        return when {
            man >= 10_000 -> "${man / 10_000}억 ${man % 10_000}만원"
            man >= 1 -> "${man}만원"
            else -> "${won}원"
        }
    }
}
