# Reboot Map — 문서 인덱스

> 전체 요구사항은 [`PRD.md`](PRD.md)를 기준으로 하며, Phase별 상세·테스트·프로세스는 본 디렉터리에서 관리합니다.

---

## Phase 진행 현황

| Phase | 상세 문서 | 자체 테스트 | 실기기 테스트 | 커밋 |
|-------|-----------|-------------|---------------|------|
| 1 | [PHASE-01](phases/PHASE-01.md) | [리포트](reports/phase-01-test-report.md) | ✅ 완료 | `2402d13`, `174803d` |
| 2 | [PHASE-02](phases/PHASE-02.md) | [리포트](reports/phase-02-test-report.md) (36건) | ✅ 완료 | `9935a38` |
| 3 | [PHASE-03](phases/PHASE-03.md) | [리포트](reports/phase-03-test-report.md) (48건) | 1차 탐색·보완 | `34a9b10` |
| 4 | [PHASE-04](phases/PHASE-04.md) | [리포트](reports/phase-04-test-report.md) (52건) | 1차 탐색·보완 | `af6dc05` |
| 5 | [PHASE-05](phases/PHASE-05.md) | [리포트](reports/phase-05-test-report.md) (96건) | ✅ 완료 | `fab6c8d` |
| 5.1 | [PHASE-05.1](phases/PHASE-05.1.md) | — | 확인 권장 | (본 작업) |
| 6 | [PHASE-06](phases/PHASE-06.md) | [리포트](reports/phase-06-test-report.md) | ⏳ 대기 | — |
| 7 | [PHASE-07](phases/PHASE-07.md) | [리포트](reports/phase-07-test-report.md) | ⏳ 대기 | — |
| 8 | [PHASE-08](phases/PHASE-08.md) | [리포트](reports/phase-08-test-report.md) (121건) | ⏳ 대기 | — |
| 9 | [PHASE-09](phases/PHASE-09.md) | [리포트](reports/phase-09-test-report.md) (136건) | ⏳ 대기 | (본 작업) |

---

## MVP 기능 요약 (Phase 1~5.1)

- 노후 현금흐름 시뮬 · 자산 입력 · Hint · DataStore
- 거주지 이동 · 양도세 · 주택연금 · A/B 차트
- 목돈 지출 타임라인 · 자산 매칭 · PDF 리포트 공유
- 세금·소득 분리 · 부동산 시세 추정(CAGR)
- **이용 가이드 팝업** · 월표 보조 설명

---

## Phase 6~9 요약

- **6** — 복수 부동산·월표 breakdown·DataStore v3
- **7** — 주거 로드맵 도메인·2주택 구간 (UI는 9에서 컨설팅으로 대체)
- **8** — 신용·차용 부채·순자산·대시보드 추가 버튼 UX
- **9** — 그룹 UI·자산운용 총평·이벤트 월표·PDF 리포트·부동산 타이밍·취득세·중개료

---

## 다음 단계

1. **Phase 9** 실기기 검증 — 그룹·총평·타이밍 컨설팅·PDF ([`PHASE-09.md`](phases/PHASE-09.md))
2. Phase 6~8 실기기 체크리스트 (미완 시)
3. Phase 3·4 정식 실기기 일괄 검증 (미완 시)

**개발 Flow (신규 프로젝트 공통):** [`process/DEVELOPMENT_FLOW_GUIDE.md`](process/DEVELOPMENT_FLOW_GUIDE.md)  
**Reboot Map 운영 세부:** [`process/DEVELOPMENT_PROCESS.md`](process/DEVELOPMENT_PROCESS.md)
