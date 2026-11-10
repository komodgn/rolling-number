# Quickstart: 빌드·테스트·검증 절차

**Plan**: [plan.md](./plan.md) | **Contracts**: [contracts/](./contracts/) | **Fixtures**: [contracts/fixtures.md](./contracts/fixtures.md)

이 문서는 기능이 스펙대로 동작하는지 확인하는 실행 안내서다. 구현 코드는 담지 않는다.

## 준비물

| 대상 | 필요 |
|------|------|
| Android | JDK 17 이상, Android Studio(최신 안정판), Android SDK, 에뮬레이터 또는 기기: **API 24, 29, 30, 최신** (포맷 폴백 경계 검증용). 에뮬레이터 화면이 검게 나오면 `-gpu swiftshader_indirect`로 다시 띄운다 |
| iOS | macOS + Xcode(최신 안정판), **iOS 15 시뮬레이터 런타임** + 최신 시뮬레이터 |
| 공통 | git. Windows PC에서는 Android만 가능하고 iOS 단계는 Mac 또는 CI에서 실행 |

## 1. 공유 테스트 케이스 확인

```bash
ls fixtures/format fixtures/diff fixtures/session
```

기대: [fixtures.md의 최소 수록 케이스](./contracts/fixtures.md#최소-수록-케이스-스펙-엣지-케이스수용-시나리오-전부)가 모두 있다.

## 2. Android 자동 테스트

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 쓴다.

```bash
# 로직 단위 테스트 + diff/session 픽스처 + Robolectric 포맷 픽스처
./gradlew :android:rollingnumber:testDebugUnitTest

# 기기 테스트: format 픽스처(API 24/29/30/최신 각각), Compose UI·시맨틱 테스트
./gradlew :android:rollingnumber:connectedDebugAndroidTest
```

기대:
- `diff/`, `session/` 케이스 100% 통과.
- `format/` 케이스가 API 24·29(폴백 경로)와 API 30·최신(`NumberFormatter` 경로)에서 모두 통과.

## 3. iOS 자동 테스트 (macOS)

```bash
xcodebuild test -scheme RollingNumber \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=latest'
xcodebuild test -scheme RollingNumber \
  -destination 'platform=iOS Simulator,name=iPhone 13,OS=15.5'
```

기대: `diff/`, `session/`, `format/` 케이스 100% 통과. 기기 이름과 OS 버전은 설치된 시뮬레이터에 맞춘다.

## 4. 플랫폼 동등성 판정 (SC-003)

두 플랫폼 테스트는 실패 시 케이스 `id`를 출력한다. CI는 PR마다 2번과 3번을 모두 돌린다.

- **통과 기준**: 두 플랫폼 모두 같은 픽스처 파일 집합을 100% 통과. `"platforms"` 제한 케이스는 `reason`이 있어야 한다.

## 5. 샘플 앱: 쿠폰 적용 결제 화면

```bash
./gradlew :android:sample:installDebug          # Android
cd samples/ios/CouponCheckout && xcodegen generate && open CouponCheckout.xcodeproj   # iOS (macOS, XcodeGen 필요)
```

두 앱에서 같은 순서로 확인한다.

| # | 조작 | 기대 결과 | 스펙 |
|---|------|-----------|------|
| 1 | 앱 첫 진입 | 금액이 애니메이션 없이 바로 보임 | US1-5 |
| 2 | 쿠폰 적용 | 최종가가 줄며 바뀐 자리만 아래로 굴러감. 할인액이 생기며 같은 줄 요소가 순간 이동 없이 밀림 | US1-1, US4-1 |
| 3 | 수량 +로 9,900 → 12,900 만들기 | "1"이 아래에서 굴러 올라오며 나타나고 폭이 부드럽게 넓어짐 | US1-2 |
| 4 | 쿠폰 버튼 빠르게 5번 연타 | 끊김·깜빡임·중복 숫자 없이 마지막 값으로 정착 | US3 |
| 5 | 로케일 메뉴에서 en-US, de-DE 전환 | 굴러가지 않고 즉시 형식만 바뀜("$", "1.234,56 €") | FR-014 |
| 6 | 애니메이션 끄기 토글 후 값 변경 | 즉시 바뀜. 진행 중에 끄면 즉시 완료 | FR-016 |
| 7 | 금액을 음수로 만드는 디버그 버튼 | 부호가 제자리에서 나타나고 숫자는 아래로 굴러감 | 엣지 |

## 6. 접근성 수동 검증 (SC-004, SC-005)

**스크린리더**: Android TalkBack(설정 > 접근성 > TalkBack), iOS VoiceOver(설정 > 손쉬운 사용 > VoiceOver).

| # | 조작 | 기대 결과 |
|---|------|-----------|
| 1 | 최종가에 포커스 후 쿠폰 적용 | 정착 후 "최종가 ₩12,900"처럼 한 번만 읽음. 중간 값 없음 |
| 2 | 쿠폰 연타 | 마지막 값만 한 번 읽음 |
| 3 | 할인액과 최종가가 함께 바뀌는 조작 | "할인액 …, 최종가 …"가 한 번에 이어서 읽힘. 따로 읽히거나 잘리지 않음 |
| 4 | 애니메이션 중 숫자에 포커스 | 목표 값(최종 값)을 읽음 |

> **R7 위험 항목 확인**: 1~3번에서 Android 알림이 전혀 들리지 않으면 숨김 라이브 리전 방식이 실패한 것이다. research R7의 대체 방안(API 35 이하 `announceForAccessibility`)을 적용하고 다시 검증한다.

**동작 줄이기**: Android "개발자 옵션 > 애니메이터 길이 배율 > 애니메이션 사용 안함" 또는 "접근성 > 애니메이션 삭제", iOS "손쉬운 사용 > 동작 > 동작 줄이기".

| # | 조작 | 기대 결과 |
|---|------|-----------|
| 5 | 동작 줄이기 켜고 쿠폰 적용 | 애니메이션 없이 즉시 바뀜 |
| 6 | 샘플 앱의 "동작 줄이기 무시" 토글 켜고 쿠폰 적용 | 일반 애니메이션 재생 |
| 7 | 애니메이션 중 동작 줄이기 켜기 | 진행 중 전환이 즉시 완료 |

## 7. 리스트 재사용 (SC-007)

샘플 앱의 "주문 내역" 탭(숫자 셀 100개, 1초마다 일부 값 변경)에서 빠르게 스크롤한다.

기대: 다른 행의 값이 잠깐 보이는 현상 0건, 셀 재사용 직후 굴러가는 애니메이션 없음, 화면에 보이는 셀의 값 변경은 정상 애니메이션.

## 8. 성능 (SC-002)

```bash
./gradlew :android:benchmark:connectedBenchmarkAndroidTest   # FrameTimingMetric
```

iOS: Xcode > Product > Profile > Animation Hitches로 결제 화면 쿠폰 연타 10초를 측정한다.

기대: 최근 3년 내 중급 기기에서 프레임 드랍 1% 미만, 히치 비율이 Apple 기준 "Good" 범위.
