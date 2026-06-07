package com.rebootmap.domain.validation

import com.rebootmap.domain.model.Asset
import com.rebootmap.domain.model.EconomicAssumptions
import com.rebootmap.domain.model.PersonalLoan
import com.rebootmap.domain.model.PersonalLoanDefaults
import com.rebootmap.domain.model.RealEstateDefaults
import com.rebootmap.domain.model.UserProfile
import com.rebootmap.domain.scenario.RelocationPlan

/**
 * 입력·저장·시뮬레이션 전 단계에서 데이터 정합성을 점검한다.
 *
 * - [IntegrityLevel.ERROR]: 규칙 위반 (저장값 자체가 모순)
 * - [IntegrityLevel.WARNING]: 입력은 있으나 시뮬레이션에 반영되지 않는 불완전 상태
 */
enum class IntegrityLevel { ERROR, WARNING }

data class IntegrityIssue(
    val level: IntegrityLevel,
    val field: String,
    val message: String,
)

object SimulationIntegrity {

    fun validateProfile(profile: UserProfile): List<IntegrityIssue> = buildList {
        if (profile.currentAge !in 18..100) {
            add(issue(IntegrityLevel.ERROR, "profile.currentAge", "현재 나이는 18~100세여야 합니다."))
        }
        if (profile.retirementAge !in 18..100) {
            add(issue(IntegrityLevel.ERROR, "profile.retirementAge", "은퇴 연령은 18~100세여야 합니다."))
        }
        if (profile.currentAge in 18..100 && profile.retirementAge in 18..100 &&
            profile.retirementAge < profile.currentAge
        ) {
            add(issue(IntegrityLevel.ERROR, "profile.retirementAge", "은퇴 연령은 현재 나이 이상이어야 합니다."))
        }
        if (profile.lifeExpectancy > 0) {
            if (profile.lifeExpectancy !in 18..100) {
                add(issue(IntegrityLevel.ERROR, "profile.lifeExpectancy", "기대 수명은 18~100세여야 합니다."))
            } else if (profile.currentAge in 18..100 && profile.lifeExpectancy < profile.currentAge) {
                add(issue(IntegrityLevel.ERROR, "profile.lifeExpectancy", "기대 수명은 현재 나이 이상이어야 합니다."))
            }
        }
        if (profile.monthlyLivingExpense < 0) {
            add(issue(IntegrityLevel.ERROR, "profile.monthlyLivingExpense", "월 생활비는 0 이상이어야 합니다."))
        }
    }

    fun validateAssumptions(assumptions: EconomicAssumptions): List<IntegrityIssue> = buildList {
        if (assumptions.inflationRate !in 0.0..0.2) {
            add(issue(IntegrityLevel.ERROR, "assumptions.inflationRate", "물가상승률은 0~20%여야 합니다."))
        }
    }

    fun validateRelocation(
        plan: RelocationPlan,
        estates: List<Asset.RealEstate> = emptyList(),
    ): List<IntegrityIssue> = buildList {
        if (plan.newHomeDebt > plan.newHomeValue) {
            add(issue(IntegrityLevel.ERROR, "relocation.newHomeDebt", "신규 주택 부채는 시세 이하여야 합니다."))
        }
        if (!plan.enabled) return@buildList
        if (plan.sellEstateId.isNotBlank()) {
            val sell = estates.find { it.id == plan.sellEstateId }
            if (sell == null) {
                add(warn("relocation.sellEstateId", "매각 부동산이 목록에 없어 주거 로드맵이 반영되지 않을 수 있습니다."))
            } else if (sell.saleYear == null) {
                add(warn("relocation.sellEstateId", "매각 부동산에 매각 연도가 없어 주거 로드맵이 반영되지 않습니다."))
            }
        }
        if (plan.buyEstateId.isNotBlank()) {
            if (estates.none { it.id == plan.buyEstateId }) {
                add(warn("relocation.buyEstateId", "이주 후 부동산이 목록에 없어 주거 로드맵이 반영되지 않을 수 있습니다."))
            }
            if (plan.sellEstateId.isNotBlank() && plan.sellEstateId == plan.buyEstateId) {
                add(issue(IntegrityLevel.ERROR, "relocation.buyEstateId", "매각·구입 부동산은 서로 달라야 합니다."))
            }
        }
    }

