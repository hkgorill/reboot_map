# Reboot Map — 노후 컨설팅 앱

전 연령층을 위한 노후 현금흐름 시뮬레이션 Android 앱 (MVP)

**현재 버전:** `1.1.0-phase2`  
**마스터 요건:** [`docs/PRD.md`](docs/PRD.md)

---

## 문서 관리

| 문서 | 용도 |
|------|------|
| [`docs/PRD.md`](docs/PRD.md) | 전체 제품 요구사항 (마스터) |
| [`docs/README.md`](docs/README.md) | 문서 인덱스·Phase 현황 |
| [`docs/process/DEVELOPMENT_PROCESS.md`](docs/process/DEVELOPMENT_PROCESS.md) | 개발·테스트·커밋 프로세스 |
| [`docs/phases/`](docs/phases/) | Phase별 상세 명세 |
| [`docs/reports/`](docs/reports/) | Phase별 테스트 리포트 |

**워크플로우:** Phase 개발 → 자체 테스트 리포트 → 실기기 검증 → README·문서 갱신 → 커밋

---

## 개발 Phase 현황

| Phase | 상태 | 버전 | 상세 | 테스트 리포트 |
|-------|------|------|------|---------------|
| 1 | ✅ 완료 | `1.0.0` | [PHASE-01](docs/phases/PHASE-01.md) | [리포트](docs/reports/phase-01-test-report.md) |
| 2 | 🔄 실기기 대기 | `1.1.0` | [PHASE-02](docs/phases/PHASE-02.md) | [리포트](docs/reports/phase-02-test-report.md) |
| 3 | 📋 예정 | — | [PHASE-03](docs/phases/PHASE-03.md) | — |
| 4 | 📋 예정 | — | [PHASE-04](docs/phases/PHASE-04.md) | — |

---

## 주요 기능 (Phase 2)

- **온보딩** 3문항 → 연령대별 프리셋 적용
- **대시보드** 결과 요약 + Vico 연도별 자산 차트 + 현금흐름 타임라인
- **자산 8종** 부동산, 국민연금, 퇴직·개인·노랑우산 연금, 투자, 현금·적금, 고정수입(임대료·급여)
- **투자 수익률 시뮬레이션** 0~15%, ±0.5%p 버튼, 기본 5%
- **지표 분리** 수입 부족 vs 실제 자산 감소 vs 자산 고갈, 마이너스 잔액(부채) 표시
- **DataStore** 입력값 저장·복원, 앱 종료 확인

---

## 기술 스택

- Kotlin + Jetpack Compose + Material 3
- MVVM · Domain 순수 계산 엔진 · DataStore
- Vico 차트 · kotlinx-serialization
- JUnit 단위 테스트 (36건) · Compose UI 테스트

---

## 로컬 실행

### 사전 요건

- Android Studio Ladybug 이상
- JDK 17
- Android SDK 35

### 빌드 & 테스트

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

### 실기기 UI 테스트 (선택)

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### 실기기 설치

Android Studio에서 `app` 모듈 Run 또는:

```powershell
.\gradlew.bat installDebug
```

---

## Phase별 실기기 체크리스트

### Phase 1 ✅ (완료)

- [x] 기본정보·자산 입력 → 결과 카드 갱신
- [x] 만원 단위·콤마·나이별 프리셋
- [x] 부동산 부채·순자산 계산

### Phase 2 🔄 (검증 대기)

- [ ] 온보딩 3문항 → 대시보드 진입
- [ ] Vico 차트 + 마이너스 구간 + 타임라인(자산 감소)
- [ ] 수익률 ±버튼(0.5%p) → 차트 즉시 갱신
- [ ] 연금 3종·고정수입 카드 입력
- [ ] 요약: 수입 부족 vs 실산 감소 구분 표시
- [ ] 자산 카드 펼치기/접기
- [ ] 앱 재실행 → 입력값 복원
- [ ] 뒤로가기 종료 확인

> 결과는 [`docs/reports/phase-02-test-report.md`](docs/reports/phase-02-test-report.md) §4에 기록

---

## 커밋 이력 (요약)

| 커밋 | Phase | 내용 |
|------|-------|------|
| `2402d13` | 1 | 계산 엔진 + 기본 UI |
| `174803d` | 1 | 만원·프리셋·부채 UX |
| (본 커밋) | 2 | 대시보드·연금3종·고정수입·차트·DataStore |

---

## 저장소

https://github.com/hkgorill/reboot_map
