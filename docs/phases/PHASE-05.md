# Phase 5 — 세금 정밀화 + 소득 유형 분리

> **상태:** ✅ 완료 (코드·자체 테스트·실기기 검증)  
> **PRD 참조:** [`../PRD.md`](../PRD.md) §6 Phase 5  
> **버전 (목표):** `1.4.0-phase5`  
> **작성일:** 2026-06-07

---

## 1. 목표

**예측 가능한 수준**에서 노후 시뮬레이션에 빠져 있던 보유·부과 세금을 반영하고,  
근로(직장) 소득과 사업 소득을 분리 입력해 **세금·건강보험료 산정 근거**를 사용자에게 명확히 보여준다.

### 배경 (Phase 1~4 한계)

| 현재 | 한계 |
|------|------|
| 연금 3.3% · 기타 15% 고정 | 종합소득세·소득 유형별 차이 미반영 |
| 부동산 | **매각 시** 양도세만, **보유 중** 재산세·종부세 없음 |
| 고정수입 1종 | 직장·사업·임대 구분 없음, 4대 보험·건강보험료 없음 |

---

## 2. 범위

### 2.1 포함 (In Scope)

#### A. 소득 유형 분리

| ID | 항목 | 입력 | 비고 |
|----|------|------|------|
| P5-01 | **직장 소득** | 월 금액, 시작 연령, 종료 연령 | 기존 `FixedIncome` 중 급여·퇴직 전 근로 |
| P5-02 | **사업 소득** | 월 금액, 시작 연령, 종료 연령 | 프리랜서·자영업 순수익 근사 |
| P5-03 | **기타 고정수입** (선택) | 월 금액, 시작·종료 연령 | 임대료·퇴직 후 아르바이트 등 |

- UI 라벨은 **「연령」** 기준 (앱 전체와 일관). 사용자가 말한 「시작/종료년도」는 `현재 나이 + (연도 − 올해)` 로 연령에 환산해 입력 (보조 힌트: `2028년 ≈ N세`).
- DataStore **v2 마이그레이션:** 기존 `fixedIncome*` → `employmentIncome*` 로 1회 이관 (사업 0).

#### B. 세금·부과 비용 (예측 가능한 간이 모델)

| ID | 항목 | 산정 방식 (MVP) | 반영 시점 |
|----|------|-----------------|-----------|
| P5-04 | **연금소득세** | 연금 수입 × `pensionIncomeTaxRate` (기본 3.3%) | 매년 |
| P5-05 | **근로소득세 (간이)** | 직장 소득 × `employmentIncomeTaxRate` (기본 6%, 원천징수 근사) | 해당 연령 구간 |
| P5-06 | **사업소득세 (간이)** | 사업 소득 × `businessIncomeTaxRate` (기본 15%, 종합소득 분리과세 근사) | 해당 연령 구간 |
| P5-07 | **양도소득세** | 기존 `CapitalGainsTaxEngine` 유지 | 매각 연도 |
| P5-08 | **재산세 (간이)** | 부동산 순자산 × `propertyTaxRate` (기본 0.25%/년, 지역·주택 유형은 후속) | 보유 매년 |
| P5-09 | **종합부동산세 (간이)** | 순자산 > `comprehensiveTaxThreshold` 시 초과분 × `comprehensiveTaxRate` (기본 0.6%/년) | 보유 매년 |
| P5-10 | **건강보험료 (지역가입자 간이)** | 소득·재산 **점수** 기반 월 보험료 × 12 (2024 요율표 단순화) | 은퇴 후 또는 사업·연금 수입 시 |
| P5-11 | **장기요양보험료** | 건강보험료 × `longTermCareRate` (기본 12.95%) | 건강보험과 동시 |

> **원칙:** 국세청·건보공단 **공식과 100% 일치하지 않음**. 사용자가 ON/OFF·비율 조정 가능, PDF·UI에 「간이 추정」 명시.

#### C. 표시·저장

| ID | 항목 |
|----|------|
| P5-12 | `AnnualTaxBreakdown` — 세목별 연간 breakdown |
| P5-13 | 월 순수입 카드 — 세금 열 → 탭/펼침 시 세목별 |
| P5-14 | PDF 리포트 — 세금 breakdown 섹션 |
| P5-15 | `EconomicAssumptions` 확장 + DataStore 필드 |

### 2.2 제외 (Out of Scope)

- 종합소득세 **누진·공제·부양가족** 정밀 신고 수준
- 지방소득세 **분리 계산** (소득세에 일괄 포함 또는 고정 가산율로 흡수)
- 취득세·상속세·증여세
- 직장가입자 4대 보험 **월별 정산** (직장 소득은 원천징수 세율에 흡수)
- 마이데이터·홈택스 연동
- 다주택·법인 보유 등 복잡한 종부세 실무 예외 전부

---

## 3. 도메인 설계

### 3.1 자산·소득 모델

