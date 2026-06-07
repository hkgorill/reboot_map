package com.rebootmap.data.mapper

import com.rebootmap.data.model.PersistedRealEstate
import com.rebootmap.data.model.SimulationPersistedState
import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.RealEstateCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RealEstatePersistenceTest {

    @Test
    fun `레거시 단일 필드는 부동산 1건으로 복원된다`() {
        val persisted = SimulationPersistedState(
            realEstateValue = 500_000_000L,
            realEstateDebt = 100_000_000L,
            realEstateIsPrimaryResidence = true,
            realEstateSaleYear = 2035,
        )

        val estates = RealEstatePersistence.resolveFromPersisted(persisted)

        assertEquals(1, estates.size)
        assertEquals("real_estate_1", estates.first().id)
        assertEquals(400_000_000L, estates.first().netEquity)
        assertEquals(RealEstateCategory.PRIMARY_RESIDENCE, estates.first().category)
        assertEquals(2035, estates.first().saleYear)
    }

    @Test
    fun `v3 realEstates 리스트가 우선 복원된다`() {
        val persisted = SimulationPersistedState(
            realEstateValue = 1L,
            realEstates = listOf(
                PersistedRealEstate(
                    id = "real_estate_2",
                    category = RealEstateCategory.NON_RESIDENTIAL.name,
                    currentValue = 300_000_000L,
                    debtAmount = 50_000_000L,
                    isPrimaryResidence = false,
                ),
            ),
        )

        val estates = RealEstatePersistence.resolveFromPersisted(persisted)

        assertEquals(1, estates.size)
        assertEquals("real_estate_2", estates.first().id)
        assertEquals(RealEstateCategory.NON_RESIDENTIAL, estates.first().category)
        assertEquals(250_000_000L, estates.first().netEquity)
    }

    @Test
    fun `복수 부동산 저장-복원 왕복이 유지된다`() {
        val original = listOf(
            Asset.RealEstate(
                id = "real_estate_1",
                currentValue = 600_000_000L,
                category = RealEstateCategory.PRIMARY_RESIDENCE,
                saleYear = null,
            ),
            Asset.RealEstate(
                id = "real_estate_2",
                currentValue = 200_000_000L,
                category = RealEstateCategory.NON_RESIDENTIAL,
                isPrimaryResidence = false,
                saleYear = null,
            ),
        )

        val persisted = RealEstatePersistence.toPersistedList(original)
        val restored = persisted.map { it.toDomain() }

        assertEquals(original, restored)
    }
}
