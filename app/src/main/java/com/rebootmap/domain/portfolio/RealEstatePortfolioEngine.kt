package com.rebootmap.domain.portfolio

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.TaxDefaults
import com.rebootmap.domain.scenario.RelocationYearFlags

/** 연도별 부동산 보유·겹침 — 취득·매각 연도 기준 */
object RealEstatePortfolioEngine {

    fun ownedEstates(
        estates: List<Asset.RealEstate>,
        year: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): List<Asset.RealEstate> = estates.filter { estate ->
        isOwned(estate, year, startYear, soldIds)
    }

    fun homeCount(
        estates: List<Asset.RealEstate>,
        year: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): Int = ownedEstates(estates, year, startYear, soldIds).size

    fun isOwned(
        estate: Asset.RealEstate,
        year: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): Boolean {
        if (estate.id in soldIds) return false
        val acq = effectiveAcquisitionYear(estate, startYear)
        if (year < acq) return false
        val sale = estate.saleYear
        if (sale != null && year >= sale) return false
        return estate.currentValue > 0 || estate.debtAmount > 0
    }

    fun effectiveAcquisitionYear(estate: Asset.RealEstate, startYear: Int): Int =
        estate.acquisitionYear ?: startYear

    fun holdingYearsAtSale(estate: Asset.RealEstate, saleYear: Int, startYear: Int): Int {
        val acq = effectiveAcquisitionYear(estate, startYear)
        return (saleYear - acq).coerceAtLeast(estate.holdingYears)
    }

    fun otherHomesAtSale(
        soldEstate: Asset.RealEstate,
        estates: List<Asset.RealEstate>,
        saleYear: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): Int = ownedEstates(estates, saleYear, startYear, soldIds)
        .count { it.id != soldEstate.id }

    /**
     * 일시적 1가구 2주택 — 신규 주택 취득 후 구주택을 [TEMPORARY_TWO_HOME_SALE_YEARS]년 이내 처분 시
     * 구주택 양도 비과세 간이 적용 (교육용).
     */
    fun qualifiesTemporaryTwoHomeExemption(
        soldEstate: Asset.RealEstate,
        estates: List<Asset.RealEstate>,
        saleYear: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): Boolean {
        if (!soldEstate.isPrimaryResidence) return false
        val others = ownedEstates(estates, saleYear, startYear, soldIds)
            .filter { it.id != soldEstate.id && it.category == com.rebootmap.domain.model.RealEstateCategory.PRIMARY_RESIDENCE }
        if (others.size != 1) return false
        val newer = others.first()
        val newAcq = newer.acquisitionYear ?: return false
        if (newAcq > saleYear) return false
        val yearsAfterNew = saleYear - newAcq
        return yearsAfterNew in 0..TaxDefaults.TEMPORARY_TWO_HOME_SALE_YEARS
    }

    fun portfolioFlags(
        estates: List<Asset.RealEstate>,
        year: Int,
        startYear: Int,
        soldIds: Set<String>,
    ): RelocationYearFlags {
        val count = homeCount(estates, year, startYear, soldIds)
        val hasConfigured = estates.any {
            it.saleYear != null || it.acquisitionYear != null
        }
        val futureAcquisition = estates.any { estate ->
            val acq = estate.acquisitionYear
            acq != null && acq > year && (estate.saleYear == null || estate.saleYear > acq)
        }
        return RelocationYearFlags(
            active = hasConfigured,
            isTwoHomeOverlap = count >= 2,
            isGapPeriod = count == 0 && futureAcquisition,
        )
    }

    fun overlapYears(
        estates: List<Asset.RealEstate>,
        startYear: Int,
        endYear: Int,
    ): List<Int> = (startYear..endYear).filter { year ->
        homeCount(estates, year, startYear, emptySet()) >= 2
    }
}
