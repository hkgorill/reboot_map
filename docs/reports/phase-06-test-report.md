# Phase 6 — 테스트 리포트

> **Phase:** 6 — 복수 부동산 + 주거용/비주거용 보유세  
> **리포트 작성일:** 2026-06-08  
> **관련 문서:** [`../phases/PHASE-06.md`](../phases/PHASE-06.md)  
> **빌드 버전:** `1.5.1-phase6`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 106건 (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 6 코드 | **구현 완료** | 복수 부동산·유형별 재산세·DataStore v3 |
| Phase 6 UX (1.5.1) | **구현 완료** | 월표 breakdown·보유세→월세금 통합·입력 보완 |
| 실기기 검증 | **⏳ 대기** | 아래 체크리스트 참고 |

---

## 2. 신규·갱신 테스트

| 클래스 | 내용 | 결과 |
|--------|------|------|
| `Phase6PropertyHoldingTest` | 주거용/비주거용 재산세·종부세 합산 | PASS |
| `RealEstatePersistenceTest` | v3 리스트·레거시 마이그레이션 | PASS |
| `SimulationStateMapperTest` | 복수 부동산 저장-복원 | PASS |
| `CashFlowEngineTest` | `incomeBreakdown` 합계 = `annualIncome` | PASS |
| `Phase5TaxEngineTest` | `PropertyHoldingTaxEngine.Input(estates=...)` | PASS |

회귀: Phase 1~5.1 관련 테스트 PASS 유지.

---

## 3. 구현 범위

### 3.1 핵심 (1.5.0)

- [x] 부동산 최대 10건 추가·삭제 UI
- [x] `RealEstateCategory` — 주거용 / 비주거용
- [x] `PropertyHoldingTaxEngine` — 유형별 재산세·종부세 합산
- [x] DataStore v3 — `realEstates` + 레거시 동기화
- [x] 이용 가이드 §6 · PDF 재산세 breakdown

### 3.2 UX·월표 (1.5.1)

- [x] `AnnualIncomeBreakdown` + 월 수입 탭 breakdown
- [x] 보유세(재산세·종부세) **월 세금에 통합** (열 5개로 단순화)
- [x] 월 세금 탭 — 소득세·건보·재산세·종부세 세목별
- [x] 만원 입력 포커스 중 콤마 제거 (커서 점프 수정)
- [x] 부동산 기본 유형 주거용
- [x] 총자산 안내 문구 중복 제거

---

## 4. 실기기 체크리스트

- [ ] 「+ 부동산 추가」로 2~10건 입력·저장 후 앱 재시작 시 복원
- [ ] 주거용 / 비주거용 전환 시 월 세금·PDF 재산세 항목 분리
- [ ] 월 수입·월 세금 탭 breakdown 확인
- [ ] 부동산 시세 만원 필드 중간 숫자 삽입 시 커서 유지
- [ ] 부동산 삭제 후 빈 카드 1건 유지
- [ ] 이주 시나리오 — 주거용 부동산 매각 연도 우선
- [ ] 기존 1건 사용자 데이터(레거시) 정상 로드
