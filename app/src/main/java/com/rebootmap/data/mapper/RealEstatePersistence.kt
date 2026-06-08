package com.rebootmap.data.mapper

import com.rebootmap.data.model.PersistedRealEstate
import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateCategory
import com.rebootmap.domain.model.RealEstateDefaults

object RealEstatePersistence {

    fun resolveFromPersisted(persisted: SimulationPersistedState): List<Asset.RealEstate> {
        if (persisted.realEstates.isNotEmpty()) {
            return persisted.realEstates.map { it.toDomain() }
        }
        if (hasLegacyRealEstate(persisted)) {
            return listOf(legacyToDomain(persisted))
        }
        return listOf(RealEstateDefaults.empty())
    }

    fun toPersistedList(estates: List<Asset.RealEstate>): List<PersistedRealEstate> =
        estates.map { PersistedRealEstate.fromDomain(it) }

    fun syncLegacyFields(
        first: Asset.RealEstate?,
    ): LegacyRealEstateFields = if (first == null) {
        LegacyRealEstateFields()
    } else {
        LegacyRealEstateFields(
            value = first.currentValue,
            debt = first.debtAmount,
            acquisitionCost = first.acquisitionCost,
            holdingYears = first.holdingYears,
            isPrimaryResidence = first.isPrimaryResidence,
            saleYear = first.saleYear,
            expectedSalePrice = first.expectedSalePrice,
        )
    }

    private fun hasLegacyRealEstate(persisted: SimulationPersistedState): Boolean =
        persisted.realEstateValue > 0L ||
            persisted.realEstateDebt > 0L ||
            persisted.realEstateAcquisitionCost > 0L ||
            persisted.realEstateSaleYear != null

    private fun legacyToDomain(persisted: SimulationPersistedState): Asset.RealEstate {
        val category = RealEstateCategory.fromLegacyPrimary(persisted.realEstateIsPrimaryResidence)
        return Asset.RealEstate(
            id = "real_estate_1",
            currentValue = persisted.realEstateValue,
            debtAmount = persisted.realEstateDebt,
            acquisitionCost = persisted.realEstateAcquisitionCost,
            holdingYears = persisted.realEstateHoldingYears,
            category = category,
            isPrimaryResidence = category == RealEstateCategory.PRIMARY_RESIDENCE,
            acquisitionYear = null,
            saleYear = persisted.realEstateSaleYear,
            expectedSalePrice = persisted.realEstateExpectedSalePrice,
        )
    }

    data class LegacyRealEstateFields(
        val value: Long = 0L,
        val debt: Long = 0L,
        val acquisitionCost: Long = 0L,
        val holdingYears: Int = 0,
        val isPrimaryResidence: Boolean = false,
        val saleYear: Int? = null,
        val expectedSalePrice: Long = 0L,
    )
}
