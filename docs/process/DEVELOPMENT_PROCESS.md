# 개발·테스트·문서 관리 프로세스

> Reboot Map 프로젝트의 Phase 단위 개발 표준 절차

---

## 1. 문서 계층

| 계층 | 문서 | 역할 |
|------|------|------|
| L0 마스터 | `docs/PRD.md` | 제품 비전, 도메인 모델, Phase 개요, 비기능 요건 |
| L1 Phase | `docs/phases/PHASE-NN.md` | 해당 Phase 상세 요건, 산출물, 완료 기준, 체크리스트 |
| L2 테스트 | `docs/reports/phase-NN-test-report.md` | 자체·실기기 테스트 결과 기록 |
| L3 운영 | `README.md` (루트) | 빌드 방법, Phase 현황, 최신 체크리스트 |

**원칙:** PRD는 전체 방향만 유지하고, 구현·테스트 상세는 Phase 문서와 리포트에 분리합니다.

---

## 2. Phase 개발 절차

```
[Phase 착수]
    ↓
PHASE-NN.md 작성/갱신 (요건·산출물·AC 확정)
    ↓
구현 (TDD: domain 테스트 우선)
    ↓
자체 테스트 (.\gradlew.bat test)
    ↓
phase-NN-test-report.md 작성 (자체 테스트 섹션)
    ↓
실기기 테스트 (체크리스트)
    ↓
phase-NN-test-report.md 갱신 (실기기 섹션)
    ↓
README.md 갱신 (현황표·체크리스트·버전)
    ↓
git commit & push
    ↓
[다음 Phase]
```

---

## 3. 자체 테스트 기준

### 3.1 필수 실행 명령

```powershell
cd d:\00_Develop\workspace\reboot_map
.\gradlew.bat test
```

### 3.2 리포트에 포함할 항목

- 실행 일시
- 테스트 총 건수 / PASS / FAIL
- 회귀 범위 (이전 Phase 테스트 포함 여부)
- 신규 추가 테스트 목록
- 빌드 결과 (`BUILD SUCCESSFUL` / `FAILED`)
- 실패 시 원인·조치

### 3.3 회귀 테스트 정책

- **매 Phase 완료 시** 이전 Phase 포함 **전체 단위 테스트** 재실행
- Phase 2+ : Compose UI 테스트 (`androidTest`) 스모크 포함 권장

---

## 4. 실기기 테스트 게이트

- Phase 문서의 **실기기 체크리스트** 전항목 검증
- 결과를 `phase-NN-test-report.md` **실기기 테스트** 섹션에 기록
- 미통과 항목이 있으면 **다음 Phase 착수 금지** (피드백 반영 후 재검증)
- 통과 항목은 `[x]`로 표시, 검증자·일자 기록

### 4.1 일괄 검증 예외 (Phase 3·4)

2026-06-06 기준, Phase 3 코드·자체 테스트 완료 후 **실기기 검증을 Phase 4 구현 완료 시점에 Phase 3·4 항목을 일괄 진행**하기로 조정했습니다.

- Phase 3·4 문서의 실기기 체크리스트에 일괄 검증 안내를 명시
- Phase 4 착수는 Phase 3 **자체 테스트 통과**로 허용 (실기기 게이트는 Phase 4 후 통합)

---

## 5. 커밋 시 문서 갱신 체크리스트

Phase 커밋 전 반드시 확인:

- [ ] `docs/phases/PHASE-NN.md` — 산출물·AC 최종 상태 반영
- [ ] `docs/reports/phase-NN-test-report.md` — 자체·실기기 결과 기록
- [ ] `docs/README.md` — Phase 현황표 갱신
- [ ] `README.md` — Phase 상태, 버전, 체크리스트, 문서 링크 갱신
- [ ] `app/build.gradle.kts` — `versionName` Phase 반영 (해당 시)

### 커밋 메시지 형식

```
Phase N: <한 줄 요약>

<상세 변경 내용 1~2문장. 테스트 건수·문서 경로 언급>
```

---

## 6. 신규 Phase 문서 작성

1. `docs/templates/PHASE_TEMPLATE.md` 복사 → `docs/phases/PHASE-NN.md`
2. `docs/templates/TEST_REPORT_TEMPLATE.md` 복사 → `docs/reports/phase-NN-test-report.md`
3. `docs/README.md` 현황표에 행 추가

---

## 7. 역할 분담

| 활동 | 담당 |
|------|------|
| PRD·Phase 명세 확정 | 기획 + 개발 |
| 구현·자체 테스트 | 개발 (AI + 사용자) |
| 실기기 테스트 | **사용자** |
| 커밋·push | 사용자 요청 시 개발 지원 |
