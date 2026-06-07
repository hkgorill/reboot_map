# Phase 6 — 테스트 리포트

> **Phase:** 6 — 복수 부동산 + 주택/비주택 보유세  
> **리포트 작성일:** 2026-06-07  
> **관련 문서:** [`../phases/PHASE-06.md`](../phases/PHASE-06.md)  
> **빌드 버전:** `1.5.0-phase6`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 106건 (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 6 코드 | **구현 완료** | 복수 부동산·유형별 재산세·DataStore v3 |
| 실기기 검증 | **⏳ 대기** | 아래 체크리스트 참고 |

---

## 2. Phase 6 신규·갱신 테스트

| ID | 클래스 | 내용 | 결과 |
|----|--------|------|------|
| — | `Phase6PropertyHoldingTest` | 주택/비주택 재산세 분리·종부세 합산 | PASS |
| — | `RealEstatePersistenceTest` | v3 리스트·레거시 단일 필드 마이그레이션 | PASS |
| — | `SimulationStateMapperTest` | 복수 부동산 저장-복원 | PASS |
| T32 | `Phase5TaxEngineTest` | `Input(estates=...)` API 갱신 | PASS |

회귀: Phase 1~5.1 관련 테스트 PASS 유지.

---

## 3. 구현 범위

- [x] 부동산 최대 3건 추가·삭제 UI
- [x] `RealEstateCategory` — 거주 주택 / 비주택 (`FilterChip`)
- [x] `PropertyHoldingTaxEngine` — 유형별 재산세·종부세 합산
- [x] `CashFlowEngine` — 복수 매각·보유세 라인
- [x] DataStore v3 — `realEstates: List<PersistedRealEstate>` + 레거시 동기화
- [x] 이용 가이드 §6 복수 부동산 반영
- [x] PDF — 주택/비주택 재산세 breakdown

---

## 4. 실기기 체크리스트

- [ ] 「+ 부동산 추가」로 2~3건 입력·저장 후 앱 재시작 시 복원
- [ ] 거주 주택 / 비주택 전환 시 월 부과·PDF 재산세 항목 분리
- [ ] 부동산 삭제 후 빈 카드 1건 유지
- [ ] 이주 시나리오 — 거주 주택 매각 연도 우선 표시
- [ ] 기존 1건 사용자 데이터(레거시) 정상 로드
