# 노후 컨설팅 앱 — 최종 PRD (Product Requirements Document)

> **프로젝트명:** Reboot Map (가칭)  
> **플랫폼:** Android (Kotlin)  
> **버전:** `1.1.0-phase2`  
> **작성일:** 2026-06-06  
> **개발 방식:** Phase 단위 개발 → 단위/회귀 테스트 → 실기기 검증 게이트

### 관련 문서

| 문서 | 설명 |
|------|------|
| [`docs/README.md`](README.md) | 문서 인덱스·Phase 현황 |
| [`docs/process/DEVELOPMENT_PROCESS.md`](process/DEVELOPMENT_PROCESS.md) | 개발·테스트·커밋 프로세스 |
| [`docs/phases/PHASE-NN.md`](phases/) | Phase별 상세 명세 |
| [`docs/reports/`](reports/) | Phase별 테스트 리포트 |

---

## 1. 제품 비전

전 연령층이 **직접 입력**만으로 노후 현금흐름을 시뮬레이션하고, 자산 고갈 시점·적자 구간·부동산·목돈 지출 리스크를 **시각적으로 이해**할 수 있는 모바일 컨설팅 앱.

- 마이데이터 연동 없음 (MVP)
- 정확한 금융 로직 + 입력 피로도 최소화 UI
- 각 Phase 완료 후 **실기기 테스트 통과** 후 다음 Phase 진행

---

## 2. 기술 스택 (개발 최적화)

| 영역 | 선택 | 이유 |
|------|------|------|
| 언어 | **Kotlin** | Android 네이티브, Compose 생태계 |
| UI | **Jetpack Compose + Material 3** | 세련된 UI, 실시간 상태 바인딩 |
| 아키텍처 | **MVVM + Clean Layer** | Domain(순수 로직) / Data / Presentation 분리 |
| 차트 | **Vico** (Phase 2) | Compose 네이티브, 애니메이션 우수 |
| 로컬 저장 | **DataStore** (Phase 2+) | 경량 설정·입력값 영속화 |
| PDF | **Android Print / HTML-PDF** (Phase 4) | 리포트 출력 |
| 테스트 | **JUnit5 + Kotest + Compose UI Test** | 단위·회귀·UI 테스트 |

### 패키지 구조

```
com.rebootmap/
├── domain/          # 순수 계산 로직, 모델 (Android 의존성 없음)
├── data/            # Repository, DataStore (Phase 2+)
├── presentation/    # Compose UI, ViewModel
└── di/              # Hilt (Phase 2+)
```

---

## 3. 핵심 도메인 모델

### 3.1 사용자 프로필 (`UserProfile`)

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| currentAge | Int | 40 | 현재 나이 |
| retirementAge | Int | 60 | 목표 은퇴 연령 |
| lifeExpectancy | Int | 90 | 기대 수명 |
| monthlyLivingExpense | Long | 3,000,000 | 목표 월 생활비 (원) |

### 3.2 경제 가정 (`EconomicAssumptions`)

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| inflationRate | Double | 0.02 | 연 물가상승률 |
| pensionIncomeTaxRate | Double | 0.033 | 연금소득세 간이세율 (MVP) |
| generalIncomeTaxRate | Double | 0.15 | 일반 소득 간이세율 (MVP) |

### 3.3 자산 (`Asset` — sealed class)

| 유형 | 설명 |
|------|------|
| `RealEstate` | 시세·부채·매각 연도 (순자산 기준 매각) |
| `NationalPension` | 예상 월 수령액·수령 시작 연령 |
| `SeverancePension` | 퇴직연금 (DC·IRP) — 적립·운용수익·은퇴 후 균등 인출 |
| `PersonalPension` | 개인연금 (연금저축·IRP) — 55세~ 수령 개시 |
| `YellowUmbrella` | 노랑우산공제 — 공제이자 복리·일시금 수령 |
| `Investment` | 주식·재테크 — 복리 성장·적자 시 인출 |
| `CashSavings` | 현금·적금 — 만기 일시 유입 |
| `FixedIncome` | 고정수입 (임대료·급여) — 연령 구간별 월 수입 |

