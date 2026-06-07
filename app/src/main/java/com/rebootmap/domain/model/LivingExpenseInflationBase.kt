package com.rebootmap.domain.model

/**
 * 은퇴 후 생활비에 물가상승률을 적용할 때의 기준 시점.
 *
 * - [SIMULATION_START]: 시뮬레이션 시작(현재)부터 물가 누적 — 은퇴 직후에도 이미 상승 반영
 * - [RETIREMENT_AGE]: 은퇴 연령을 기준(0년)으로 물가 누적 — 목표 월 생활비가 은퇴 직후 금액
 */
enum class LivingExpenseInflationBase {
    SIMULATION_START,
    RETIREMENT_AGE,
    ;

    companion object {
        fun fromPersisted(value: String): LivingExpenseInflationBase =
            entries.find { it.name == value } ?: RETIREMENT_AGE
    }
}
