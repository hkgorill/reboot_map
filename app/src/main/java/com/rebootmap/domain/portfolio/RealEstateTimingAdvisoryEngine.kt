package com.rebootmap.domain.portfolio

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateProjection
import com.rebootmap.domain.model.TaxDefaults
import com.rebootmap.domain.tax.AcquisitionTaxEngine
import com.rebootmap.domain.tax.BrokerageFeeEngine
import com.rebootmap.domain.tax.CapitalGainsTaxEngine
/** 부동산 보유·매각 타이밍 컨설팅 (교육용, 간이 세법) */
data class RealEstateTimingReport(
    val headline: String,
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val watchPoints: List<String>,
    val overlapYears: List<Int>,
    val suggestedSaleYears: Map<String, Int>,
)

object RealEstateTimingAdvisoryEngine {

    fun evaluate(
        estates: List<Asset.RealEstate>,
        startYear: Int,
        horizonYears: Int = 20,
    ): RealEstateTimingReport {
        val configured = estates.filter { it.currentValue > 0 || it.debtAmount > 0 }
        if (configured.isEmpty()) {
            return RealEstateTimingReport(
                headline = "부동산 입력 후 컨설팅이 생성됩니다",
                summary = "시세·취득·매각 연도를 입력해 주세요.",
                strengths = emptyList(),
                weaknesses = emptyList(),
                watchPoints = listOf("부동산 카드에서 순자산과 연도를 먼저 입력하세요."),
                overlapYears = emptyList(),
                suggestedSaleYears = emptyMap(),
            )
        }

        val endYear = startYear + horizonYears
        val overlap = RealEstatePortfolioEngine.overlapYears(configured, startYear, endYear)
        val suggested = buildSuggestedSaleYears(configured, startYear)
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val watchPoints = mutableListOf<String>()

        if (overlap.isEmpty()) {
            strengths += "향후 ${horizonYears}년 내 2주택 겹침 구간이 없습니다(입력 기준)."
        } else {
            weaknesses += "2주택 겹침 ${overlap.size}년 — 종부세·양도세 부담이 커질 수 있습니다."
            watchPoints += "겹침 구간: ${overlap.take(5).joinToString(", ")}${if (overlap.size > 5) "…" else ""}년"
        }

        configured.forEachIndexed { index, estate ->
            val label = estateLabel(index, configured.size)
            val saleYear = estate.saleYear ?: return@forEachIndexed
            val others = RealEstatePortfolioEngine.otherHomesAtSale(
                estate,
                configured,
                saleYear,
                startYear,
                emptySet(),
            )
            if (others == 0 && estate.isPrimaryResidence) {
                strengths += "$label ${saleYear}년 매각 — 1주택 비과세 검토(간이)."
            }
            if (RealEstatePortfolioEngine.qualifiesTemporaryTwoHomeExemption(
                    estate,
                    configured,
                    saleYear,
                    startYear,
                    emptySet(),
                )
            ) {
                strengths += "$label — 일시적 1가구 2주택 비과세 구간(간이)."
            }
            if (others > 0 && !RealEstatePortfolioEngine.qualifiesTemporaryTwoHomeExemption(
                    estate,
                    configured,
                    saleYear,
                    startYear,
                    emptySet(),
                )
            ) {
                watchPoints += "$label 매각 시 다주택 과세·중과 가능(간이)."
            }
        }

        configured.forEachIndexed { index, estate ->
            val acq = estate.acquisitionYear ?: return@forEachIndexed
            if (acq <= startYear) return@forEachIndexed
            val label = estateLabel(index, configured.size)
            val othersAtBuy = configured.count { other ->
                other.id != estate.id &&
                    RealEstatePortfolioEngine.isOwned(other, acq - 1, startYear, emptySet())
            }
            if (othersAtBuy > 0) {
                watchPoints += "$label ${acq}년 취득 — 다주택 취득세 가중(간이)."
            }
        }

        val headline = when {
            overlap.isEmpty() -> "겹침 없음 — 비교적 단순한 포트폴리오"
            overlap.size <= 2 -> "짧은 2주택 구간 — 매각 시점 조정 여지"
            else -> "2주택 구간이 깁니다 — 매각·취득 순서 재검토 권장"
        }

        return RealEstateTimingReport(
            headline = headline,
            summary = "취득세·중개료·양도세·일시적 1가구2주택(3년) 반영 간이 시뮬. 실제 신고는 전문가 상담을 권합니다.",
            strengths = strengths.distinct(),
            weaknesses = weaknesses.distinct(),
            watchPoints = watchPoints.distinct(),
            overlapYears = overlap,
            suggestedSaleYears = suggested,
        )
    }