기본 투자 수익률: **5%** (`InvestmentDefaults.DEFAULT_RETURN_RATE`)

### 3.4 시뮬레이션 결과 (`CashFlowProjection`)

| 필드 | 설명 |
|------|------|
| yearlySnapshots | 연도별 스냅샷 리스트 |
| depletionYear | 총자산 **0 이하** 최초 연도 (null = 기대 수명까지 유지) |
| deficitYears | **수입 부족** 연도 — 연금·기타수입 < 생활비+세금 (투자 수익 미반영) |
| assetDeclineYears() | **실제 자산 감소** 연도 — 전년 대비 총자산 감소 (투자·연금 잔액 반영) |

**연도별 스냅샷 (`YearSnapshot`)**

| 필드 | 설명 |
|------|------|
| year, age | 연도, 나이 |
| totalAssets | 총 자산 |
| annualIncome | 세전 연 수입 |
| annualExpense | 연 생활비 (물가 반영) |
| annualTax | 연 세금 |
| netCashFlow | 세후 순현금흐름 |
| endingBalance | 연말 잔액 |

---

## 4. 계산 엔진 명세 (Phase 1 핵심)

### 4.1 시뮬레이션 범위

- 시작: `currentYear` (시스템 연도)
- 종료: `lifeExpectancy`에 해당하는 연도
- 시간 단위: **연 단위** (MVP)

### 4.2 연간 수입 계산

1. **국민연금:** `startAge` 이후 `monthlyPayout × 12` (연금소득세)
2. **퇴직연금:** 납입 종료 전 적립+운용(3%), 은퇴 후 잔액 균등 인출
3. **개인연금:** 납입 종료 전 적립+운용(3%), `payoutStartAge` 이후 균등 인출
4. **노랑우산:** 공제이자(3.3%) 복리, `payoutAge`에 일시금 (일반소득세)
5. **투자:** 연 복리 성장, 적자 시 우선 인출
6. **현금/적금:** `maturityYear`에 일시 유입
7. **부동산:** `saleYear`에 순자산 일시 유입
8. **고정수입:** `startAge~endAge` 구간 `monthlyAmount × 12` (일반소득세)

### 4.3 연간 지출 계산

```
annualExpense(year) = monthlyLivingExpense × 12 × (1 + inflationRate)^(year - baseYear)
```

- `retirementAge` 이전: 생활비 미반영 (MVP: 근로 소득 가정 없음, 자산 축적만)
- `retirementAge` 이후: 위 공식 적용

### 4.4 세금 계산 (MVP 간이 모델)

| 소득 유형 | 세율 |
|-----------|------|
| 연금 수입 | `pensionIncomeTaxRate` (3.3%) |
| 투자 수익 인출분 | `generalIncomeTaxRate` (15%) |
| 부동산 매각 대금 | Phase 3에서 양도세 정밀 계산, MVP는 0% |

```
annualTax = Σ(incomeType × applicableRate)
netCashFlow = annualIncome - annualExpense - annualTax
```

적자 시 투자·연금·현금 순으로 인출, **미충당 적자는 부채(마이너스 잔액)로 누적**.

### 4.5 경고 판정

| 지표 | 기준 |
|------|------|
| **자산 고갈** | `endingBalance <= 0` 최초 연도 |
| **수입 부족** | 은퇴 후 `annualIncome - annualExpense - annualTax < 0` |
| **실제 자산 감소** | 은퇴 후 전년 대비 `endingBalance` 감소 (차트·타임라인 빨간색) |

> 수입 부족 연수와 자산 감소 연수는 다를 수 있음 (투자 수익·연금 인출·적립 잔액 변동 반영).

---

## 5. UI/UX 가이드라인

