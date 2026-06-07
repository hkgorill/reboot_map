# Phase 7 — 테스트 리포트

> **Phase:** 7 — 주거 로드맵  
> **리포트 작성일:** 2026-06-08  
> **관련 문서:** [`../phases/PHASE-07.md`](../phases/PHASE-07.md)  
> **빌드 버전:** `1.6.0-phase7`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 113건 (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 7 코드 | **구현 완료** | sell/buy 연동·flags·UI·DataStore v4 |
| 실기기 검증 | **⏳ 대기** | 아래 체크리스트 참고 |

---

## 2. Phase 7 신규 테스트

| ID | 클래스 | 내용 |
|----|--------|------|
| P7-T01 | `Phase7RelocationTest` | linked buy — 구입 전 보유세 제외·2주택 flags |
| P7-T02 | `Phase7RelocationTest` | AfterSale 무주택 gap flags |
| P7-T03 | `Phase7RelocationTest` | 다운사이징 60% 프리셋 |
| P7-T04 | `Phase7RelocationTest` | Mapper sell/buy id round-trip |
| P7-T05~06 | `Phase7RelocationTest` | `isConfigured(estates)` |
| — | `CashFlowProjectionTest` | `relocationFlags` 기본값 |

---

## 3. 실기기 체크리스트

- [ ] 주거 로드맵 — 매각·구입 건 선택 후 타임라인 요약 표시
- [ ] 「+ 이주 후 주택 추가」→ 새 부동산 카드·구입 연결
- [ ] 다운사이징 프리셋 → 신규 시세 60% 채움
- [ ] 2주택(매각 전 구입) — 월표 `·2주택`, A/B 차트 차이
- [ ] 앱 재시작 후 sell/buy id 복원
- [ ] PDF — 주거 로드맵 섹션·매각/이주 건 표기
