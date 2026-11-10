# Contributing

Thanks for your interest in RollingNumber. This library ships the same component on two
platforms, so the most important rule is **parity**: Android and iOS must behave the same.

## Ground rules

- The two platforms implement the same rules independently and must pass the **same** test
  cases in `fixtures/`. If you change behavior, update the fixtures and make both sides pass.
- Keep the public API small and symmetric across platforms. New options should exist on both
  Android and iOS (or neither), with matching names where the language allows.
- By contributing you agree your contribution is licensed under the project's
  [Apache License 2.0](LICENSE).

## Project layout

| Path | Contents |
|------|----------|
| `android/rollingnumber` | Android library (Jetpack Compose) |
| `android/sample`, `android/benchmark` | Android demo and frame benchmark |
| `Package.swift`, `Sources/`, `Tests/` | iOS Swift Package |
| `samples/ios/CouponCheckout` | iOS demo (XcodeGen) |
| `fixtures/` | JSON test cases shared by both platforms |
| `specs/001-rolling-number-display` | Spec, design, task list |

## Building and testing

Full instructions are in the [quickstart](specs/001-rolling-number-display/quickstart.md).

- **iOS**: `swift test` (library). The demo is generated with `xcodegen generate` in
  `samples/ios/CouponCheckout`, then opened in Xcode.
- **Android**: `./gradlew :android:rollingnumber:testDebugUnitTest` for unit tests, and the
  Macrobenchmark (`./gradlew :android:benchmark:connectedBenchmarkAndroidTest`) on a physical
  device for frame timing.

Run the relevant tests on both platforms before opening a pull request.

## Pull requests

1. Branch from `main`.
2. Keep changes focused; describe what changed and why, and note any fixture updates.
3. Make sure tests pass on both platforms and the public API stays in sync.

## Reporting issues

Please include the platform, OS version, the value transition that misbehaves (from → to),
the format/locale in use, and what you expected versus what happened.