### 디자인 시스템

- **테마:** Material 3 Dynamic Color + 다크 모드 지원
- **컬러:** Primary `#1B5E7B` (신뢰·안정), Accent `#F4A261` (경고·강조), Surface `#F8FAFC`
- **타이포:** Pretendard 또는 Noto Sans KR
- **카드:** 16dp 라운드, subtle elevation, 섹션별 아이콘

### Phase별 UI 범위

| Phase | 화면 |
|-------|------|
| 1 | 기본정보 폼 + 자산 입력 폼 + 결과 요약 카드 |
| 2 | 온보딩 + 대시보드 + Vico 차트 + 수익률 시뮬레이션(±0.5%p) + 8종 자산 카드 |
| 3 | 부동산 시나리오 탭 + 절세 비교 + 주택연금 시뮬 |
| 4 | 목돈 타임라인 + 자산 매칭 + PDF 리포트 |

---

## 6. Phase별 개발 계획 및 완료 기준

### Phase 1 — 핵심 계산 엔진 + 기본 입력 UI ✅

> 상세: [`phases/PHASE-01.md`](phases/PHASE-01.md) · 테스트: [`reports/phase-01-test-report.md`](reports/phase-01-test-report.md)

**목표:** Pure Logic 계산 + 기본 입력 → 결과 요약

**산출물**
- [ ] `domain/model/*` 데이터 모델
- [ ] `domain/engine/CashFlowEngine` 순수 계산 클래스
- [ ] 단위 테스트 15건 이상 (엣지 케이스 포함)
- [ ] 기본정보 + 4대 자산 입력 Compose 화면
- [ ] 결과 요약 (고갈 연도, 적자 연도 수, 최종 잔액)

**완료 기준 (Acceptance Criteria)**
1. 국민연금·퇴직연금·투자·적금·부동산 입력 시 100세까지 연도별 배열 반환
2. 물가 2% 반영 시 생활비 증가 검증
3. 모든 단위 테스트 PASS
4. 실기기에서 입력 → 결과 확인 가능

**실기기 테스트 체크리스트**
- [ ] 기본정보 입력 후 저장/계산 동작
- [ ] 각 자산 유형 추가·수정·삭제
- [ ] 결과 화면에 고갈 연도 표시
- [ ] 회전·다크모드 레이아웃 정상

---

### Phase 2 — 실시간 시각화 + 대시보드 ✅ (자체테스트 완료 · 실기기 대기)

> 상세: [`phases/PHASE-02.md`](phases/PHASE-02.md) · 테스트: [`reports/phase-02-test-report.md`](reports/phase-02-test-report.md)

**목표:** Progressive Disclosure + 인터랙티브 차트

**산출물**
- 온보딩 3문항 플로우
- 자산 카드 Progressive UI
- Vico 연도별 현금흐름 차트 (실시간 업데이트)
- 적자 구간 붉은색 강조
- DataStore 입력값 저장
- Compose UI 회귀 테스트

**완료 기준**
1. 슬라이더/입력 변경 시 300ms 이내 차트 갱신
2. 적자 구간 시각적 강조
3. 앱 재시작 후 입력값 복원

---

### Phase 3 — 부동산 유동화 + 절세 시나리오

> 상세: [`phases/PHASE-03.md`](phases/PHASE-03.md)

**목표:** 거주지 이동·양도세·주택연금 시뮬레이션

**산출물**
- 거주지 이동 시뮬레이터 (2주택 상태 포함)
- 양도소득세 계산기 (1세대1주택 비과세 요건)
- 부부 공동명의 종부세/양도세 비교
- 주택연금 월 수령액 시뮬
- 재투자( IRP/주식) 현금흐름 추적

**완료 기준**
1. 양도세 계산 단위 테스트 (국세청 표 기준 샘플 케이스)
2. 시나리오 A/B 비교 차트

---

### Phase 4 — 목돈 지출 + 리포트

