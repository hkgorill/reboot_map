# Phase 8 — 신용·차용 부채

> **상태:** ✅ 개발 완료 (실기기 검증 대기)  
> **버전:** `1.7.0-phase8`  
> **선행:** [PHASE-07](PHASE-07.md)  
> **작성일:** 2026-06-08

---

## 1. 목표

주택담보 대출(부동산 카드)과 별도로 **개인 신용·차용 부채**를 입력해 순자산·월 현금흐름에 반영한다.

---

## 2. 구현 범위

| ID | 항목 | 내용 |
|----|------|------|
| P8-01 | `PersonalLoan` 도메인 | 잔액·연 이자율·월 상환·상환 종료 연령·유형 |
| P8-02 | `PersonalLoanEngine` | 이자·원금 상환, 미상환 시 이자 가산 |
| P8-03 | `CashFlowEngine` | 지출 반영, 총자산 = 자산 − 부채 잔액 |
| P8-04 | UI | 최대 5건, 「+ 신용·차용 부채 추가」 |
| P8-05 | DataStore v5 | `personalLoans: List<PersistedPersonalLoan>` |
| P8-06 | 월표·PDF·가이드 | 대출 상환 breakdown, 순자산 안내 |

---

## 3. 도메인

```kotlin
data class PersonalLoan(
    val balance: Long,
    val annualInterestRate: Double,
    val monthlyPayment: Long,
    val repaymentEndAge: Int = 0,
    val category: PersonalLoanCategory, // BANK_CREDIT, PRIVATE_LOAN, OTHER
)
```

**엔진 규칙**

- `annualExpense +=` 연간 원리금 상환
- `endingBalance = (유동 + 비유동) − Σ부채잔액`
- 월 상환 = 0 → 연 이자만 부담, 미납 이자는 잔액 가산
- 건보 산정 시 금융재산에서 부채 잔액 차감 (근사)

---

## 4. UI

- **입력 순서:** 「+ 신용·차용 부채 추가」 버튼 → 클릭 시 **아래**에 부채 N 카드 (부동산 섹션과 동일 패턴)
- 부동산 카드 블록과 연금·투자 등 **기타 자산** 블록 사이에 배치
- 유형: 신용대출 / 지인 차용 / 기타
- 다운사이징·DSR·카드 리볼빙은 범위 외 (후속 검토)

### UX 보완 (8.1)

- **부동산** — 「+ 부동산 추가」 버튼을 부동산 카드 **위**로 이동 (부채 섹션과 동일)
- 대시보드 자산 영역: 부동산(버튼→카드) → 연금·투자 등 → 부채(버튼→카드)

---

## 5. 완료 기준

1. 부채 입력·저장·복원
2. 순자산·상환·잔액 감소 반영
3. 월표 대출 상환 표시
4. 단위 테스트 PASS + 실기기 체크리스트

---

*테스트 리포트: [`phase-08-test-report.md`](../reports/phase-08-test-report.md)*