```kotlin
// 기존 FixedIncome deprecated → 마이그레이션 후 제거

data class EmploymentIncome(
    override val id: String = "employment_income",
    val monthlyAmount: Long,
    val startAge: Int,
    val endAge: Int,
) : Asset()

data class BusinessIncome(
    override val id: String = "business_income",
    val monthlyAmount: Long,
    val startAge: Int,
    val endAge: Int,
) : Asset()

data class OtherFixedIncome(  // 임대·아르바이트 (선택)
    override val id: String = "other_fixed_income",
    val monthlyAmount: Long,
    val startAge: Int,
    val endAge: Int,
) : Asset()
```

### 3.2 경제 가정 확장 (`EconomicAssumptions`)

| 필드 | 기본값 | 설명 |
|------|--------|------|
| `employmentIncomeTaxRate` | 0.06 | 근로소득 간이세율 |
| `businessIncomeTaxRate` | 0.15 | 사업소득 간이세율 |
| `propertyTaxEnabled` | true | 재산세 반영 |
| `propertyTaxRate` | 0.0025 | 순자산 대비 연율 |
| `comprehensiveRealEstateTaxEnabled` | true | 종부세 반영 |
| `comprehensiveTaxThreshold` | 600_000_000L | 1주택 6억 공제 근사 |
| `comprehensiveTaxRate` | 0.006 | 초과분 연율 |
| `healthInsuranceEnabled` | true | 지역가입자 보험료 |
| `longTermCareRate` | 0.1295 | 장기요양 요율 |

기존 `generalIncomeTaxRate` → **사업·기타 고정수입**에만 적용 후 점진 폐기.

### 3.3 세금 엔진 구조

```
domain/tax/
├── CapitalGainsTaxEngine.kt      (기존)
├── PropertyHoldingTaxEngine.kt   (신규 P5-08~09)
├── HealthInsurancePremiumEngine.kt (신규 P5-10~11)
├── IncomeTaxEngine.kt            (신규 — 소득 유형별 집계)
└── AnnualTaxBreakdown.kt         (신규)
```

```kotlin
data class AnnualTaxBreakdown(
    val pensionIncomeTax: Long = 0,
    val employmentIncomeTax: Long = 0,
    val businessIncomeTax: Long = 0,
    val otherIncomeTax: Long = 0,
    val capitalGainsTax: Long = 0,
    val propertyTax: Long = 0,
    val comprehensiveRealEstateTax: Long = 0,
    val healthInsurance: Long = 0,
    val longTermCare: Long = 0,
) {
    val total: Long get() = /* sum */
}
```

### 3.4 건강보험료 간이 산식 (지역가입자)

**입력 (해당 연도)**

- **소득 점수:** (연금 수입 + 사업 소득 + 기타 고정수입 + 금융 이자 추정) / 12 → 월 → 건보 **소득 월액 보험료** 테이블 (구간 고정값 3~4단)
- **재산 점수:** (금융자산 + 부동산 순자산 × 공제율) → 건보 **재산 점수 보험료** 테이블
- **연 보험료** = (소득분 + 재산분) × 12, 상·하한 캡 (예: 월 20만~500만 근사)

은퇴 전 직장 소득만 있는 경우: 건강보험 **직장가입자 가정** → Phase 5에서는 **지역 전환 연령(은퇴)** 이후만 자동 산정 (설정으로 은퇴 전부터 지역 가정 가능).

### 3.5 CashFlowEngine 변경

```
annualExpense = 생활비(물가) + propertyTax + comprehensiveRET  // 보유 부담
annualTax = IncomeTaxEngine + capitalGains + healthInsurance + longTermCare
netCashFlow = annualIncome - annualExpense - annualTax
```

- `YearSnapshot`에 `taxBreakdown: AnnualTaxBreakdown` 추가 (또는 `annualTax` 유지 + breakdown 별도)
- `liquidAssets` 계산 로직은 Phase 4와 동일

---

## 4. UI 설계

### 4.1 자산 카드

| 카드 | 필드 |
|------|------|
| **직장 소득** | 월 금액, 시작 연령, 종료 연령 (힌트: 은퇴 연령까지) |
| **사업 소득** | 월 금액, 시작 연령, 종료 연령 |
| **기타 고정수입** | 월 금액, 시작·종료 (임대료 등) |

기존 「고정수입」 카드 **제거** (마이그레이션 안내 1회).

### 4.2 기본 정보 / 가정

- 토글: 재산세 · 종부세 · 건강보험료 반영 여부
- 고급: 각 세율·임계값 (기본 접힘)

### 4.3 월 순수입 vs 생활비

| 열 | 설명 |
|----|------|
| 월 수입 | 세전 (탭 시 항목별, Phase 6.1~) |
| 월 생활비 | 물가 반영 |
| 월 세금 | 소득세·건보·보유세(재산세·종부세) 합산 (탭 시 세목별) |
| 월 순현금 | 생활비·세금 차감 후 |

---

