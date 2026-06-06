# Phase 2 — 테스트 리포트

> **Phase:** 2 — 실시간 시각화 + 대시보드  
> **리포트 작성일:** 2026-06-06  
> **관련 문서:** [`../phases/PHASE-02.md`](../phases/PHASE-02.md)  
> **빌드 버전:** `1.1.0-phase2`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 36건 |
| 자체 테스트 (UI) | **구현 완료** | `OnboardingScreenTest` 1건 (connectedDebugAndroidTest 미실행) |
| 회귀 테스트 | **PASS** | Phase 1 전체 포함 |
| 실기기 테스트 | **대기** | 사용자 검증 필요 |
| **Phase 출시 게이트** | **대기** | 실기기 통과 후 Phase 3 |

---

## 2. 자체 테스트 — 단위 (JUnit)

### 2.1 실행 환경

- OS: Windows 10
- JDK: 17
- Gradle: 8.9
- 명령: `.\gradlew.bat test`
- 실행 일시: 2026-06-06

### 2.2 결과

```
BUILD SUCCESSFUL
총 36건 — PASS 36 / FAIL 0
```

### 2.3 Phase 2 신규·확장 테스트

| ID | 테스트 클래스 | 케이스 | 결과 |
|----|--------------|--------|------|
| T2-01 | `SimulationStateMapperTest` | 저장↔UI 왕복·구버전 마이그레이션 | PASS |
| T2-02 | `CashFlowProjectionTest` | 자산 감소 연도·구간 포맷 | PASS |
| T2-03 | `InvestmentReturnRateTest` | 0.5%p 증감·표기·기본 5% | PASS |
| T19~T22 | `CashFlowEngineTest` | 개인연금·노랑우산·부채·고정수입 | PASS |

### 2.4 회귀 범위

| 범위 | 건수 | 결과 |
|------|------|------|
| `CashFlowEngineTest` | 20 | PASS |
| `AgeBasedPresetTest` | 5 | PASS |
| `CashFlowProjectionTest` | 3 | PASS |
| `FormattedMoneyTest` | 2 | PASS |
| `SimulationStateMapperTest` | 2 | PASS |
| `InvestmentReturnRateTest` | 4 | PASS |
| **합계** | **36** | **PASS** |

---

## 3. 자체 테스트 — UI (Compose)

### 3.1 구현된 테스트

| 테스트 | 파일 | 내용 |
|--------|------|------|
| `onboarding_first_step_shows_age_question` | `OnboardingScreenTest.kt` | 온보딩 1단계 문구·진행 표시 |

### 3.2 실행

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

> 에뮬레이터/실기기 연결 환경에서 실행 필요.

---

## 4. 실기기 테스트

**검증자:** ___________  
**기기:** ___________  
**검증일:** ___________

### 4.1 체크리스트

| # | 항목 | 결과 | 비고 |
|---|------|------|------|
| 1 | 온보딩 3문항 → 대시보드 | [ ] | |
| 2 | Vico 차트 + 마이너스 구간 표시 | [ ] | |
| 3 | 타임라인 빨간색 = 실제 자산 감소 | [ ] | 슬라이더 연동 |
| 4 | 수익률 ±버튼(0.5%p) → 차트 갱신 | [ ] | |
| 5 | 연금 3종·고정수입 카드 입력 | [ ] | |
| 6 | 요약: 수입 부족 vs 자산 감소 구분 | [ ] | |
| 7 | 앱 재실행 → 입력값 복원 | [ ] | DataStore |
| 8 | 뒤로가기 종료 확인 | [ ] | |
| 9 | 회전·다크모드 | [ ] | |

### 4.2 발견 이슈

| ID | 심각도 | 설명 | 조치 | 상태 |
|----|--------|------|------|------|
| — | — | — | — | — |

---

## 5. 결론 및 다음 단계

- Phase 2 자체 테스트: **통과 (36건)**
- Phase 2 게이트: **실기기 대기**
- 실기기 통과 후: Phase 3 (부동산·절세) 착수

---

## 6. 변경 이력

| 일자 | 변경 |
|------|------|
| 2026-06-06 | Phase 2 구현 직후 자체 테스트 리포트 작성 |
| 2026-06-06 | 연금 3종·고정수입·부채·수익률 시뮬 반영, 36건 PASS |
