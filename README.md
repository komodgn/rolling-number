# RollingNumber

**English** | [한국어](README.ko.md)

An Android (Jetpack Compose) and iOS (SwiftUI) component that displays changing numbers — like prices on checkout and cart screens — by rolling only the digits that actually change. When a coupon is applied, the quantity changes, or shipping is recalculated, users immediately see **how much** the amount changed and **in which direction**.

- Values roll up when they increase and down when they decrease. Only the changed digits move.
- When the number of digits grows or shrinks (9,900 → 12,900), new digits roll in from the same direction and the overall width changes smoothly.
- If the value changes again mid-animation, it continues seamlessly from its current position.
- Currency, percent, compact (1.2K, 1.2천), fraction digits, and rounding are formatted with each OS's locale rules.
- Screen readers hear the final value once, and the component respects the reduce-motion setting.

| Platform | Minimum version | Distribution |
|----------|-----------------|--------------|
| Android | 7.0 (API 24) | Maven Central `io.github.komodgn:rollingnumber` |
| iOS | 15 | Swift Package Manager (this repository) |

## Installation

**Android** (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("io.github.komodgn:rollingnumber:<version>")
}
```

**iOS**: In Xcode, go to File > Add Package Dependencies, enter this repository's URL, and add `RollingNumber`.

## Usage

Just pass a value and a format. A single `RollingNumber` is enough on its own — grouping is optional.

```kotlin
// Android
RollingNumber(value = total, format = RollingNumberFormat.currency("KRW"))
```

```swift
// iOS
RollingNumber(total, format: .currency(code: "KRW"))
```

Use a decimal type to avoid rounding errors on money (Android `BigDecimal`, iOS `Decimal`). `Int`, `Long`, and `Double` are also accepted.

### Formats

| Format | Example |
|--------|---------|
| `number(minFraction, maxFraction, grouping, rounding)` | `1,234.5` |
| `currency(code, …)` | `₩12,900`, `$1,234.56`, `1.234,56 €` |
| `percent(minFraction, maxFraction, rounding)` | 0.125 → `12.5%` |
| `compact(maxFraction, rounding)` | `1.2K`, `1.2천`, `1.3만` |

Without a locale, the device locale is used. Leading and trailing text is added with `prefix`/`suffix` (e.g. `suffix = "/mo"`).

### Options

| Option | Android (`RollingNumber` parameters) | iOS (modifiers) |
|--------|--------------------------------------|-----------------|
| Roll direction | `trend = RollingTrend.Auto / AlwaysUp / AlwaysDown / PerDigit` | `.rollingTrend(.auto)` |
| Curve, on/off, ignore reduce-motion, animate first display | `motion = RollingNumberDefaults.motion(...)` / `RollingMotion(...)` | `.rollingMotion(RollingMotion(...))` |
| Font, size, color, weight | `style = RollingNumberDefaults.style(textStyle)` | `.font(...)`, `.foregroundColor(...)` |
| Tabular (fixed-width) digits | `RollingNumberStyle(tabularDigits = true)` | `.rollingTabularDigits(true)` |
| Top/bottom fade mask | `RollingNumberStyle(fadeMaskHeight = 4.dp)` | `.rollingFadeMask(height: 4)` |
| List item identity | `itemKey = order.id` | `.rollingItemKey(order.id)` |
| Accessibility label | `accessibilityLabel = "Total"` | `.rollingAccessibilityLabel("Total")` |
| Disable change announcements | `announceChanges = false` | `.rollingAnnouncements(false)` |
| Start/end events | `onAnimationStart`, `onAnimationEnd` | `.onRollingAnimationStart {}`, `.onRollingAnimationEnd {}` |

The default curve is a spring that settles in about 0.3–0.6 s. Each digit slides from its previous glyph to its next glyph; it does not count through intermediate values (an odometer-style count-up, e.g. 2, 3 … 39 for 1 → 40, is not performed).

### Groups

Numbers that change together — such as subtotal, discount, and total — can be wrapped in a group. Grouping is **optional** and is not needed for a single number. When grouped, members start together, neighbors are pushed with the same curve as one number widens, and the screen reader reads them as one merged announcement (e.g. "Discount -₩1,900, Total ₩12,900"). A group does not impose layout, so you keep using `Row`/`HStack` as usual.

```kotlin
RollingNumberGroup {
    Row {
        RollingNumber(discount, krw, accessibilityLabel = "Discount")
        RollingNumber(total, krw, accessibilityLabel = "Total")
    }
}
```

```swift
RollingNumberGroup {
    HStack {
        RollingNumber(discount, format: krw).rollingAccessibilityLabel("Discount")
        RollingNumber(total, format: krw).rollingAccessibilityLabel("Total")
    }
}
```

## Accessibility and reduce motion

- Each number is a single accessibility element. It exposes only "label + final value", never the intermediate rolling values.
- When a value changes, the final value is announced once after the animation settles. If it changes rapidly several times, only the last value is announced.
- When the OS reduce-motion setting is on (Android "Remove animations", iOS "Reduce Motion"), values change instantly without animation. You can override this with `respectReduceMotion = false`.

## Repository layout

| Path | Contents |
|------|----------|
| `android/rollingnumber` | Android library |
| `android/sample` | Android demo (coupon checkout, order history) |
| `android/benchmark` | Frame performance measurement (Macrobenchmark) |
| `Package.swift`, `Sources/`, `Tests/` | iOS Swift Package |
| `samples/ios/CouponCheckout` | iOS demo (XcodeGen) |
| `fixtures/` | JSON test cases shared by both platforms |
| `specs/001-rolling-number-display` | Spec, design, task list |

Both platforms implement the same rules independently and verify parity by passing the same test cases in `fixtures/`. Build and test instructions are in the [quickstart](specs/001-rolling-number-display/quickstart.md).

## Roadmap / possible future work

- **Odometer-style counting.** An opt-in trend that scrolls through intermediate values (e.g. 2, 3 … 39 for 1 → 40) instead of sliding each digit straight from its old glyph to its new one. Not implemented today; it would need the motion engine to generate and scroll the in-between digit sequence. Not a committed promise — tracked here as a direction, not a schedule.

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright © komodgn.
