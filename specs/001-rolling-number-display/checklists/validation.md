# Validation Record: 롤링 숫자 표시 컴포넌트

**Date**: 2026-11-10 | **Branch**: `001-rolling-number-display` | **Guide**: [quickstart.md](../quickstart.md)

실제로 실행해 확인한 것과 아직 확인하지 못한 것을 나눠 기록한다. 환경: Windows 11, JDK 21(JBR), Android 에뮬레이터 Pixel 9a API 36(`-gpu swiftshader_indirect`). Mac과 Swift 툴체인은 없음.

## 확인함

| quickstart | 항목 | 결과 |
|-----------|------|------|
| §1 | 공유 테스트 케이스 존재 | `FixtureCoverageTest` 통과: contracts/fixtures.md의 최소 수록 케이스 id가 모두 `fixtures/`에 있음 |
| §2 | Android JVM·Robolectric 테스트 | 전부 통과 (DecimalValue 6, Tokenizer 11, Format 픽스처 API 29·33, Diff 픽스처, Session 픽스처, MotionEngine 7, Continuity 3, Tween 3, RollingNumberFormat 5, FixtureCoverage 1) |
| §2 | Android 기기 테스트 (API 36) | 전부 통과: 애니메이션 중간 프레임·이벤트 1회, 시맨틱 노드 1개, 그룹 이웃 연속 이동, 재구성 없음, Format 픽스처 |
| §4 | 플랫폼 동등성 | Android 쪽만 확인. iOS 미실행 |
| §5 #1 | 첫 진입 시 애니메이션 없음 | 샘플 앱에서 확인 (API 36) |
| §5 #2 | 쿠폰 적용 시 할인액·최종가 변경, 한 줄 요약 정렬 | 샘플 앱에서 확인 (-₩990, ₩11,910) |
| §5 #3 | 수량 증가 시 자릿수 증가 | 샘플 앱에서 최종 값 확인. 중간 프레임은 기기 테스트(`RollingNumberAnimationTest`)로 확인 |
| §5 #5 | 로케일 전환 | de-DE에서 `9.900 ₩`으로 즉시 바뀜 확인 |
| §6 접근성 트리 | 라벨+최종 값만 노출 | uiautomator로 `최종가 ₩11,910`, `할인액 -₩990` 확인 |

## 아직 확인하지 못함

| 항목 | 이유 | 필요한 것 |
|------|------|-----------|
| §3 iOS 테스트 전체, iOS 샘플 앱 | Mac 없음. iOS 코드는 작성만 됨 | macOS에서 `xcodebuild test` (CI `ios` 잡) |
| §2 API 24·29·30 실제 기기 | 해당 에뮬레이터 이미지 없음. 레거시 경로는 Robolectric API 29로만 확인 | API 24 이미지 설치 후 `connectedDebugAndroidTest` |
| §5 #4 쿠폰 연타 체감 | 소프트웨어 렌더링 에뮬레이터라 체감 판단 불가. 연속성은 단위 테스트로 확인 | 실제 기기 |
| §6 실제 TalkBack/VoiceOver 낭독 (T077, T093) | 사람이 들어야 함. Android는 `announceForAccessibility`로 바꿈(research R7) | 실제 기기에서 TalkBack 켜고 확인 |
| §6 #5~#7 동작 줄이기 | 판단 규칙은 session 픽스처로 확인. 실제 설정 토글은 미확인 | 기기에서 "애니메이션 삭제" 켜고 확인 |
| §7 리스트 재사용 스크롤 (T104) | 규칙은 `us6-control.json`으로 확인. 실제 스크롤 미확인 | 기기에서 "주문 내역" 탭 스크롤 |
| §8 성능 (SC-002, T106·T107) | 에뮬레이터 수치는 의미 없음 | 실제 중급 기기에서 Macrobenchmark, Mac에서 Instruments |
| Maven Central 실제 배포 | 계정·서명 키 없음. 로컬 staging 저장소 생성은 확인 | GitHub secrets `SIGNING_KEY`, `SIGNING_PASSWORD`, `CENTRAL_TOKEN` |

## 확인 중 발견한 것

- 에뮬레이터 기본 GPU 모드에서 모든 앱 화면이 검게 나왔다(Settings 포함). `-gpu swiftshader_indirect`로 해결. 앱 문제 아님.
- 라이브러리가 Compose 타입(`Modifier`, `TextStyle`, `Dp`)을 공개 API에 쓰는데 `implementation`으로 선언되어 있었다. 배포 시 소비자 컴파일이 깨질 수 있어 `api`로 바꿨다.