## 5. 저장·마이그레이션

`SimulationPersistedState` v2 필드 예시:

```kotlin
// 소득
val employmentIncomeMonthly: Long = 0L
val employmentIncomeStartAge: Int = 0
val employmentIncomeEndAge: Int = 0
val businessIncomeMonthly: Long = 0L
val businessIncomeStartAge: Int = 0
val businessIncomeEndAge: Int = 0
val otherFixedIncomeMonthly: Long = 0L
// ...

// 세금 가정
val propertyTaxEnabled: Boolean = true
val healthInsuranceEnabled: Boolean = true
// ...
```

**마이그레이션 규칙**

```
if (fixedIncomeMonthly > 0 && employmentIncomeMonthly == 0)
    employmentIncome* ← fixedIncome*
```

---

## 6. 테스트 전략

| ID | 시나리오 | 기대 |
|----|----------|------|
| T30 | 직장 소득 48~60세 | 15%가 아닌 `employmentIncomeTaxRate` 적용 |
| T31 | 사업 소득 + 연금 | 사업 15% + 연금 3.3% 분리 |
| T32 | 부동산 5억 보유 | 재산세·종부세 매년 `annualExpense` 또는 breakdown 반영 |
| T33 | 은퇴 후 연금만 | 건강보험료 > 0 |
| T34 | FixedIncome 마이그레이션 | 직장 소득으로 이관, 수치 동일 |
| T35 | 세금 breakdown 합 | `total == annualTax` |

회귀: Phase 1~4 전체 테스트 PASS 유지.

---

## 7. 구현 순서 (개발 스프린트)

| 스프린트 | 내용 | 산출물 |
|----------|------|--------|
| **P5-S1** | 도메인 모델·마이그레이션·`AnnualTaxBreakdown` | Asset 3종, Mapper, Integrity |
| **P5-S2** | `PropertyHoldingTaxEngine` + 단위 테스트 | T32 |
| **P5-S3** | `HealthInsurancePremiumEngine` + 단위 테스트 | T33 |
| **P5-S4** | `IncomeTaxEngine` + `CashFlowEngine` 통합 | T30~31, T35 |
| **P5-S5** | UI (카드 분리·세금 토글·월표 breakdown) | 실기기 체크리스트 |
| **P5-S6** | PDF·문서·`phase-05-test-report.md` | Phase 5 완료 |

**예상 규모:** 도메인·테스트 ~15파일, UI ~8파일, 기존 테스트 수정 ~10건.

---

## 8. 완료 기준 (Acceptance Criteria)

1. 직장·사업 소득 **분리 입력·저장·복원** 정상
2. 보유 부동산 **재산세·종부세(간이)** 매년 반영, ON/OFF 가능
3. 은퇴 후 **건강보험료(지역가입자 간이)** 반영, ON/OFF 가능
4. 월 순수입 카드·PDF에 **세목별 breakdown** 표시
5. `./gradlew.bat test` 전체 PASS + 실기기 체크리스트 완료
6. PRD·README·`docs/README.md` Phase 5 갱신

---

## 9. 실기기 체크리스트 (2026-06-07 완료)

- [x] 직장·사업 소득 각각 입력 후 차트·월표 갱신
- [x] 재산세 OFF 시 보유 부담 0
- [x] 건강보험 OFF 시 보험료 0, 연금만으로 순현금 변화 확인
- [x] 기존 사용자 데이터 마이그레이션 후 고정수입 값 유지
- [x] PDF 세금 breakdown 가독성
- [x] 부동산 예상 매각가 → 차트·매각 수입·연평균 변동률 표시

테스트 리포트: [`../reports/phase-05-test-report.md`](../reports/phase-05-test-report.md)

---

## 10. 리스크

| 리스크 | 완화 |
|--------|------|
| 세법·건보 요율 변경 | 엔진 상수·테이블 분리, 연도 주석 |
| 사용자 과신 | UI·PDF에 「간이 추정」 고지 |
| FixedIncome 제거 | 마이그레이션 + 1버전 병행 deprecated |

---

## 11. 부가 기능 — 부동산 시세 추정 (`1.4.0`)

매각 예정 연도 입력 시 **예상 매각 가격(총 자산가치)** 을 추가 입력하면, 현재 시세와의 차이로 **연평균 상승·하락률(CAGR)** 을 자동 계산해 매년 반영한다.

| 반영 위치 | 내용 |
|-----------|------|
| `RealEstateProjection` | `r = (예상매각가/현재시세)^(1/년수)−1` |
| 차트 | `illiquidAssets` 연도별 변동 |
| `CashFlowEngine` | 보유세·매각 순수입·양도세 |
| UI | 연평균 ±%/년 · 매각 시 예상 순자산 |
| 저장 | `realEstateExpectedSalePrice` |

---

**상태:** ✅ 완료 (`1.4.0-phase5`) · 실기기 2026-06-07 · 단위 테스트 96건 PASS