> 상세: [`phases/PHASE-04.md`](phases/PHASE-04.md)

**목표:** 생애주기 대형 지출 방어 + PDF 리포트

**산출물**
- 목돈 지출 타임라인 (드래그 앤 드롭)
- 자산 매칭 추천 알고리즘
- PDF 컨설팅 리포트 생성·공유

**완료 기준**
1. 목돈 이벤트 배치 시 현금흐름 재계산
2. PDF 생성 및 공유 동작

---

## 7. 테스트 전략

### 7.1 단위 테스트 (매 Phase)

- **대상:** `domain/` 전체 (Android Framework 무의존)
- **프레임워크:** JUnit5 + Kotest assertions
- **커버리지 목표:** Domain 90%+
- **필수 케이스:**
  - 정상 시나리오 (자산 충분)
  - 자산 고갈 시나리오
  - 물가상승률 0% / 5% 엣지
  - 은퇴 전/후 경계
  - 자산 0개 입력

### 7.2 회귀 테스트 (매 Phase)

- 이전 Phase 테스트 스위트 **전체 재실행**
- Phase 2+: Compose UI Test (스모크)
- CI: `./gradlew test` 로컬 실행 (GitHub Actions Phase 2+)

### 7.3 실기기 게이트

각 Phase 완료 후 사용자 실기기 테스트 → 피드백 반영 → 다음 Phase 착수

---

## 8. 비기능 요건

| 항목 | 목표 |
|------|------|
| APK 크기 | < 15MB (MVP) |
| 계산 응답 | < 100ms (100년 시뮬) |
| 최소 SDK | API 26 (Android 8.0) |
| Target SDK | API 35 |
| 오프라인 | 완전 오프라인 동작 |
| 개인정보 | 모든 데이터 로컬 저장, 서버 전송 없음 |

---

## 9. 리스크 및 완화

| 리스크 | 완화 |
|--------|------|
| 세법 정확성 | Phase 1~2는 간이세율, Phase 3에서 국세청 기준 정밀화 + 테스트 케이스 고정 |
| 입력 피로도 | Phase 2 Progressive Disclosure |
| 계산 오류 | Pure Function + TDD, Golden File 테스트 |

---

## 10. 용어 정의

- **세후 현금흐름:** 연간 수입 − 생활비 − 세금
- **자산 고갈:** 총자산이 0 이하가 되는 최초 시점
- **수입 부족:** 연금·기타수입이 생활비+세금보다 적은 연도
- **실제 자산 감소:** 전년 대비 총자산이 줄어든 연도 (타임라인 빨간색)
- **예상 부채:** 자산 소진 후 미충당 생활비가 누적된 마이너스 잔액

---

## 부록 A: Phase 1 테스트 케이스 목록

| ID | 시나리오 | 기대 결과 |
|----|----------|-----------|
| T01 | 기본 입력만 (자산 없음) | 은퇴 후 지출만, 즉시 고갈 |
| T02 | 현금 1억 + 월 200만 생활비 | 고갈 연도 산출 |
| T03 | 국민연금 150만/월, 65세 시작 | 65세부터 수입 반영 |
| T04 | 퇴직연금 적립 + 인출 | contributionEndAge 경계 검증 |
| T05 | 투자 5억, 7% 수익률 | 복리 성장 검증 |
| T06 | 적금 2028년 3000만 만기 | 해당 연도 유입 |
| T07 | 부동산 2030년 5억 매각 | 해당 연도 유입 |
| T08 | 물가 2% 10년 | 생활비 1.219배 |
| T09 | 복합 자산 시나리오 | 전체 스냅샷 일관성 |
| T10 | lifeExpectancy = retirementAge | 1년 시뮬 |
| T16~T22 | 부채·연금3종·고정수입·자산감소 등 | Phase 2 확장 |

---

*다음 단계: Phase 2 실기기 게이트 → Phase 3 (부동산·절세)*
