# Phase 7 — 주거 로드맵 (이주 시나리오 ↔ 부동산 연동)

> **상태:** ✅ 개발 완료 (실기기 검증 대기)  
> **버전:** `1.6.0-phase7`  
> **선행:** [PHASE-06](PHASE-06.md)  
> **작성일:** 2026-06-08

---

## 1. 목표

Phase 6 복수 부동산과 **단절**되어 유명무실해진 「거주지 이동 시나리오」를 **주거 로드맵**으로 재정의하고, 매각·구입 건을 명시적으로 연결한다.

---

## 2. 구현 범위

| ID | 항목 | 내용 |
|----|------|------|
| P7-01 | 매각·구입 연동 | `sellEstateId` / `buyEstateId` — 기존 부동산 카드 선택 또는 가상 신규 주택 |
| P7-02 | 2주택·무주택 구간 | `RelocationYearFlags` — 월표 나이 열·엔진 플래그 |
| P7-03 | A/B 차트 | 「현재 입력 유지」 vs 「주거 로드맵 적용」 범례·비교 |
| P7-04 | 다운사이징 프리셋 | 매각 부동산 시세 60% → `newHomeValue` 자동 입력 |
| P7-05 | DataStore v4 | `relocationSellEstateId` / `relocationBuyEstateId` 저장 |

---

## 3. 도메인

```kotlin
data class RelocationPlan(
    val sellEstateId: String = "",
    val buyEstateId: String = "",  // 비어 있으면 newHomeValue/Debt 가상 주택
    ...
)

data class RelocationYearFlags(
    val isTwoHomeOverlap: Boolean,  // purchaseYear ≤ year < saleYear
    val isGapPeriod: Boolean,       // saleYear < year < purchaseYear (AfterSale)
)
```

**엔진 규칙**

- `linkedBuyEstateId` — 구입 연도 전까지 보유세·순자산 집계에서 제외
- 2주택 양도세 — `sellEstateId` 매각 연도에만 `purchaseYear < saleYear` 시 비과세 상실
- 가상 신규 주택 — `buyEstateId` 없을 때 기존 Phase 3 `newHomeValue`/`newHomeDebt` 유지

---

## 4. UI

- 카드 제목: **주거 로드맵**
- 매각·이주 후 거주 `FilterChip` 선택
- 「+ 이주 후 주택 추가」→ 신규 부동산 카드 + `buyEstateId` 연결
- 한 줄 타임라인 요약 (예: `2032년 부동산 1 매각 → 2031년 부동산 2 구입 · 2주택 1년`)
- 다운사이징 프리셋 버튼
- 월표: 2주택·무주택 구간 나이 열에 `·2주택` / `·무주택` 표시

---

## 5. 완료 기준

1. 매각·구입 id 저장·복원 (Mapper round-trip)
2. linked buy 구입 전 보유세 제외·2주택 flags 정확
3. A/B 차트·PDF·이용 가이드 갱신
4. 단위 테스트 PASS + 실기기 체크리스트

---

*테스트 리포트: [`phase-07-test-report.md`](../reports/phase-07-test-report.md)*
