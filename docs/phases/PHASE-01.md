# Phase 1 — 핵심 계산 엔진 + 기본 입력 UI

> **상태:** 실기기 완료 (피드백 반영 커밋 완료)  
> **PRD 참조:** [`../PRD.md`](../PRD.md) §6 Phase 1  
> **버전:** `1.0.0-phase1`  
> **작성일:** 2026-06-06  
> **최종 갱신:** 2026-06-06

---

## 1. 목표

외부 연동 없이 사용자 입력만으로 **세후 현금흐름**을 연 단위로 산출하는 순수 계산 엔진과 기본 입력 UI를 구축한다.

---

## 2. 범위

### 2.1 포함 (In Scope)

- [x] Domain 모델 (`UserProfile`, `Asset`, `EconomicAssumptions`, `CashFlowProjection`)
- [x] `CashFlowEngine` 순수 계산 로직
- [x] 기본정보 + 4대 자산 입력 Compose UI
- [x] 결과 요약 카드 (고갈 연도, 적자 연도, 최종 자산)
- [x] 단위 테스트 15건 이상

### 2.2 제외 (Out of Scope)

- 차트 시각화 → Phase 2
- DataStore 영속화 → Phase 2
- 만원 단위·나이별 프리셋·부채 입력 → Phase 1 후속 UX 개선 (`174803d`)

---

## 3. 상세 기능 요건

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| P1-01 | 기본 정보 입력 | 현재 나이, 은퇴 연령, 기대 수명, 월 생활비 | P0 |
| P1-02 | 4대 자산 입력 | 부동산, 국민연금, 퇴직연금, 주식, 현금/적금 | P0 |
| P1-03 | 현금흐름 계산 | 물가상승·간이세율 반영 연도별 시뮬레이션 | P0 |
| P1-04 | 결과 요약 | 자산 고갈 시점, 적자 연도, 최종 잔액 | P0 |
| P1-05 | 경제 가정 | 물가상승률 입력 (기본 2%) | P1 |

---

## 4. 기술 설계

### 4.1 패키지

```
com.rebootmap/
├── domain/
│   ├── model/          # UserProfile, Asset, EconomicAssumptions, CashFlowProjection
│   └── engine/         # CashFlowEngine
└── presentation/
    ├── simulation/     # ViewModel, UiState, ResultSummaryCard
    ├── components/     # InputFields, FormattedMoney
    └── theme/          # Material 3 테마
```

### 4.2 계산 규칙 요약

- 시뮬레이션: 현재 연도 ~ 기대 수명 (연 단위)
- 은퇴 전: 생활비 미반영, 자산 축적·성장
- 은퇴 후: 물가 반영 생활비 + 연금·매각·적금 수입 − 간이세금
- 부동산: 매각 연도 일시 유입 (Phase 1 후속: 순자산·부채 반영)

---

## 5. 산출물 체크리스트

- [x] `domain/model/*`
- [x] `domain/engine/CashFlowEngine.kt`
- [x] 단위 테스트 16건 (`CashFlowEngineTest`)
- [x] 기본 입력 Compose 화면
- [x] `ResultSummaryCard`
- [x] `docs/reports/phase-01-test-report.md`

---

## 6. 완료 기준 (Acceptance Criteria)

| AC | 기준 | 결과 |
|----|------|------|
| AC-1 | 4대 자산 입력 시 기대 수명까지 연도별 배열 반환 | ✅ |
| AC-2 | 물가 2% 반영 생활비 증가 검증 | ✅ |
| AC-3 | 단위 테스트 전건 PASS | ✅ |
| AC-4 | 실기기 입력 → 결과 확인 | ✅ (피드백 반영) |

---

## 7. 실기기 테스트 체크리스트

- [x] 기본정보 입력 후 결과 카드 갱신
- [x] 4대 자산 섹션 입력
- [x] 자산 고갈/유지 상태 표시
- [x] 회전·다크모드 레이아웃 (사용자 확인)
- [x] UX 피드백: 만원 단위, 나이별 프리셋, 부채 입력 → `174803d` 반영

**검증자:** 사용자  
**검증일:** 2026-06-06

---

## 8. 커밋 이력

| 커밋 | 일자 | 메시지 |
|------|------|--------|
| `2402d13` | 2026-06-06 | Phase 1 MVP: 노후 현금흐름 계산 엔진 및 기본 입력 UI |
| `174803d` | 2026-06-06 | Phase 1 UX 개선: 만원 단위, 나이별 프리셋, 부동산 부채 |

---

## 9. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06-06 | 초안 작성 (PRD 기반) |
| 2026-06-06 | 실기기 피드백·후속 UX 반영, 문서 분리 |
