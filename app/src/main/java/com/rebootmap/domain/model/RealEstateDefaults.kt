package com.rebootmap.domain.model

object RealEstateDefaults {
    const val MAX_COUNT = 3

    fun empty(id: String = "real_estate_1"): Asset.RealEstate = Asset.RealEstate(
        id = id,
        currentValue = 0L,
        category = RealEstateCategory.PRIMARY_RESIDENCE,
        saleYear = null,
        isPrimaryResidence = true,
    )

    fun nextId(existing: List<Asset.RealEstate>): String? {
        val used = existing.map { it.id }.toSet()
        for (index in 1..MAX_COUNT) {
            val id = "real_estate_$index"
            if (id !in used) return id
        }
        return null
    }

    fun propertyTaxRate(category: RealEstateCategory, assumptions: EconomicAssumptions): Double =
        when (category) {
            RealEstateCategory.PRIMARY_RESIDENCE -> assumptions.propertyTaxRate
            RealEstateCategory.NON_RESIDENTIAL -> assumptions.nonResidentialPropertyTaxRate
        }
}
