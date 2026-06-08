# Phase 9 — 테스트 리포트

> **Phase:** 9 — 대시보드 UX·자산운용 리포트·부동산 타이밍  
> **리포트 작성일:** 2026-06-08  
> **관련 문서:** [`../phases/PHASE-09.md`](../phases/PHASE-09.md)  
> **빌드 버전:** `1.8.0-phase9`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | **136건** (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 9 코드 | **구현 완료** | 그룹·총평·컨설팅·포트폴리오·세금 |
| 실기기 검증 | **⏳ 대기** | 아래 체크리스트 참고 |

---

## 2. Phase 9 신규·갱신 테스트

| 클래스 | 내용 |
|--------|------|
| `CashFlowHighlightPlannerTest` | 이벤트 라벨·은퇴 후 연표 |
| `AssetAdvisoryEngineTest` | 총평 점수·등급 |
| `DashboardGroupSummariesTest` | 그룹 요약 문자열 |
| `RealEstatePortfolioEngineTest` | 취득·일시적 1가구2주택 |
| `Phase9PortfolioStressTest` | 랜덤 부동산 300조합 |
| `YearSnapshotRecurringCashFlowTest` | 월표 정기 수입 분리 |
| `AnnualIncomeBreakdownTest` | 일시·정기 수입 breakdown |

---

## 3. 실기기 체크리스트

- [ ] 대시보드 6개 그룹 접기/펼치기·요약 표시
- [ ] 자산운용 총평 카드 (점수·코멘트)
- [ ] 월표 전환 시점 요약·은퇴 후 연표 펼치기
- [ ] 부동산 — 타이밍 컨설팅·취득 연도·타임라인 드래그
- [ ] 제안 매각 시점 적용 → 차트 A/B
- [ ] PDF 자산운용 리포트 (총평·컨설팅·연표)
- [ ] 이용 가이드 (⋮ 메뉴) 내용 확인

---

## 4. 점검 중 수정 (QA)

| 이슈 | 조치 |
|------|------|
| 시세 0 + 취득 연도만 입력 시 보유로 과다 집계 | `isOwned` — 시세·부채 있을 때만 보유 |
| 취득 > 매각 연도 모델 예외 | 입력 시 매각 해제·역전 방지 |
| 매각 미입력 타임라인 가짜 연도 | 매각 연도 있을 때만 드래그 UI |