    fun validateAsset(asset: Asset): List<IntegrityIssue> = when (asset) {
        is Asset.RealEstate -> buildList {
            if (asset.debtAmount > asset.currentValue) {
                add(issue(IntegrityLevel.ERROR, "realEstate.debt", "부채는 시세 이하여야 합니다."))
            }
            if (asset.expectedSalePrice > 0 && asset.saleYear == null) {
                add(warn("realEstate.expectedSalePrice", "예상 매각 가격은 매각 연도가 있을 때만 반영됩니다."))
            }
            if (!asset.isSimulationReady() && (asset.currentValue > 0 || asset.debtAmount > 0)) {
                add(warn("realEstate", "부분 입력 — 순자산 계산은 가능하나 매각 시나리오는 별도 확인이 필요합니다."))
            }
        }
        is Asset.NationalPension -> buildList {
            if (asset.monthlyPayout > 0 && asset.startAge == 0) {
                add(warn("nationalPension.startAge", "월 수령액은 입력됐으나 수령 연령이 없어 시뮬레이션에 반영되지 않습니다."))
            }
            if (asset.startAge > 0 && asset.monthlyPayout == 0L) {
                add(warn("nationalPension.monthlyPayout", "수령 연령은 있으나 월 수령액이 없어 시뮬레이션에 반영되지 않습니다."))
            }
            if (asset.startAge != 0 && asset.startAge !in 55..75) {
                add(issue(IntegrityLevel.ERROR, "nationalPension.startAge", "수령 시작 연령은 55~75세여야 합니다."))
            }
        }
        is Asset.EmploymentIncome -> validateRangedIncome(asset, "employmentIncome")
        is Asset.BusinessIncome -> validateRangedIncome(asset, "businessIncome")
        is Asset.OtherFixedIncome -> validateRangedIncome(asset, "otherFixedIncome")
        is Asset.CashSavings -> buildList {
            if (asset.maturityAmount > 0 && asset.maturityYear == 0) {
                add(warn("cashSavings.maturityYear", "만기 금액은 있으나 만기 연도가 없어 시뮬레이션에 반영되지 않습니다."))
            }
            if (asset.maturityYear > 0 && asset.maturityAmount == 0L) {
                add(warn("cashSavings.maturityAmount", "만기 연도는 있으나 금액이 없어 시뮬레이션에 반영되지 않습니다."))
            }
        }
        is Asset.HousingPension -> buildList {
            if (asset.enabled && asset.startAge == 0) {
                add(warn("housingPension.startAge", "주택연금이 켜져 있으나 개시 연령이 없습니다."))
            }
        }
        else -> emptyList()
    }

    fun validatePersonalLoan(loan: PersonalLoan): List<IntegrityIssue> = buildList {
        if (loan.balance > 0 && loan.monthlyPayment == 0L) {
            add(
                warn(
                    "personalLoan.monthlyPayment",
                    "월 상환이 없으면 이자만 연간 부담으로 계산됩니다.",
                ),
            )
        }
        if (loan.repaymentEndAge > 0 && loan.monthlyPayment == 0L && loan.balance > 0) {
            add(warn("personalLoan.repaymentEndAge", "상환 종료 연령은 월 상환액이 있을 때 의미가 있습니다."))
        }
    }

