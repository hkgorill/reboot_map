# Reboot Map — 노후 컨설팅 앱

전 연령층을 위한 노후 현금흐름 시뮬레이션 Android 앱 (MVP)

## 기술 스택

- Kotlin + Jetpack Compose + Material 3
- MVVM, Domain 순수 계산 엔진
- JUnit 단위 테스트

## 개발 Phase

| Phase | 상태 | 내용 |
|-------|------|------|
| 1 | 진행 중 | 계산 엔진 + 기본 입력 UI |
| 2 | 예정 | 실시간 차트 + Progressive UI |
| 3 | 예정 | 부동산·절세 시나리오 |
| 4 | 예정 | 목돈 지출 + PDF 리포트 |

상세 명세: [`docs/PRD.md`](docs/PRD.md)

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

### 실기기 설치

Android Studio에서 `app` 모듈을 실기기에 Run 하거나:

```powershell
.\gradlew.bat installDebug
```

## Phase 1 실기기 테스트 체크리스트

- [ ] 기본정보(나이, 은퇴연령, 생활비) 입력 후 결과 카드 갱신
- [ ] 부동산·연금·투자·적금 각 섹션 입력
- [ ] 자산 고갈/유지 상태 표시 확인
- [ ] 화면 회전·다크모드 레이아웃

테스트 통과 후 Phase 2 착수합니다.
