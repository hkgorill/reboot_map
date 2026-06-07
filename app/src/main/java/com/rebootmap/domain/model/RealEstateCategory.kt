package com.rebootmap.domain.model

/** 부동산 유형 — 보유세(재산세) 세율 구분용 (주거용/비주거용) */
enum class RealEstateCategory {
    /** 주거용 */
    PRIMARY_RESIDENCE,
    /** 비주거용 (임대·상가 등) */
    NON_RESIDENTIAL,
    ;

    fun label(): String = when (this) {
        PRIMARY_RESIDENCE -> "주거용"
        NON_RESIDENTIAL -> "비주거용"
    }

    companion object {
        fun fromPersisted(value: String): RealEstateCategory = when (value) {
            "NON_RESIDENTIAL" -> NON_RESIDENTIAL
            else -> PRIMARY_RESIDENCE
        }

        /** 레거시 [Asset.RealEstate.isPrimaryResidence] → 유형 */
        fun fromLegacyPrimary(isPrimaryResidence: Boolean): RealEstateCategory =
            if (isPrimaryResidence) PRIMARY_RESIDENCE else NON_RESIDENTIAL
    }
}