    fun validatePersonalLoanCollection(loans: List<PersonalLoan>): List<IntegrityIssue> = buildList {
        if (loans.size > PersonalLoanDefaults.MAX_COUNT) {
            add(
                issue(
                    IntegrityLevel.ERROR,
                    "personalLoans.count",
                    "신용·차용 부채는 최대 ${PersonalLoanDefaults.MAX_COUNT}건까지 입력할 수 있습니다.",
                ),
            )
        }
        val duplicateIds = loans.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            add(
                issue(
                    IntegrityLevel.ERROR,
                    "personalLoans.id",
                    "부채 ID가 중복되었습니다: ${duplicateIds.joinToString()}",
                ),
            )
        }
    }

    fun validateRealEstateCollection(assets: List<Asset>): List<IntegrityIssue> = buildList {
        val estates = assets.filterIsInstance<Asset.RealEstate>()
        if (estates.size > RealEstateDefaults.MAX_COUNT) {
            add(
                issue(
                    IntegrityLevel.ERROR,
                    "realEstates.count",
                    "부동산은 최대 ${RealEstateDefaults.MAX_COUNT}건까지 입력할 수 있습니다.",
                ),
            )
        }
        val duplicateIds = estates.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            add(
                issue(
                    IntegrityLevel.ERROR,
                    "realEstates.id",
                    "부동산 ID가 중복되었습니다: ${duplicateIds.joinToString()}",
                ),
            )
        }
    }

    fun validateAssets(assets: List<Asset>): List<IntegrityIssue> =
        validateRealEstateCollection(assets) + assets.flatMap(::validateAsset)

    /** 시뮬레이션에 포함되는 자산만 추려 정합성·반영 여부를 함께 검증 */
    fun validateForSimulation(
        profile: UserProfile,
        assumptions: EconomicAssumptions,
        assets: List<Asset>,
        relocationPlan: RelocationPlan = RelocationPlan(),
        personalLoans: List<PersonalLoan> = emptyList(),
    ): List<IntegrityIssue> {
        val issues = buildList {
            addAll(validateProfile(profile))
            addAll(validateAssumptions(assumptions))
            addAll(validateRelocation(relocationPlan, assets.filterIsInstance<Asset.RealEstate>()))
            addAll(validatePersonalLoanCollection(personalLoans))
            addAll(personalLoans.flatMap(::validatePersonalLoan))
            addAll(validateAssets(assets))
        }
        val active = assets.filter { it.isSimulationReady() }
        return buildList {
            addAll(issues)
            if (active.isEmpty()) {
                add(warn("assets", "시뮬레이션에 반영할 자산이 없습니다."))
            }
        }
    }

    private fun issue(level: IntegrityLevel, field: String, message: String) =
        IntegrityIssue(level, field, message)

    private fun warn(field: String, message: String) =
        IntegrityIssue(IntegrityLevel.WARNING, field, message)

    private fun validateRangedIncome(
        asset: Asset,
        fieldPrefix: String,
    ): List<IntegrityIssue> {
        val monthly = when (asset) {
            is Asset.EmploymentIncome -> asset.monthlyAmount
            is Asset.BusinessIncome -> asset.monthlyAmount
            is Asset.OtherFixedIncome -> asset.monthlyAmount
            else -> return emptyList()
        }
        val startAge = when (asset) {
            is Asset.EmploymentIncome -> asset.startAge
            is Asset.BusinessIncome -> asset.startAge
            is Asset.OtherFixedIncome -> asset.startAge
            else -> 0
        }
        val endAge = when (asset) {
            is Asset.EmploymentIncome -> asset.endAge
            is Asset.BusinessIncome -> asset.endAge
            is Asset.OtherFixedIncome -> asset.endAge
            else -> 0
        }
        return buildList {
            if (monthly > 0 && (startAge == 0 || endAge == 0)) {
                add(warn("$fieldPrefix.age", "월 수입은 입력됐으나 시작·종료 연령이 불완전해 시뮬레이션에 반영되지 않습니다."))
            }
            if (startAge > 0 && endAge > 0 && startAge > endAge) {
                add(warn("$fieldPrefix.age", "시작 연령이 종료 연령보다 커서 수입이 시뮬레이션에 반영되지 않습니다."))
            }
        }
    }
}

/** ViewModel [com.rebootmap.presentation.simulation.SimulationViewModel] 과 동일 기준 */
fun Asset.isSimulationReady(): Boolean = when (this) {
    is Asset.RealEstate -> currentValue > 0 || debtAmount > 0
    is Asset.NationalPension -> monthlyPayout > 0 && startAge > 0
    is Asset.SeverancePension -> balance > 0 || monthlyContribution > 0
    is Asset.PersonalPension -> balance > 0 || monthlyContribution > 0
    is Asset.YellowUmbrella -> balance > 0 || monthlyContribution > 0
    is Asset.Investment -> currentValue > 0
    is Asset.CashSavings -> maturityAmount > 0 && maturityYear > 0
    is Asset.EmploymentIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
    is Asset.BusinessIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
    is Asset.OtherFixedIncome -> monthlyAmount > 0 && startAge > 0 && endAge > 0
    is Asset.HousingPension -> enabled && startAge > 0
}
