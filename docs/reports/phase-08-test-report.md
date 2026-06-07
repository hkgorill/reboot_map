# Phase 8 — 테스트 리포트

> **Phase:** 8 — 신용·차용 부채  
> **리포트 작성일:** 2026-06-08  
> **관련 문서:** [`../phases/PHASE-08.md`](../phases/PHASE-08.md)  
> **빌드 버전:** `1.7.0-phase8`

---

## 1. 요약

| 구분 | 결과 | 비고 |
|------|------|------|
| 자체 테스트 (단위) | **PASS** | 121건 (`.\gradlew.bat :app:testDebugUnitTest`) |
| Phase 8 코드 | **구현 완료** | PersonalLoan·엔진·UI·DataStore v5 |
| 실기기 검증 | **⏳ 대기** | 아래 체크리스트 참고 |

---

## 2. Phase 8 신규 테스트

| ID | 클래스 | 내용 |
|----|--------|------|
| — | `PersonalLoanEngineTest` | 이자·원금·미상환·상환 종료 |
| P8-T01 | `Phase8PersonalLoanTest` | 부채 잔액 순자산 차감 |
| P8-T02 | `Phase8PersonalLoanTest` | 월 상환 현금흐름·잔액 감소 |
| P8-T03 | `Phase8PersonalLoanTest` | Mapper round-trip |
| P8-T04 | `Phase8PersonalLoanTest` | 상환 부담 → 고갈 가속 |

---

## 3. 실기기 체크리스트

- [ ] 「+ 부동산 추가」·「+ 신용·차용 부채 추가」 버튼이 각 입력 카드 **위**에 표시
- [ ] 「+ 신용·차용 부채 추가」→ 아래에 부채 카드 표시
- [ ] 잔액·금리·월 상환 입력 → 총자산(차트) 감소
- [ ] 월표 「월 세금」 탭 → 「대출 상환」
- [ ] 앱 재시작 후 부채 복원
- [ ] PDF 신용·차용 부채 섹션
