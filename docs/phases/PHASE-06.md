# Phase 6 — 복수 부동산 + 주택/비주택 보유세 (설계 초안)

> **상태:** ✅ 개발 완료 (실기기 검증 대기)  
> **버전:** `1.5.0-phase6`  
> **선행:** [PHASE-05.1](PHASE-05.1.md)  
> **작성일:** 2026-06-07

---

## 1. 목표

부동산 자산을 **2건 이상** 입력하고, **주택·비주택**을 구분해 보유세(재산세·종부세)를 합산·분리 계산한다.

### 배경 (Phase 5 한계)

| 현재 | 한계 |
|------|------|
| 부동산 카드 1장 | 자가+임대·다주택 시나리오 불가 |
| `PropertyHoldingTaxEngine` | 순자산 **합산** × 단일 세율 |
| `isPrimaryResidence` | **양도세** 전용, 보유세 미반영 |

---

## 2. 범위 (제안)

### 2.1 포함 (In Scope)

| ID | 항목 | MVP |
|----|------|-----|
| P6-01 | 부동산 **추가·삭제** (최대 3건) | UI + 엔진 |
| P6-02 | 유형: **거주 주택** / **비주택**(임대·상가) | enum |
| P6-03 | 재산세 **유형별 세율** | 주택 0.25%, 비주택 0.4% 근사 |
| P6-04 | 종부세 **합산 순자산** | 기존 6억 공제·0.6% 유지 |
| P6-05 | DataStore **v3** 마이그레이션 | 기존 1건 → 목록 1건 |
| P6-06 | 차트·월 부과·PDF·가이드 갱신 | |

### 2.2 후속 (Out of Scope — MVP)

- 1세대 1주택 종부세 특례·공시가격
- 지역별·공제별 재산세 정밀
- 대출 원리금 상환 스케줄

---

## 3. 도메인 설계 (초안)

```kotlin
enum class RealEstateCategory {
    PRIMARY_RESIDENCE,   // 거주 주택
    NON_RESIDENTIAL,       // 비주택
}

data class RealEstate(
    val id: String,           // uuid
    val category: RealEstateCategory,
    val currentValue: Long,
    // ... 기존 필드 유지
)
```

### 보유세 산식 (간이)

```
재산세 = Σ (각 부동산 순자산 × 유형별 propertyTaxRate)
종부세 = max(0, Σ순자산 − threshold) × comprehensiveTaxRate
```

양도세·주택연금·이주 시나리오는 **매각 연도·id** 기준으로 기존 로직 확장.

---

## 4. UI 설계 (초안)

- 자산 영역 「부동산」 카드 → **목록** (카드당 1건)
- 「+ 부동산 추가」 (3건 상한)
- 유형: `FilterChip` 거주 주택 / 비주택
- 이용 가이드 §6 「복수 부동산」 절 추가

---

## 5. 저장·마이그레이션

```kotlin
// v3: 단일 필드 deprecated → 리스트
@Serializable
data class PersistedRealEstate(
    val id: String,
    val category: String,
    val value: Long,
    // ...
)

val realEstates: List<PersistedRealEstate> = emptyList()
// 마이그레이션: realEstateValue > 0 → listOf(legacy one)
```

---

## 6. 구현 스프린트 (예정)

| 스프린트 | 내용 |
|----------|------|
| P6-S1 | 모델·enum·Mapper v3·마이그레이션 |
| P6-S2 | `PropertyHoldingTaxEngine` 유형별 분리 |
| P6-S3 | `CashFlowEngine` 복수 매각·illiquid 합산 |
| P6-S4 | UI 목록·추가·삭제 |
| P6-S5 | 테스트·가이드·PDF·문서 |

**예상 규모:** ~18파일

---

## 7. 완료 기준

1. 주택 1 + 비주택 1 입력·저장·복원
2. 월 부과·PDF에 유형별 breakdown (선택)
3. 기존 1건 사용자 데이터 마이그레이션
4. 단위 테스트 PASS + 실기기 체크리스트

---

*테스트 리포트: [`phase-06-test-report.md`](../reports/phase-06-test-report.md)*
