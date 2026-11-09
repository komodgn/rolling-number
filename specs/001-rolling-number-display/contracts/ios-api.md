# Contract: iOS 공개 API (SwiftUI)

**배포**: Swift Package Manager, 저장소 루트 `Package.swift`, product `RollingNumber` | **플랫폼**: iOS 15+ | **모듈**: `RollingNumber`

시그니처만 정의한다. 구현은 tasks 단계에서 다룬다.

## 최소 사용 (SC-001, FR-024)

```swift
RollingNumber(total, format: .currency(code: "KRW"))
```

## RollingNumber

```swift
public struct RollingNumber: View {
    public init(
        _ value: Decimal,
        format: RollingNumberFormat = .number(),
        locale: Locale? = nil,          // nil = 기기 로케일 (FR-003)
        prefix: String = "",            // FR-004
        suffix: String = ""
    )
    public init(_ value: Int, format: RollingNumberFormat = .number(), locale: Locale? = nil, prefix: String = "", suffix: String = "")
    public init(_ value: Double, format: RollingNumberFormat = .number(), locale: Locale? = nil, prefix: String = "", suffix: String = "")
}
```

글꼴·크기·색상·굵기는 SwiftUI 표준 수정자를 따른다(FR-021): `.font(_:)`, `.fontWeight(_:)`(iOS 16+, iOS 15는 `.font(.title.weight(.bold))`), `.foregroundColor(_:)`.

## 수정자 (View extension)

```swift
extension View {
    func rollingTrend(_ trend: RollingTrend) -> some View                  // FR-005
    func rollingMotion(_ motion: RollingMotion) -> some View               // FR-010, FR-016, FR-017
    func rollingTabularDigits(_ enabled: Bool) -> some View                // FR-021 (기본 true)
    func rollingFadeMask(height: CGFloat) -> some View                     // FR-022 (기본 4, 0 = 없음)
    func rollingItemKey<K: Hashable>(_ key: K) -> some View                // FR-013
    func rollingAccessibilityLabel(_ label: String) -> some View           // FR-019 라벨
    func rollingAnnouncements(_ enabled: Bool) -> some View                // FR-019 (기본 true)
    func rollingGroupOrder(_ order: Int) -> some View                      // 그룹 알림 순서
    func onRollingAnimationStart(_ action: @escaping () -> Void) -> some View   // FR-020
    func onRollingAnimationEnd(_ action: @escaping () -> Void) -> some View
}
```

수정자는 SwiftUI `Environment`로 전달되므로 컨테이너에 한 번 걸면 안의 모든 `RollingNumber`에 적용된다(가까운 쪽 우선).

## RollingNumberFormat

```swift
public struct RollingNumberFormat: Hashable {
    public static func number(minFraction: Int? = nil, maxFraction: Int? = nil, grouping: Bool = true, rounding: RollingRounding = .halfUp) -> Self
    public static func currency(code: String, minFraction: Int? = nil, maxFraction: Int? = nil, grouping: Bool = true, rounding: RollingRounding = .halfUp) -> Self
    public static func percent(minFraction: Int? = nil, maxFraction: Int? = nil, rounding: RollingRounding = .halfUp) -> Self
    public static func compact(maxFraction: Int? = 1, rounding: RollingRounding = .halfUp) -> Self
}
public enum RollingRounding: Hashable { case halfUp, halfEven, down, up }
```

- 잘못된 값은 디버그 빌드에서 `assertionFailure`, 릴리스에서는 가까운 유효 값으로 보정한다(SwiftUI 뷰 생성자가 throw할 수 없어서).

## RollingTrend / RollingMotion

```swift
public enum RollingTrend: Hashable { case auto, alwaysUp, alwaysDown, perDigit }

public struct RollingMotion: Hashable {
    public var layout: RollingCurve
    public var roll: RollingCurve
    public var fade: RollingCurve
    public var enabled: Bool = true
    public var respectReduceMotion: Bool = true
    public var animateFirstDisplay: Bool = false
    public static let `default`: RollingMotion        // research R5 기본 곡선
}

public enum RollingCurve: Hashable {
    case spring(stiffness: Double, dampingRatio: Double)
    case tween(duration: TimeInterval, x1: Double, y1: Double, x2: Double, y2: Double)
}
```

## RollingNumberGroup (FR-015, FR-019a)

```swift
public struct RollingNumberGroup<Content: View>: View {
    public init(@ViewBuilder content: () -> Content)
}
```

- 레이아웃을 강제하지 않는다. 안의 `RollingNumber`가 모두 같은 그룹이 된다(중첩 시 가장 가까운 그룹).

## 동작 보장

| 항목 | 보장 |
|------|------|
| 접근성 | 요소 하나(`children: .ignore`), 라벨 = 접근성 라벨, 값 = 목표 값 전체 문자열 |
| 스레드 | 모든 콜백은 메인 액터 |
| 재평가 | 애니메이션 프레임마다 상위 뷰 body를 재평가하지 않음 |
| 잘못된 값 | `Double.nan`/무한대는 무시하고 마지막 유효 값 유지, 디버그 로그 |
