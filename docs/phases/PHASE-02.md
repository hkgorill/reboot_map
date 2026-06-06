# Phase 2 — 실시간 시각화 + 대시보드

> **상태:** ✅ 완료 (자체·실기기 통과)  
> **PRD 참조:** [`../PRD.md`](../PRD.md) §6 Phase 2  
> **버전:** `1.1.0-phase2`  
> **작성일:** 2026-06-06  
> **최종 갱신:** 2026-06-06

---

## 1. 목표

입력 피로도를 줄이는 **Progressive Disclosure** UI와 **실시간 인터랙티브 차트**로 노후 현금흐름을 직관적으로 시각화한다.

---

## 2. 범위

### 2.1 포함 (In Scope)

- [x] 온보딩 3문항 (나이, 은퇴 연령, 월 생활비)
- [x] 대시보드 (결과 요약 + 차트 + 슬라이더)
- [x] Vico 연도별 자산 추이 라인 차트
- [x] 적자 구간 타임라인 (붉은색 강조)
- [x] 자산 카드 Progressive UI (접기/펼치기)
- [x] 투자 수익률 시뮬레이션 (0~15%, ±0.5%p 버튼, 기본 5%)
- [x] DataStore 입력값 저장·복원
- [x] 앱 종료 확인 (뒤로가기)
- [x] Compose UI 스모크 테스트

### 2.3 Phase 2 후속 개선 (2026-06-06)

- [x] 연금 3종 세분화 (퇴직연금·개인연금·노랑우산공제)
- [x] 고정수입 카드 (임대료·급여, 연령 구간)
- [x] 마이너스 잔액(부채 누적) 반영
- [x] 요약 지표 분리 (수입 부족 vs 실제 자산 감소)
- [x] 차트·타임라인 슬라이더 연동 (`assetDeclineYears`)
- [x] 요약 카드 연도 구간 표기·정렬 개선

### 2.2 제외 (Out of Scope)

- 부동산 시나리오·절세 → Phase 3
- 목돈 타임라인·PDF → Phase 4
- GitHub Actions CI → Phase 2+ 선택

---

## 3. 상세 기능 요건

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| P2-01 | 온보딩 | 3문항 완료 시 프리셋 적용 + 대시보드 진입 | P0 |
| P2-02 | 현금흐름 차트 | Vico 라인 차트, 나이별 총자산(만원) | P0 |
| P2-03 | 적자 강조 | 차트 하단 타임라인 붉은 막대 | P0 |
| P2-04 | 수익률 시뮬레이션 | 슬라이더 + ±버튼(0.5%p), 300ms 내 차트 갱신 | P0 |
| P2-05 | Progressive UI | 자산·기본정보 카드 접기/펼치기 | P1 |
| P2-06 | DataStore | 온보딩 완료 후 상태 저장, 재실행 시 복원 | P0 |
| P2-07 | 연금 3종 | 퇴직·개인·노랑우산 제도별 계산 | P0 |
| P2-08 | 고정수입 | 임대료·급여 연령 구간 수입 | P1 |
| P2-09 | 부채 누적 | 자산 소진 후 마이너스 잔액 표시 | P1 |

---

## 4. 기술 설계

### 4.1 신규 패키지

```
com.rebootmap/
├── data/
│   ├── model/SimulationPersistedState.kt
│   ├── mapper/SimulationStateMapper.kt
│   └── SimulationDataStoreRepository.kt
└── presentation/
    ├── onboarding/OnboardingScreen.kt
    ├── dashboard/DashboardScreen.kt
    ├── chart/CashFlowChart.kt
    ├── navigation/RebootMapApp.kt
    └── components/ExpandableCard.kt
```

### 4.2 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Vico compose-m3 | 1.13.1 | 라인 차트 |
| DataStore Preferences | 1.1.1 | 로컬 저장 |
| kotlinx-serialization-json | 1.7.3 | 상태 JSON 직렬화 |

### 4.3 저장·복원 흐름

1. `SimulationViewModel` 상태 변경 → 300ms 디바운스 후 `SimulationStateMapper.toPersisted()`
2. `SimulationDataStoreRepository.save()`
3. 앱 시작 시 `load()` → `toUiState()` → 온보딩 완료 시 대시보드 직행

---

## 5. 산출물 체크리스트

- [x] `OnboardingScreen.kt`
- [x] `DashboardScreen.kt` + `CashFlowChart.kt`
- [x] `SimulationDataStoreRepository.kt`
- [x] `SimulationStateMapper` + 단위 테스트
- [x] `OnboardingScreenTest` (androidTest)
- [x] `docs/reports/phase-02-test-report.md`

---

## 6. 완료 기준 (Acceptance Criteria)

| AC | 기준 | 결과 |
|----|------|------|
| AC-1 | 슬라이더/입력 변경 시 300ms 이내 차트 갱신 | ✅ (구현·자체테스트) |
| AC-2 | 적자 구간 시각적 강조 | ✅ |
| AC-3 | 앱 재시작 후 입력값 복원 | ✅ 실기기 통과 |

---

## 7. 실기기 테스트 체크리스트

- [x] 온보딩 3문항 완료 후 대시보드 진입
- [x] 연도별 자산 차트 + 적자 타임라인 붉은색 표시
- [x] 투자 수익률 ±버튼(0.5%p) 조작 시 차트 즉시 갱신
- [x] 연금 3종·고정수입·요약 지표 구분
- [x] 자산 카드 탭으로 펼치기/접기
- [x] 앱 완전 종료 후 재실행 시 입력값 복원
- [x] 뒤로가기 종료 확인
- [x] 회전·다크모드 레이아웃

**검증자:** 사용자  
**검증일:** 2026-06-06

---

## 8. 커밋 이력

| 커밋 | 일자 | 메시지 |
|------|------|--------|
| (본 커밋) | 2026-06-06 | Phase 2 + 연금 세분화·고정수입·UX 개선 |

---

## 9. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06-06 | Phase 2 상세 문서 작성 |
| 2026-06-06 | 연금 3종·고정수입·부채 누적·수익률 시뮬레이션·요약 정렬 반영 |
| 2026-06-06 | 실기기 테스트 통과, Phase 2 게이트 완료 |