    fun estimateTransactionTaxWon(
        estates: List<Asset.RealEstate>,
        startYear: Int,
    ): Long {
        var total = 0L
        estates.forEach { estate ->
            val saleYear = estate.saleYear ?: return@forEach
            val gross = RealEstateProjection.projectedGrossValue(estate, saleYear, startYear)
            val net = RealEstateProjection.projectedNetEquity(estate, saleYear, startYear)
            val others = RealEstatePortfolioEngine.otherHomesAtSale(
                estate,
                estates,
                saleYear,
                startYear,
                emptySet(),
            )
            val temp = RealEstatePortfolioEngine.qualifiesTemporaryTwoHomeExemption(
                estate,
                estates,
                saleYear,
                startYear,
                emptySet(),
            )
            total += CapitalGainsTaxEngine.calculate(
                CapitalGainsTaxEngine.Input(
                    salePrice = gross.coerceAtLeast(net),
                    acquisitionCost = estate.effectiveAcquisitionCost,
                    holdingYears = RealEstatePortfolioEngine.holdingYearsAtSale(
                        estate,
                        saleYear,
                        startYear,
                    ),
                    isPrimaryResidence = estate.isPrimaryResidence,
                    otherHomesAtSale = others,
                    temporaryTwoHomeExempt = temp,
                ),
            ).tax
            total += BrokerageFeeEngine.calculate(gross, BrokerageFeeEngine.Side.SALE)
            estate.acquisitionYear?.let { acq ->
                val price = estate.currentValue
                val othersAtBuy = RealEstatePortfolioEngine.homeCount(
                    estates,
                    acq - 1,
                    startYear,
                    emptySet(),
                )
                total += AcquisitionTaxEngine.calculate(
                    AcquisitionTaxEngine.Input(
                        acquisitionPrice = price,
                        category = estate.category,
                        otherHomesAtAcquisition = othersAtBuy,
                    ),
                ).tax
                total += BrokerageFeeEngine.calculate(price, BrokerageFeeEngine.Side.PURCHASE)
            }
        }
        return total
    }

    private fun buildSuggestedSaleYears(
        estates: List<Asset.RealEstate>,
        startYear: Int,
    ): Map<String, Int> {
        val suggestions = mutableMapOf<String, Int>()
        estates.forEach { estate ->
            val saleYear = estate.saleYear ?: return@forEach
            val acqOthers = estates.filter { other ->
                other.id != estate.id &&
                    other.acquisitionYear != null &&
                    other.acquisitionYear!! <= saleYear &&
                    saleYear - other.acquisitionYear!! <= TaxDefaults.TEMPORARY_TWO_HOME_SALE_YEARS
            }
            if (acqOthers.isNotEmpty() && estate.isPrimaryResidence) {
                val newHome = acqOthers.maxByOrNull { it.acquisitionYear!! }!!
                val target = newHome.acquisitionYear!! + TaxDefaults.TEMPORARY_TWO_HOME_SALE_YEARS
                if (target < saleYear) {
                    suggestions[estate.id] = target
                }
            }
        }
        return suggestions
    }

    private fun estateLabel(index: Int, count: Int): String =
        if (count > 1) "부동산 ${index + 1}" else "부동산"
}
