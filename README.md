# RollingNumber

결제·장바구니 화면의 가격처럼 바뀌는 숫자를, 바뀐 자리만 굴려서 보여주는 Android(Jetpack Compose)·iOS(SwiftUI) 컴포넌트입니다. 쿠폰 적용, 수량 변경, 배송비 변동 때 사용자가 금액이 **얼마나, 어느 방향으로** 바뀌었는지 바로 알 수 있게 합니다.

- 값이 오르면 위로, 내리면 아래로 굴러갑니다. 바뀐 자리만 움직입니다.
- 자릿수가 늘거나 줄면(9,900 → 12,900) 새 자리가 같은 방향으로 굴러 들어오고, 전체 폭이 부드럽게 바뀝니다.
- 애니메이션 도중 값이 또 바뀌어도 지금 위치에서 끊김 없이 이어집니다.
- 통화·퍼센트·축약(1.2K, 1.2천)·소수 자리·반올림을 각 OS의 로케일 규칙으로 표시합니다.
- 스크린리더에는 최종 값만 한 번 알리고, 동작 줄이기 설정을 따릅니다.

| 플랫폼 | 최소 버전 | 배포 |
|--------|-----------|------|
| Android | 7.0 (API 24) | Maven Central `io.github.komodgn:rollingnumber` |
| iOS | 15 | Swift Package Manager (이 저장소) |

## 설치

**Android** (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("io.github.komodgn:rollingnumber:<version>")
}
```

**iOS**: Xcode > File > Add Package Dependencies에 이 저장소 URL을 넣고 `RollingNumber`를 추가합니다.

## 사용법

값과 포맷만 주면 됩니다.

```kotlin
// Android
RollingNumber(value = total, format = RollingNumberFormat.currency("KRW"))
```

```swift
// iOS
RollingNumber(total, format: .currency(code: "KRW"))
```

값은 금액 오차가 없도록 10진 타입을 권장합니다(Android `BigDecimal`, iOS `Decimal`). `Int`·`Long`·`Double`도 받습니다.

### 포맷

| 포맷 | 예 |
|------|----|
| `number(minFraction, maxFraction, grouping, rounding)` | `1,234.5` |
| `currency(code, …)` | `₩12,900`, `$1,234.56`, `1.234,56 €` |
| `percent(minFraction, maxFraction, rounding)` | 0.125 → `12.5%` |
| `compact(maxFraction, rounding)` | `1.2K`, `1.2천`, `1.3만` |

로케일을 주지 않으면 기기 로케일을 따릅니다. 앞뒤 문구는 `prefix`/`suffix`로 붙입니다(예: `suffix = "/월"`).

### 옵션

| 옵션 | Android (`RollingNumber` 매개변수) | iOS (수정자) |
|------|-----------------------------------|--------------|
| 회전 방향 | `trend = RollingTrend.Auto / AlwaysUp / AlwaysDown / PerDigit` | `.rollingTrend(.auto)` |
| 곡선·켜고 끄기·동작 줄이기 무시·첫 표시 애니메이션 | `motion = RollingNumberDefaults.motion(...)` / `RollingMotion(...)` | `.rollingMotion(RollingMotion(...))` |
| 글꼴·크기·색·굵기 | `style = RollingNumberDefaults.style(textStyle)` | `.font(...)`, `.foregroundColor(...)` |
| 고정폭 숫자 | `RollingNumberStyle(tabularDigits = true)` | `.rollingTabularDigits(true)` |
| 위아래 페이드 마스크 | `RollingNumberStyle(fadeMaskHeight = 4.dp)` | `.rollingFadeMask(height: 4)` |
| 리스트 항목 식별자 | `itemKey = order.id` | `.rollingItemKey(order.id)` |
| 접근성 라벨 | `accessibilityLabel = "최종가"` | `.rollingAccessibilityLabel("최종가")` |
| 변경 알림 끄기 | `announceChanges = false` | `.rollingAnnouncements(false)` |
| 시작/종료 이벤트 | `onAnimationStart`, `onAnimationEnd` | `.onRollingAnimationStart {}`, `.onRollingAnimationEnd {}` |

기본 곡선은 스프링이며 약 0.3~0.6초 안에 멈춥니다.

### 그룹

원가·할인액·최종가처럼 같이 바뀌는 숫자는 그룹으로 묶습니다. 같이 시작하고, 한 숫자가 넓어지면 옆 숫자도 같은 곡선으로 밀리며, 스크린리더는 "할인액 -₩1,900, 최종가 ₩12,900"처럼 한 번에 묶어 읽습니다. 그룹은 배치를 정하지 않으므로 `Row`/`HStack` 등은 그대로 쓰면 됩니다.

```kotlin
RollingNumberGroup {
    Row {
        RollingNumber(discount, krw, accessibilityLabel = "할인액")
        RollingNumber(total, krw, accessibilityLabel = "최종가")
    }
}
```

```swift
RollingNumberGroup {
    HStack {
        RollingNumber(discount, format: krw).rollingAccessibilityLabel("할인액")
        RollingNumber(total, format: krw).rollingAccessibilityLabel("최종가")
    }
}
```

## 접근성과 동작 줄이기

- 숫자 하나는 접근성 요소 하나입니다. "라벨 + 최종 값"만 노출하고, 굴러가는 중간 값은 노출하지 않습니다.
- 값이 바뀌면 애니메이션이 멈춘 뒤 최종 값을 한 번 알립니다. 빠르게 여러 번 바뀌면 마지막 값만 알립니다.
- OS의 동작 줄이기(Android "애니메이션 삭제", iOS "동작 줄이기")가 켜져 있으면 애니메이션 없이 바로 바뀝니다. `respectReduceMotion = false`로 무시할 수 있습니다.

## 저장소 구조

| 경로 | 내용 |
|------|------|
| `android/rollingnumber` | Android 라이브러리 |
| `android/sample` | Android 데모 (쿠폰 결제, 주문 내역) |
| `android/benchmark` | 프레임 성능 측정 (Macrobenchmark) |
| `Package.swift`, `Sources/`, `Tests/` | iOS Swift Package |
| `samples/ios/CouponCheckout` | iOS 데모 (XcodeGen) |
| `fixtures/` | 두 플랫폼이 함께 쓰는 JSON 테스트 케이스 |
| `specs/001-rolling-number-display` | 스펙, 설계, 작업 목록 |

두 플랫폼은 같은 규칙을 각자 구현하고, `fixtures/`의 같은 테스트 케이스를 통과해 동작이 같음을 확인합니다. 빌드·테스트 방법은 [quickstart](specs/001-rolling-number-display/quickstart.md)에 있습니다.

## 라이선스

정해지지 않았습니다. (`android/rollingnumber/build.gradle.kts`의 POM에는 Apache 2.0이 임시로 들어가 있습니다. 배포 전에 확정하고 `LICENSE` 파일을 추가하세요.)
