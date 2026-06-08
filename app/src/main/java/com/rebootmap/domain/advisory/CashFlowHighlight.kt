package com.rebootmap.domain.advisory

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.model.YearSnapshot

/** 월표·PDF에 표시할 대표 연도 */
data class CashFlowHighlight(
    val snapshot: YearSnapshot,
    val labels: List<String>,
)

object CashFlowHighlightPlanner {

    fun highlights(
        projection: CashFlowProjection,
        profile: UserProfile,
        assets: List<Asset> = emptyList(),
    ): List<CashFlowHighlight> {
        val snapshots = projection.yearlySnapshots
        if (snapshots.isEmpty()) return emptyList()

        val pensionStartAge = assets
            .filterIsInstance<Asset.NationalPension>()
            .firstOrNull { it.monthlyPayout > 0 && it.startAge > 0 }
            ?.startAge

        val byAge = linkedMapOf<Int, LinkedHashSet<String>>()

        fun tag(age: Int, label: String) {
            byAge.getOrPut(age) { linkedSetOf() }.add(label)
        }

        snapshots.firstOrNull()?.let { tag(it.age, "현재") }

        snapshots.firstOrNull { it.age >= profile.retirementAge }?.let {
            tag(it.age, "은퇴 첫해")
        }

        pensionStartAge?.let { startAge ->
            snapshots.firstOrNull { it.age == startAge && it.incomeBreakdown.nationalPension > 0 }
                ?.let { tag(it.age, "국민연금 개시") }
        }

        projection.deficitYears
            .mapNotNull { year -> snapshots.firstOrNull { it.year == year } }
            .minByOrNull { it.age }
            ?.let { tag(it.age, "첫 수입 부족") }

        snapshots.filter { it.incomeBreakdown.lumpSumTotal > 0 }.forEach { snap ->
            val detail = buildList {
                if (snap.incomeBreakdown.realEstateSale > 0) add("부동산 매각")
                if (snap.incomeBreakdown.cashSavingsMaturity > 0) add("만기 유입")
                if (snap.incomeBreakdown.yellowUmbrellaPayout > 0) add("노랑우산")
            }.joinToString("·")
            tag(snap.age, "일시 유입($detail)")
        }

        projection.depletionYear?.let { year ->
            snapshots.firstOrNull { it.year == year }?.let {
                tag(it.age, "자산 고갈 예상")
            }
        }

        snapshots.lastOrNull()?.takeIf { it.age != snapshots.first().age }?.let {
            tag(it.age, "기대 수명")
        }

        return byAge.entries.mapNotNull { (age, labels) ->
            snapshots.firstOrNull { it.age == age }?.let { CashFlowHighlight(it, labels.toList()) }
        }
    }

    fun postRetirementSnapshots(
        projection: CashFlowProjection,
        retirementAge: Int,
    ): List<YearSnapshot> = projection.yearlySnapshots.filter { it.age >= retirementAge }
}
