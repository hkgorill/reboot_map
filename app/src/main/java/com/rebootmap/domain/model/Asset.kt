package com.rebootmap.domain.model

sealed class Asset {
    abstract val id: String

    data class RealEstate(
        override val id: String = "real_estate",
        val currentValue: Long,
        val saleYear: Int?,
    ) : Asset() {
        init {
            require(currentValue >= 0) { "부동산 시세는 0 이상이어야 합니다." }
        }
    }

    data class NationalPension(
        override val id: String = "national_pension",
        val monthlyPayout: Long,
        val startAge: Int = 65,
    ) : Asset() {
        init {
            require(monthlyPayout >= 0) { "국민연금 수령액은 0 이상이어야 합니다." }
            require(startAge in 55..75) { "수령 시작 연령은 55~75 사이여야 합니다." }
        }
    }

    data class RetirementPension(
        override val id: String = "retirement_pension",
        val balance: Long,
        val monthlyContribution: Long,
        val contributionEndAge: Int,
    ) : Asset() {
        init {
            require(balance >= 0) { "퇴직연금 잔액은 0 이상이어야 합니다." }
            require(monthlyContribution >= 0) { "월 납입액은 0 이상이어야 합니다." }
            require(contributionEndAge in 18..100) { "납입 종료 연령이 유효하지 않습니다." }
        }
    }

    data class Investment(
        override val id: String = "investment",
        val currentValue: Long,
        val annualReturnRate: Double,
    ) : Asset() {
        init {
            require(currentValue >= 0) { "투자 평가액은 0 이상이어야 합니다." }
            require(annualReturnRate in -0.5..1.0) { "연 수익률은 -50%~100% 사이여야 합니다." }
        }
    }

    data class CashSavings(
        override val id: String = "cash_savings",
        val maturityAmount: Long,
        val maturityYear: Int,
    ) : Asset() {
        init {
            require(maturityAmount >= 0) { "만기 금액은 0 이상이어야 합니다." }
        }
    }
}
