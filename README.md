# Reboot Map — 노후 컨설팅 앱



전 연령층을 위한 노후 현금흐름 시뮬레이션 Android 앱 (MVP)



**현재 버전:** `1.4.1-phase5.1`  

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

| 5 | ✅ 완료 | `1.4.0` | [PHASE-05](docs/phases/PHASE-05.md) | [리포트](docs/reports/phase-05-test-report.md) |

| 5.1 | ✅ 완료 | `1.4.1` | [PHASE-05.1](docs/phases/PHASE-05.1.md) | — |

| 6 | ✅ 코드 완료 | `1.5.1` | [PHASE-06](docs/phases/PHASE-06.md) | [리포트](docs/reports/phase-06-test-report.md) |

| 7 | ✅ 코드 완료 | `1.6.0` | [PHASE-07](docs/phases/PHASE-07.md) | [리포트](docs/reports/phase-07-test-report.md) |

| 8 | ✅ 코드 완료 | `1.7.0` | [PHASE-08](docs/phases/PHASE-08.md) | [리포트](docs/reports/phase-08-test-report.md) |



> **다음:** Phase 8 실기기 검증



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



### Phase 5 (완료)



- **소득 3종** — 직장·사업·기타 고정수입 (기존 고정수입 마이그레이션)

- **세금·보험 간이** — 재산세·종부세·건보·장기요양 (ON/OFF)

- **월 순수입 카드** — 생활비·부과·세금(세목별)·순현금·총자산 전년 대비

- **PDF** — 연령별 현금흐름·세목 breakdown

- **부동산 시세 추정** — 예상 매각가 → 연평균 상승·하락률, 차트·매각 수입 반영



### Phase 5.1 (완료)



- **이용 가이드** — ⋮ 메뉴 → 팝업 (사용법·계산식·예시)

- **월표 보조 설명** — 총자산 증감 ≠ 월 순현금×12 안내 (마일스톤 연령 하단 1회)



### Phase 6 (코드 완료)



- **복수 부동산** — 최대 10건 추가·삭제, DataStore v3 (`realEstates`)

- **유형** — 주거용 / 비주거용 (`FilterChip`), 재산세 0.25% / 0.4%

- **월 순수입 카드** — 월 수입·월 세금(보유세 포함) 탭 시 항목별 breakdown

- **만원 입력** — 포커스 중 콤마 미적용으로 중간 삽입 시 커서 유지

- **PDF·가이드** — 주거용/비주거용 재산세, 통합 세금 breakdown



### Phase 7 (코드 완료)



- **주거 로드맵** — 매각·이주 후 거주 부동산 명시적 연결 (기존 「거주지 이동」 승격)

- **2주택·무주택 구간** — 월표 나이 열·엔진 flags

- **A/B 차트** — 현재 입력 vs 주거 로드맵 적용

- **다운사이징 프리셋** — 매각 시세 60% → 신규 주택 시세

- **DataStore v4** — `relocationSellEstateId` / `relocationBuyEstateId`



### Phase 8 (코드 완료)



- **신용·차용 부채** — 최대 5건, 주택담보와 별도 입력

- **순자산** — 총자산에서 부채 잔액 차감

- **월 상환** — 원리금 연간 지출·잔액 감소

- **월표·PDF** — 대출 상환 breakdown

- **대시보드 UX (8.1)** — 부동산·부채 모두 「추가」 버튼이 입력 카드 **위**에 표시



---



## 기술 스택



- Kotlin + Jetpack Compose + Material 3

- MVVM · Domain 순수 계산 엔진 · DataStore

- Vico 차트 · kotlinx-serialization

- JUnit 단위 테스트 (**121건**) · Compose UI 테스트



---



## 로컬 실행



```powershell

.\gradlew.bat test

.\gradlew.bat assembleDebug

```



### 실기기 — 터미널로 앱 설치



USB 디버깅을 켠 뒤, 프로젝트 루트에서 실행합니다.



```powershell

# 기기 연결 확인 (목록에 device 상태여야 함)

adb devices



# 디버그 APK 빌드 + 연결된 실기기에 설치 (권장)

.\gradlew.bat installDebug

```



APK만 직접 설치할 때:



```powershell

.\gradlew.bat assembleDebug

adb install -r app\build\outputs\apk\debug\app-debug.apk

```



설치 후 앱 실행 (패키지: `com.rebootmap`):



```powershell

adb shell am start -n com.rebootmap/.MainActivity

```



> Android Studio 없이도 `adb`(Platform Tools)만 PATH에 있으면 됩니다.  
> `unauthorized`가 보이면 기기에서 USB 디버깅 허용을 눌러 주세요.



### 실기기 — 터미널로 화면 캡처



캡처 파일은 **다운로드 폴더**에 저장하는 예시입니다.



```powershell

# 방법 1: 한 줄 (PC로 바로 저장)

adb exec-out screencap -p > "$env:USERPROFILE\Downloads\screen.png"

```



```powershell

# 방법 2: 기기 저장 후 pull

adb shell screencap -p /sdcard/screen.png

adb pull /sdcard/screen.png "$env:USERPROFILE\Downloads\screen.png"

adb shell rm /sdcard/screen.png

```



파일명에 날짜·시간을 넣을 때:



```powershell

$path = "$env:USERPROFILE\Downloads\screen_$(Get-Date -Format 'yyyyMMdd_HHmmss').png"

adb exec-out screencap -p > $path

Write-Host "저장됨: $path"

```



여러 대 연결 시 `-s <device_serial>` 옵션으로 기기를 지정할 수 있습니다.



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

