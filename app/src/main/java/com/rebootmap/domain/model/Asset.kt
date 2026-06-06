package com.rebootmap.domain.model

/** 연금 상품별 기본 가정 (2024년 제도·통계 근사) */
object PensionDefaults {
    /** DC 퇴직연금 운용수익률 가정 */
    const val SEVERANCE_RETURN_RATE = 0.03

    /** 연금저축·IRP 적립기간 운용수익률 가정 */
    const val PERSONAL_RETURN_RATE = 0.03

    /** 노랑우산공제 공제이자 (연 복리, 2024년 5년 만기 기준 약 3.3%) */
    const val YELLOW_UMBRELLA_RETURN_RATE = 0.033

    /** 연금저축 최소 연금 수령 개시 연령 */
    const val PERSONAL_MIN_PAYOUT_AGE = 55
}

sealed class Asset {
    abstract val id: String

    data class RealEstate(
        override val id: String = "real_estate",
        val currentValue: Long,
        val debtAmount: Long = 0L,
        val acquisitionCost: Long = 0L,
        val holdingYears: Int = 10,
        val isPrimaryResidence: Boolean = true,
        val saleYear: Int?,
    ) : Asset() {
        init {
            require(currentValue >= 0) { "부동산 시세는 0 이상이어야 합니다." }
            require(debtAmount >= 0) { "부채 금액은 0 이상이어야 합니다." }
            require(acquisitionCost >= 0) { "취득가액은 0 이상이어야 합니다." }
            require(holdingYears in 0..50) { "보유 연수가 유효하지 않습니다." }
        }

        val netEquity: Long get() = (currentValue - debtAmount).coerceAtLeast(0L)

        /** 양도세 계산용 취득가 — 미입력 시 순자산 기준(차익 0 가정) */
        val effectiveAcquisitionCost: Long
            get() = if (acquisitionCost > 0) acquisitionCost else netEquity
    }

    data class NationalPension(
        override val id: String = "national_pension",
        val monthlyPayout: Long,
        val startAge: Int = 65,
    ) : Asset() {
        init {
            require(monthlyPayout >= 0) { "국민연금 수령액은 0 이상이어야 합니다." }
            require(startAge == 0 || startAge in 55..75) { "수령 시작 연령은 55~75 사이여야 합니다." }
        }
    }

    /**
     * 퇴직연금 (DC·DB·IRP 퇴직금)
     * - 근로자: 퇴직 시까지 적립, 은퇴 후 연금·일시금 수령
     * - 적립 중 DC 운용수익 반영, 은퇴 후 잔액 균등 인출
     */
    data class SeverancePension(
        override val id: String = "severance_pension",
        val balance: Long,
        val monthlyContribution: Long,
        val contributionEndAge: Int,
        val annualReturnRate: Double = PensionDefaults.SEVERANCE_RETURN_RATE,
    ) : Asset() {
        init {
            require(balance >= 0) { "퇴직연금 잔액은 0 이상이어야 합니다." }
            require(monthlyContribution >= 0) { "월 납입액은 0 이상이어야 합니다." }
            require(contributionEndAge == 0 || contributionEndAge in 18..100) {
                "납입 종료 연령이 유효하지 않습니다."
            }
            require(annualReturnRate in -0.2..0.2) { "운용 수익률이 유효하지 않습니다." }
        }
    }

    /**
     * 개인연금 (연금저축·개인형 IRP)
     * - 세액공제 대상, 55세 이후 연금 수령
     * - 적립 중 운용수익 반영, 수령 개시 후 잔액 균등 인출
     */
    data class PersonalPension(
        override val id: String = "personal_pension",
        val balance: Long,
        val monthlyContribution: Long,
        val contributionEndAge: Int,
        val payoutStartAge: Int = PensionDefaults.PERSONAL_MIN_PAYOUT_AGE,
        val annualReturnRate: Double = PensionDefaults.PERSONAL_RETURN_RATE,
    ) : Asset() {
        init {
            require(balance >= 0) { "개인연금 잔액은 0 이상이어야 합니다." }
            require(monthlyContribution >= 0) { "월 납입액은 0 이상이어야 합니다." }
            require(contributionEndAge == 0 || contributionEndAge in 18..100) {
                "납입 종료 연령이 유효하지 않습니다."
            }
            require(payoutStartAge == 0 || payoutStartAge in 55..70) {
                "수령 개시 연령은 55~70세 사이여야 합니다."
            }
            require(annualReturnRate in -0.2..0.2) { "운용 수익률이 유효하지 않습니다." }
        }
    }

    /**
     * 노랑우산공제 (소기업·소상공인 공제)
     * - 자영업자·소상공인 대상, 공제부금 적립
     * - 공제이자 복리 적용, 지정 연령에 일시금 수령 (폐업·만기 시)
     */
    data class YellowUmbrella(
        override val id: String = "yellow_umbrella",
        val balance: Long,
        val monthlyContribution: Long,
        val contributionEndAge: Int,
        val payoutAge: Int = 60,
        val annualReturnRate: Double = PensionDefaults.YELLOW_UMBRELLA_RETURN_RATE,
    ) : Asset() {
        init {
            require(balance >= 0) { "노랑우산 잔액은 0 이상이어야 합니다." }
            require(monthlyContribution >= 0) { "월 공제부금은 0 이상이어야 합니다." }
            require(contributionEndAge == 0 || contributionEndAge in 18..100) {
                "납입 종료 연령이 유효하지 않습니다."
            }
            require(payoutAge == 0 || payoutAge in 55..70) { "수령 연령은 55~70세 사이여야 합니다." }
            require(annualReturnRate in 0.0..0.1) { "공제이자율이 유효하지 않습니다." }
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

    /**
     * 고정수입 (임대료·근로소득·퇴직 후 아르바이트 등)
     * - 지정 연령 구간 동안 매년 월 수입 × 12 반영
     * - 근로소득은 종료 연령을 은퇴 연령으로, 임대료는 기대 수명까지 설정
     */
    data class FixedIncome(
        override val id: String = "fixed_income",
        val monthlyAmount: Long,
        val startAge: Int,
        val endAge: Int,
    ) : Asset() {
        init {
            require(monthlyAmount >= 0) { "월 고정수입은 0 이상이어야 합니다." }
            require(startAge == 0 || startAge in 18..100) { "수입 시작 연령이 유효하지 않습니다." }
            require(endAge == 0 || endAge in 18..100) { "수입 종료 연령이 유효하지 않습니다." }
            require(startAge == 0 && endAge == 0 || startAge in 18..100 && endAge in 18..100 && startAge <= endAge) {
                "수입 시작 연령은 종료 연령 이하여야 합니다."
            }
        }
    }

    /**
     * 주택연금 (역모기지) — 주택 담보 월 수령액
     * - homeEquityOverride가 0이면 부동산 순자산을 담보로 사용
     */
    data class HousingPension(
        override val id: String = "housing_pension",
        val enabled: Boolean = false,
        val startAge: Int = 65,
        val homeEquityOverride: Long = 0L,
    ) : Asset() {
        init {
            require(startAge == 0 || startAge in 55..80) {
                "주택연금 개시 연령은 55~80세 사이여야 합니다."
            }
            require(homeEquityOverride >= 0) { "담보 주택 가치는 0 이상이어야 합니다." }
        }
    }
}
