# Reboot Map — 노후 컨설팅 앱



전 연령층을 위한 노후 현금흐름 시뮬레이션 Android 앱 (MVP)



**현재 버전:** `1.3.0-phase4`  

**마스터 요건:** [`docs/PRD.md`](docs/PRD.md)



---



## 문서 관리



| 문서 | 용도 |

|------|------|

| [`docs/PRD.md`](docs/PRD.md) | 전체 제품 요구사항 (마스터) |

| [`docs/README.md`](docs/README.md) | 문서 인덱스·Phase 현황 |

| [`docs/process/DEVELOPMENT_FLOW_GUIDE.md`](docs/process/DEVELOPMENT_FLOW_GUIDE.md) | **개발 Flow 가이드** (신규 프로젝트 재사용) |
| [`docs/process/DEVELOPMENT_PROCESS.md`](docs/process/DEVELOPMENT_PROCESS.md) | Reboot Map 운영·커밋 세부 규칙 |

| [`docs/phases/`](docs/phases/) | Phase별 상세 명세 |

| [`docs/reports/`](docs/reports/) | Phase별 테스트 리포트 |



**워크플로우:** Phase 개발 → 자체 테스트 → 실기기 검증 → 문서 갱신 → 커밋·push



---



## 개발 Phase 현황



| Phase | 상태 | 버전 | 상세 | 테스트 리포트 |

|-------|------|------|------|---------------|

| 1 | ✅ 완료 | `1.0.0` | [PHASE-01](docs/phases/PHASE-01.md) | [리포트](docs/reports/phase-01-test-report.md) |

| 2 | ✅ 완료 | `1.1.0` | [PHASE-02](docs/phases/PHASE-02.md) | [리포트](docs/reports/phase-02-test-report.md) |

| 3 | ✅ 코드 완료 | `1.2.0` | [PHASE-03](docs/phases/PHASE-03.md) | [리포트](docs/reports/phase-03-test-report.md) |

| 4 | ✅ 코드 완료 | `1.3.0` | [PHASE-04](docs/phases/PHASE-04.md) | [리포트](docs/reports/phase-04-test-report.md) |



> **실기기 테스트:** 2026-06-06 1차 탐색·버그 보완 완료 → **2026-06-07 정식 일괄 검증 예정** (Phase 3·4)



---



## 주요 기능



### Phase 2 (완료)



- 온보딩 3문항 · 대시보드 · Vico 차트 · DataStore · 자산 8종

- 지표 분리 (수입 부족 / 실제 자산 감소 / 자산 고갈 / 부채)



### Phase 3 (코드 완료)



- 거주지 이동 · 양도소득세 · 주택연금 · A/B 비교 차트

- Hint 기반 입력 · 입력 초기화 · 자동 재계산

- 키보드 가림 개선 (`imePadding` + 포커스 스크롤)



### Phase 4 (코드 완료)



- **목돈 지출 타임라인** — 추가·삭제·연도 ◀▶ 조정 · 지출 일정 요약

- **자산 매칭** — 각 목돈 항목 아래 추천 자산 (적금 만기·매각·투자 등)

- **PDF 리포트** — ⋮ 메뉴 «PDF 리포트 공유» (요약·목돈·권고 가이드)



---



## 기술 스택



- Kotlin + Jetpack Compose + Material 3

- MVVM · Domain 순수 계산 엔진 · DataStore

- Vico 차트 · kotlinx-serialization

- JUnit 단위 테스트 (**52건**) · Compose UI 테스트



---



## 로컬 실행



```powershell

.\gradlew.bat test

.\gradlew.bat assembleDebug

```



실기기 설치: Android Studio에서 `app` Run 또는 `.\gradlew.bat installDebug`



---



## Phase 3·4 실기기 체크리스트 (일괄 검증 · 2026-06-07 예정)



### Phase 3



- [ ] 거주지 이동 · 양도세 · 주택연금 · A/B 차트

- [ ] Hint 입력 · 자동 재계산 · 입력 초기화



### Phase 4



- [ ] 목돈 추가·연도 조정·삭제 · 차트·요약 반영

- [ ] 자산 매칭 추천 (목돈 항목별 «추천 자산»)

- [ ] PDF 리포트 공유



> 2026-06-06 1차 확인: 목돈 UI 노출 · 카테고리/PDF/차트 크래시 보완 완료  

> 상세: [`docs/reports/phase-04-test-report.md`](docs/reports/phase-04-test-report.md) §4



---



## 커밋 이력 (요약)



| 커밋 | Phase | 내용 |

|------|-------|------|

| `2402d13` | 1 | 계산 엔진 + 기본 UI |

| `174803d` | 1 | 만원·프리셋·부채 UX |

| `9935a38` | 2 | 대시보드·연금3종·고정수입·차트 |

| `34a9b10` | 3 | 부동산·절세·hint 입력 |

| `5e4bead` | — | 키보드 가림 개선 |

| (본 커밋) | 4 | 목돈·매칭·PDF + 실기기 보완 |



---



## 저장소



https://github.com/hkgorill/reboot_map

