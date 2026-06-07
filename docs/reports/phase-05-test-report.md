# Phase 5 — 테스트 리포트

> **Phase:** 5 — 세금 정밀화 + 소득 유형 분리 (+ 부동산 시세 추정)  
> **리포트 작성일:** 2026-06-07  
> **관련 문서:** [`../phases/PHASE-05.md`](../phases/PHASE-05.md)  
> **빌드 버전:** `1.4.0-phase5`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 96건 (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 5 코드 | **구현 완료** | 세금 엔진·소득 3종·월표·PDF breakdown |
| 부동산 시세 추정 | **구현 완료** | 예상 매각가 → 연평균 상승·하락률(CAGR) |
| 실기기 검증 | **✅ 완료** | 2026-06-07, 검증자 확인 |

---

## 2. Phase 5 신규·갱신 테스트

| ID | 클래스 | 내용 | 결과 |
|----|--------|------|------|
| T30~T31 | `Phase5TaxEngineTest` | 소득 유형별 세금·보유세 | PASS |
| T32 | `Phase5TaxEngineTest` | 재산세·종부세 간이 | PASS |
| T33 | `Phase5TaxEngineTest` | 건보·장기요양 간이 | PASS |
| T34 | `Phase5TaxEngineTest` | `fixedIncome*` → `employmentIncome*` 마이그레이션 | PASS |
| T35 | `Phase5TaxEngineTest` | `AnnualTaxBreakdown.totalTax` 합계 | PASS |
| T07b | `CashFlowEngineTest` | 예상 매각가 연도별 순자산·매각 수입 | PASS |
| — | `RealEstateProjectionTest` | CAGR·상승·하락·미입력 유지 | PASS |
| — | `AssetEmploymentIncomeTest` | 직장 소득 정합성 | PASS |
| — | `SimulationIntegrityTest` | 소득 3종·레거시 마이그레이션 | PASS |
| — | `UserScenario48Test` | 48세 실기기 재현 시나리오 | PASS |

회귀: Phase 1~4 관련 `CashFlowEngineTest` 등 기존 테스트 PASS 유지.

---

## 3. 구현 범위

### 3.1 세금·소득 (Phase 5 핵심)

- [x] `EmploymentIncome` · `BusinessIncome` · `OtherFixedIncome` (기존 `FixedIncome` 제거)
- [x] `IncomeTaxEngine` — 연금 3.3% · 근로 6% · 사업 15% · 기타 15%
- [x] `PropertyHoldingTaxEngine` — 재산세·종부세 (ON/OFF)
- [x] `HealthInsurancePremiumEngine` — 지역가입자 간이 (ON/OFF)
- [x] `CashFlowEngine` — `annualLivingExpense` / `annualHoldingCost` / `taxBreakdown` 통합
- [x] DataStore v2 — `employmentIncome*` · 세금 가정 필드 · `fixedIncome*` 1회 이관
- [x] `MonthlyCashFlowSummaryCard` — 월 생활비·부과·세금(탭 시 세목별)·순현금
- [x] `SimulationPdfExporter` — 연령별 현금흐름·세목 breakdown
- [x] 대시보드 `TaxAssumptionSection` — 재산세·종부세·건보 토글

### 3.2 부동산 시세 추정 (1.4.0 부가)

- [x] `expectedSalePrice` — 매각 예정 연도 시 예상 매각가(총 자산가치) 입력
- [x] `RealEstateProjection` — CAGR 산식, 연도별 시세·순자산 추정
- [x] 차트 `illiquidAssets` · 보유세 · 매각 수입·양도세·목돈 매칭에 반영
- [x] UI — 연평균 ±%/년 · 매각 시 예상 순자산 미리보기

**산식:** `r = (예상매각가 ÷ 현재시세)^(1÷년수) − 1`, 부채는 고정·시세만 변동

### 3.3 Phase 4 연계 개선 (동일 릴리스 포함)

- [x] 생활비 물가 기준 (`LivingExpenseInflationBase`) — 기본 은퇴 시점
- [x] 국민연금 물가연동 · 퇴직·개인연금 운용수익률 · 노랑우산 공제이자
- [x] 차트 그리기 순서 (총자산 → 비유동 → 유동)
- [x] 월 순수입 카드 — 월 세금·총자산 전년 대비 안내

---

## 4. 실기기 테스트 (2026-06-07)

| # | 항목 | 결과 | 비고 |
|---|------|------|------|
| 1 | 직장·사업 소득 각각 입력 후 차트·월표 갱신 | ✅ | |
| 2 | 재산세 OFF → 보유 부담 0 | ✅ | |
| 3 | 건강보험 OFF → 보험료 0 | ✅ | |
| 4 | 기존 데이터 `fixedIncome*` → 직장 소득 이관 | ✅ | |
| 5 | PDF 세금 breakdown 가독성 | ✅ | |
| 6 | 월 세금 탭 → 세목별 펼침 | ✅ | |
| 7 | 부동산 예상 매각가 → 차트 비유동선·매각 연도 수입 | ✅ | |
| 8 | 연평균 상승·하락률 UI 표시 | ✅ | |

---

## 5. 알려진 한계 (간이 모델)

- 세율·건보 요율은 2024년 근사치, 실제 신고·공제와 차이 가능
- 부동산 시세는 단순 CAGR, 지역·주택 유형·대출 상환 미반영
- UI·PDF에 「간이 추정」 고지 문구 포함

---

## 6. 다음 단계

- Phase 3·4 정식 실기기 일괄 검증 (미완 시)
- 후속: 대출 상환 스케줄·지역별 재산세율 등 (별도 Phase 검토)
