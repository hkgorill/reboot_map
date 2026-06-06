# Reboot Map — 문서 인덱스

> 전체 요구사항은 [`PRD.md`](PRD.md)를 기준으로 하며, Phase별 상세·테스트·프로세스는 본 디렉터리에서 관리합니다.

---

## 문서 구조

```
docs/
├── PRD.md                          # 전체 제품 요구사항 (마스터)
├── README.md                       # 본 인덱스
├── process/
│   └── DEVELOPMENT_PROCESS.md      # Phase 개발·테스트·커밋 프로세스
├── phases/
│   ├── PHASE-01.md                 # Phase 1 상세 명세
│   ├── PHASE-02.md                 # Phase 2 상세 명세
│   ├── PHASE-03.md                 # Phase 3 상세 명세
│   └── PHASE-04.md                 # Phase 4 상세 명세 (예정)
├── reports/
│   ├── phase-01-test-report.md     # Phase 1 자체 테스트 리포트
│   ├── phase-02-test-report.md     # Phase 2 자체 테스트 리포트
│   └── phase-03-test-report.md     # Phase 3 자체 테스트 리포트
└── templates/
    ├── PHASE_TEMPLATE.md           # Phase 상세 문서 템플릿
    └── TEST_REPORT_TEMPLATE.md     # 테스트 리포트 템플릿
```

---

## Phase 진행 현황

| Phase | 상세 문서 | 자체 테스트 | 실기기 테스트 | 커밋 |
|-------|-----------|-------------|---------------|------|
| 1 | [PHASE-01](phases/PHASE-01.md) | [리포트](reports/phase-01-test-report.md) | 완료 | `2402d13`, `174803d` |
| 2 | [PHASE-02](phases/PHASE-02.md) | [리포트](reports/phase-02-test-report.md) (36건 PASS) | ✅ 통과 | `9935a38` |
| 3 | [PHASE-03](phases/PHASE-03.md) | [리포트](reports/phase-03-test-report.md) (48건 PASS) | Phase 4 후 일괄 | (본 커밋) |
| 4 | [PHASE-04](phases/PHASE-04.md) | — | Phase 3·4 일괄 예정 | — |

---

## Phase 3 주요 기능 (코드 완료 · 2026-06-06)

- 거주지 이동 시나리오 · 양도소득세 · 주택연금 · A/B 비교 차트
- Hint 기반 입력: 온보딩 3항목만 실제 값, 연령대 평균은 `참고: …` hint
- 입력 초기화(⋮) · 자동 재계산(요약·차트·타임라인)
- 단위 테스트 48건 PASS

---

## Phase 2 주요 기능 (구현 완료)

- 온보딩 3문항 · 대시보드 · Vico 차트 · DataStore
- 자산 8종: 부동산, 국민연금, 퇴직·개인·노랑우산 연금, 투자, 현금·적금, **고정수입**
- 투자 수익률 시뮬레이션 (0~15%, ±0.5%p)
- 요약 지표: 자산 고갈 · **실제 자산 감소** · **수입 부족** · 부채 누적(마이너스)
- 앱 종료 확인 (뒤로가기)

---

## 워크플로우 요약

1. **착수** — `PRD.md` + `phases/PHASE-NN.md` 기준으로 개발
2. **자체 테스트** — `.\gradlew.bat test` 실행 후 `reports/phase-NN-test-report.md` 작성
3. **실기기 테스트** — Phase 문서 내 체크리스트 검증, 결과를 리포트에 반영  
   *(Phase 3·4: Phase 4 완료 후 일괄 검증 — 2026-06-06 조정)*
4. **커밋** — Phase 단위 커밋 + 루트 `README.md` 현황·체크리스트 갱신

상세 절차: [`process/DEVELOPMENT_PROCESS.md`](process/DEVELOPMENT_PROCESS.md)
