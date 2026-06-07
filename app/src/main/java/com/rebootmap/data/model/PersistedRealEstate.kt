package com.rebootmap.data.model

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateCategory
import kotlinx.serialization.Serializable

@Serializable
data class PersistedRealEstate(
    val id: String = "real_estate_1",
    val category: String = RealEstateCategory.PRIMARY_RESIDENCE.name,
    val currentValue: Long = 0L,
    val debtAmount: Long = 0L,
    val acquisitionCost: Long = 0L,
    val holdingYears: Int = 10,
    val isPrimaryResidence: Boolean = true,
    val saleYear: Int? = null,
    val expectedSalePrice: Long = 0L,
) {
    fun toDomain(): Asset.RealEstate {
        val resolvedCategory = RealEstateCategory.fromPersisted(category)
        return Asset.RealEstate(
            id = id,
            currentValue = currentValue,
            debtAmount = debtAmount,
            acquisitionCost = acquisitionCost,
            holdingYears = holdingYears,
            category = resolvedCategory,
            isPrimaryResidence = resolvedCategory == RealEstateCategory.PRIMARY_RESIDENCE,
            saleYear = saleYear,
            expectedSalePrice = expectedSalePrice,
        )
    }

    companion object {
        fun fromDomain(estate: Asset.RealEstate): PersistedRealEstate = PersistedRealEstate(
            id = estate.id,
            category = estate.category.name,
            currentValue = estate.currentValue,
            debtAmount = estate.debtAmount,
            acquisitionCost = estate.acquisitionCost,
            holdingYears = estate.holdingYears,
            isPrimaryResidence = estate.isPrimaryResidence,
            saleYear = estate.saleYear,
            expectedSalePrice = estate.expectedSalePrice,
        )
    }
}
