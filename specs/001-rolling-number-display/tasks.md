---

description: "Task list for 롤링 숫자 표시 컴포넌트 (Rolling Number Display)"
---

# Tasks: 롤링 숫자 표시 컴포넌트 (Rolling Number Display)

**Input**: Design documents from `specs/001-rolling-number-display/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: 포함한다. 스펙 SC-003과 사용자 지시("공유 JSON 테스트 케이스로 동등성 검증")가 테스트를 명시적으로 요구한다. 각 스토리에서 테스트·픽스처 작업을 먼저 하고, 실패를 확인한 뒤 구현한다.

**Organization**: 사용자 스토리 단위로 나눈다. 스토리마다 Android와 iOS 작업이 짝으로 있다. 단계 순서는 스펙 우선순위를 따른다: US1(P1) → US2(P1) → US5(P1) → US3(P2) → US4(P2) → US6(P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능 (다른 파일, 끝나지 않은 작업에 의존하지 않음)
- **[Story]**: 해당 사용자 스토리 (US1~US6)

## Path Conventions

| 약칭 | 실제 경로 |
|------|-----------|
| Android 라이브러리 소스 | `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/` |
| Android JVM 테스트 | `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/` |
| Android 기기 테스트 | `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/` |
| iOS 라이브러리 소스 | `Sources/RollingNumber/` |
| iOS 테스트 | `Tests/RollingNumberTests/` |
| 공유 픽스처 | `fixtures/` (형식: [contracts/fixtures.md](./contracts/fixtures.md)) |

작업 설명에는 항상 전체 경로를 쓴다.

**공통 규칙 (모든 작업)**:
- `logic/`(Android)와 `Logic/`(iOS) 폴더의 파일은 Compose, SwiftUI, UIKit, `android.*`, OS 포맷터를 import하지 않는다.
- 같은 이름의 Kotlin/Swift 로직은 [contracts/logic-api.md](./contracts/logic-api.md)와 같은 이름·의미를 쓴다.
- **iOS 작업은 macOS에서만 빌드·테스트할 수 있다.** Windows에서는 iOS 코드를 작성만 하고, 검증은 CI(T008)나 Mac에서 한다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 저장소 구조와 빌드 설정

- [X] T001 Create Gradle root build: `settings.gradle.kts` (include `:android:rollingnumber`, `:android:sample`, `:android:benchmark`), `build.gradle.kts`, `gradle.properties`, Gradle wrapper(`gradlew`, `gradlew.bat`, `gradle/wrapper/`), and `gradle/libs.versions.toml` pinning Kotlin 2.x, AGP, Compose BOM (latest stable), kotlinx-serialization-json, JUnit4, Robolectric, AndroidX Test, Compose UI Test, Macrobenchmark
- [X] T002 Create Android library module `android/rollingnumber/build.gradle.kts`: `com.android.library` + Kotlin + Compose compiler plugin, `namespace = "io.github.komodgn.rollingnumber"`, `minSdk = 24`, compileSdk/targetSdk latest, `kotlin { explicitApi() }`, no third-party runtime dependencies, `testOptions.unitTests.isIncludeAndroidResources = true`, `sourceSets["test"].resources.srcDir(rootProject.file("fixtures"))`, `sourceSets["androidTest"].assets.srcDir(rootProject.file("fixtures"))`, plus `android/rollingnumber/src/main/AndroidManifest.xml`
- [ ] T003 [P] Create root `Package.swift`: `swift-tools-version:5.9`, `platforms: [.iOS(.v15)]`, library product `RollingNumber`, target `RollingNumber` at `Sources/RollingNumber`, test target `RollingNumberTests` at `Tests/RollingNumberTests`, no dependencies; add placeholder `Sources/RollingNumber/RollingNumber.swift` and `Tests/RollingNumberTests/SmokeTests.swift` so the package builds — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T004 [P] Create Android sample app module `android/sample/build.gradle.kts` (`com.android.application`, minSdk 24, depends on `project(":android:rollingnumber")`) with `android/sample/src/main/AndroidManifest.xml` and `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/MainActivity.kt` showing an empty `CheckoutScreen` composable in `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt`
- [ ] T005 [P] Create iOS sample app spec `samples/ios/CouponCheckout/project.yml` (XcodeGen; iOS 15 deployment target; local package dependency `path: ../../`) with `samples/ios/CouponCheckout/Sources/CouponCheckoutApp.swift` and empty `samples/ios/CouponCheckout/Sources/CheckoutView.swift`; document `xcodegen generate` in `samples/ios/CouponCheckout/README.md` — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T006 [P] Create Macrobenchmark module `android/benchmark/build.gradle.kts` (`com.android.test`, `targetProjectPath = ":android:sample"`, minSdk 24) with empty `android/benchmark/src/main/kotlin/io/github/komodgn/rollingnumber/benchmark/CheckoutBenchmark.kt`
- [X] T007 [P] Create `fixtures/README.md` describing the three folders `fixtures/format/`, `fixtures/diff/`, `fixtures/session/`, the common header `{"schema": 1, "kind": ..., "cases": [...]}`, and linking `specs/001-rolling-number-display/contracts/fixtures.md` as the authoritative format
- [X] T008 [P] Create CI workflow `.github/workflows/ci.yml`: job `android` (ubuntu, JDK 17, `./gradlew :android:rollingnumber:testDebugUnitTest`), job `ios` (macos, latest Xcode, `xcodebuild test -scheme RollingNumber` on latest iPhone simulator and an iOS 15 simulator runtime)
- [X] T009 [P] Create `.gitignore` (Gradle, Android Studio, Xcode, SwiftPM `.build/`, `.DS_Store`, `local.properties`) and `.editorconfig` (UTF-8, LF, 4-space indent for `.kt`/`.kts`/`.swift`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 스토리가 쓰는 값 모델, 조각 분해, 기본 포맷(일반·통화), 픽스처 읽기

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 어떤 스토리도 시작할 수 없다

### 픽스처 읽기

- [X] T010 [P] Implement Android fixture models and loader in `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/fixtures/Fixtures.kt`: kotlinx-serialization data classes mirroring `contracts/fixtures.md` (`format`, `diff`, `session` cases), `loadAll(kind)` reading every `*.json` under classpath folder `format/`, `diff/`, `session/`; throw (fail) when `schema != 1`; skip cases whose `platforms` excludes `"android"` only if `reason` is present, otherwise fail
- [X] T011 [P] Implement Android device-test fixture loader in `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/fixtures/AssetFixtures.kt` reading the same JSON from instrumentation assets with the same models and rules as T010 (copy the model file or share via a `testFixtures`-style source set)
- [ ] T012 [P] Implement iOS fixture models and loader in `Tests/RollingNumberTests/Support/Fixtures.swift`: `Decodable` structs mirroring `contracts/fixtures.md`; locate the repo-root `fixtures/` folder relative to `#filePath`; `loadAll(kind:)` decodes every `*.json`; `XCTFail` when `schema != 1`; skip cases whose `platforms` excludes `"ios"` only if `reason` is present — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

### DecimalValue (data-model §1)

- [X] T013 [P] Write Android tests `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/DecimalValueTest.kt`: parse accepts only `-?[0-9]+(\.[0-9]+)?`; `"NaN"`, `"Infinity"`, `""`, `"1e5"`, `"1,000"` return null; `"-0"`/`"-0.00"` → negative=false ("0은 항상 false (-0 금지)"); leading zeros stripped ("앞자리 0 제거(값이 0이면 \"0\")"); trailing fraction zeros stripped ("끝자리 0 제거"); `12.5 == 12.50`; compare order "부호 → 정수 자릿수 길이 → 정수 문자열 → 소수 문자열"; 20-digit values compare correctly
- [ ] T014 [P] Write iOS tests `Tests/RollingNumberTests/Logic/DecimalValueTests.swift` with exactly the same cases as T013 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T015 [P] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/DecimalValue.kt` (`parse`, `compareTo`, `canonical`) to pass T013
- [ ] T016 [P] Implement iOS `Sources/RollingNumber/Logic/DecimalValue.swift` (`parse`, `Comparable`, `canonical`) to pass T014 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

### 조각 분해 (data-model §3~§5)

- [X] T017 [P] Write Android tests `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/TokenizerTest.kt` covering every example row of data-model §5 (`₩12,900`, `-₩300`, `1.234,56 €` with de-DE symbols, `1.2K`, `~₩12,900/월`), U+2212 minus, a literal split by the sign, empty prefix/suffix producing no `AP`/`AS` token, and `normalize()` output (`1.234,56 €` → `1,234.56`)
- [ ] T018 [P] Write iOS tests `Tests/RollingNumberTests/Logic/TokenizerTests.swift` with exactly the same cases as T017 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T019 [P] Implement Android types in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/FormattedNumber.kt`: `NumberSymbols(decimalSeparator, groupingSeparator, minusSigns)`, `FormattedNumber(text, value, symbols, prefix, suffix, formatKey)`, `TokenKind` (`int`, `frac`, `decimal`, `group`, `sign`, `literal`, `prefix`, `suffix`), `Token(kind, text, key)`
- [ ] T020 [P] Implement iOS types in `Sources/RollingNumber/Logic/FormattedNumber.swift` mirroring T019 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T021 Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/Tokenizer.kt` (`tokenize`, `normalize`) following the key rules verbatim: 숫자 영역 = 첫 숫자~마지막 숫자; 소수점 `D`; 정수 자리 `I0`부터 왼쪽으로; 소수 자리 `F0`부터 오른쪽으로; 구분자 `G{k}` (k = 오른쪽 정수 자리 수); 부호 `S` (한 문자열에 하나); 리터럴 앞쪽 `Lp0`… 바깥 방향, 뒤쪽 `Ls0`…, 부호는 세지 않음; 접두 `AP`, 접미 `AS`; text with no digit is an error (depends on T019)
- [ ] T022 Implement iOS `Sources/RollingNumber/Logic/Tokenizer.swift` with the same rules as T021 (depends on T020) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

### 기본 포맷: 일반·통화 (FR-002, FR-003 일부)

- [X] T023 [P] Implement Android public `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingNumberFormat.kt` per `contracts/android-api.md` (`number`, `currency`, `percent`, `compact`, `RollingRounding`); validation: "`minFractionDigits ≤ maxFractionDigits`, 둘 다 0~15. 어기면 생성 시 예외", "통화 코드는 ISO 4217 3글자 대문자. 플랫폼이 모르는 코드면 예외" → `IllegalArgumentException`; internal `formatKey(locale)` string; `equals` by formatKey fields
- [ ] T024 [P] Implement iOS public `Sources/RollingNumber/RollingNumberFormat.swift` per `contracts/ios-api.md` with the same fields; invalid values → `assertionFailure` in debug and clamp to nearest valid value in release; internal `formatKey(locale:)` — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T025 Implement Android formatter in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/format/IcuFormatter.kt` (interface `format(value: BigDecimal, spec, locale): FormattedNumber` + factory choosing by `Build.VERSION.SDK_INT >= 30`), `format/ModernIcuFormatter.kt` (`android.icu.number.NumberFormatter`, styles number & currency), `format/LegacyIcuFormatter.kt` (`android.icu.text.NumberFormat.getInstance` / `getCurrencyInstance`); symbols from `android.icu.text.DecimalFormatSymbols` (monetary separators for currency); force Latin digits with `numbers=latn` `ULocale` keyword (depends on T019, T023)
- [ ] T026 Implement iOS formatter in `Sources/RollingNumber/Format/FoundationFormatter.swift`: `Decimal.FormatStyle` (number) and `Decimal.FormatStyle.Currency` (currency); symbols from a `NumberFormatter` with the same locale/style (`decimalSeparator`/`currencyDecimalSeparator`, `groupingSeparator`/`currencyGroupingSeparator`, `minusSign`); force Latin digits with `@numbers=latn` locale (depends on T020, T024) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T027 Create `fixtures/format/currency-basic.json` with cases `krw-basic` (ko-KR, KRW, 12900 → normalized `12,900`), `usd-basic` (en-US, USD, 1234.56 → `1,234.56`), `eur-de` (de-DE, EUR, 1234.56 → `1,234.56`, literal `Ls0` "€"), `jpy-basic` (ja-JP, JPY, 12900), `number-basic` (en-US, number, 1234567.891 default fraction), each with full expected token list derived from data-model §5
- [X] T028 [P] Write Android Robolectric format fixture runner `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/format/FormatFixtureTest.kt`: for every `format/` case run `IcuFormatter` → `Tokenizer`, compare `normalized` exactly and tokens with the comparison rules of `contracts/fixtures.md` §1 (literal text compared after removing U+0020/U+00A0/U+202F unless `textMatch: exact`); run once with `@Config(sdk = [29])` and once with `@Config(sdk = [33])` to cover both formatter paths
- [X] T029 [P] Write Android device format fixture runner `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/format/FormatFixtureDeviceTest.kt` with the same comparison as T028 using `AssetFixtures`
- [ ] T030 [P] Write iOS format fixture runner `Tests/RollingNumberTests/Format/FormatFixtureTests.swift` with the same comparison rules as T028 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

**Checkpoint**: 두 플랫폼에서 `currency-basic.json`과 로직 단위 테스트가 통과한다

---

## Phase 3: User Story 1 - 금액 변화를 방향 있는 롤링으로 보기 (Priority: P1) 🎯 MVP

**Goal**: 값이 바뀌면 바뀐 자리만 값의 방향으로 굴러가고, 늘어나거나 줄어든 자리는 굴러 들어오거나 나가며 페이드되고, 폭이 부드럽게 바뀐다. 첫 표시와 같은 값 재입력은 애니메이션하지 않는다.

**Independent Test**: 샘플 앱에서 숫자 하나를 두고 수량 ±로 15,000 → 12,000, 9,900 → 12,900, 12,900 → 9,900을 만들어 각 자리의 방향, 등장·퇴장, 폭 전환을 확인한다(quickstart §5 #1~#3). 자동으로는 `diff/us1-*.json`, `session/us1-basic.json`이 두 플랫폼에서 통과한다.

### Tests for User Story 1 ⚠️ (먼저 작성하고 실패 확인)

- [X] T031 [P] [US1] Create `fixtures/diff/us1-basic.json` (trend `auto`) with cases `roll-down-single`, `digit-count-grows`, `group-appears`, `digit-count-shrinks`, `first-display` (animate false → empty elements; animate true → all digits `enter-up`, others `enter-fade`), `same-value`; expected `direction` and ordered `elements` derived by hand from data-model §6 (exiting elements inserted after their previous-list predecessor)
- [X] T032 [P] [US1] Create `fixtures/diff/us1-edge-cases.json` (trend `auto`) with cases `zero-to-positive`, `positive-to-zero`, `positive-to-negative` (`₩500` → `-₩300`, `S` `enter-fade`), `negative-to-positive`, `trailing-zero-same-value` (12.5 → 12.50: direction `none`, `F1` `enter-fade`), `fraction-drops` (12.99 → 13: `D` `exit-fade`, `F0`/`F1` `exit-up`, `I0` `roll-up`), `billion` (999,999,999 → 1,000,000,000), `compact-unit-appears` (`999` → `1K`), `compact-unit-changes` (`1.2K` → `1.2M`, `Ls0` `crossfade`), `affix-changes` (suffix `/월` → `/년`, `AS` `crossfade`), `de-de-currency` (`1.234,56 €` → `1.299,00 €`)
- [X] T033 [P] [US1] Write Android diff fixture runner `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/DiffFixtureTest.kt`: build `FormattedNumber` from each case's `symbols`/`from`/`to`, call `TransitionDiff.compute`, assert `direction` and the full ordered `elements` list field by field
- [ ] T034 [P] [US1] Write iOS diff fixture runner `Tests/RollingNumberTests/Logic/DiffFixtureTests.swift` identical in behavior to T033 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T035 [P] [US1] Write Android motion tests `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/motion/MotionEngineTest.kt` with a virtual clock: default springs (layout 300/1.0, roll 380/0.8, fade 500/1.0) settle between 0.3 s and 0.6 s; same result for 8 ms vs 16 ms vs jittered frame intervals (substep ≤ 4 ms) within 0.001; settle thresholds "위치 오차 < 0.001줄, 속도 < 0.01줄/초, 투명도 오차 < 0.005, 폭 비율 오차 < 0.001"; `enter-up` layer starts at offsetY +1 alpha 0 and ends at 0/1; `exit-down` ends at offsetY +1 alpha 0 widthFactor 0 and is removed
- [ ] T036 [P] [US1] Write iOS motion tests `Tests/RollingNumberTests/Logic/MotionEngineTests.swift` with exactly the same cases as T035 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T037 [P] [US1] Create `fixtures/session/us1-basic.json` with cases: first `set` produces no events; same value again produces no events; one change produces `start` then `end` exactly once; invalid value (`"NaN"`) keeps last value and produces no events
- [X] T038 [P] [US1] Write Android session fixture runner `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/SessionFixtureTest.kt` driving `RollingSession` (and later `AnnouncementCoordinator`) with a virtual clock per `contracts/fixtures.md` §3, comparing event/announcement kind, order, count exactly and `end` time within ±16.7 ms when given
- [ ] T039 [P] [US1] Write iOS session fixture runner `Tests/RollingNumberTests/Logic/SessionFixtureTests.swift` identical in behavior to T038 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

### Implementation for User Story 1

- [X] T040 [P] [US1] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/TransitionDiff.kt` with `Trend` enum (`auto`, `alwaysUp`, `alwaysDown`, `perDigit`; only `auto` behavior required now, others may throw `NotImplementedError` until US6), `DiffResult`, `DiffElement(key, kind, from, to, action)`, action table of data-model §6 including "direction이 `none`이면 Trend와 상관없이 d = none", empty elements when full text incl. affixes is equal, and the ordering rule
- [ ] T041 [P] [US1] Implement iOS `Sources/RollingNumber/Logic/TransitionDiff.swift` mirroring T040 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T042 [P] [US1] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/motion/MotionCurve.kt` (`Spring(stiffness > 0, dampingRatio > 0)`, `Tween(durationMs > 0, x1, y1, x2, y2)`, `MotionSpec(layout, roll, fade, enabled = true, respectReduceMotion = true, animateFirstDisplay = false)`, default constants from research R5) and `logic/motion/SpringIntegrator.kt` (mass 1, semi-implicit Euler, fixed substep ≤ 4 ms, value+velocity state)
- [ ] T043 [P] [US1] Implement iOS `Sources/RollingNumber/Logic/Motion/MotionCurve.swift` and `Sources/RollingNumber/Logic/Motion/SpringIntegrator.swift` mirroring T042 — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T044 [US1] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/motion/ElementState.kt` (key, widthFactor 0~1, layers with text/offsetY -1~1/alpha, exiting) and `logic/motion/MotionEngine.kt` (`apply(diff, nowNanos)`, `step(nowNanos): Boolean`, `snapToEnd()`, `frame(measure, tabularDigitWidth): RenderFrame` with totalWidth and per-item x/width/layers); `stay` keeps the element, `roll-*` adds an incoming layer and sends the old layer out, `enter-*`/`exit-*`/`crossfade` per data-model §6 (depends on T040, T042)
- [ ] T045 [US1] Implement iOS `Sources/RollingNumber/Logic/Motion/ElementState.swift` and `Sources/RollingNumber/Logic/Motion/MotionEngine.swift` mirroring T044 (depends on T041, T043) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T046 [US1] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/RollingSession.kt`: phases `empty`/`idle`/`animating`, internal listener `onStart`/`onEnd`, transition rows of data-model §10 for: same text → nothing; `idle` + animatable → diff → `animating` + onStart; `animating` + update → re-apply without events; `step` all settled → `idle` + onEnd; first value without animation; `accessibilityText` = target full text (depends on T044)
- [ ] T047 [US1] Implement iOS `Sources/RollingNumber/Logic/RollingSession.swift` mirroring T046 (depends on T045) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T048 [P] [US1] Implement Android rendering: `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/render/GlyphCache.kt` (`TextMeasurer` cache per text+style; tabular digit width = max width of "0"~"9"), `render/FadeMask.kt` (top/bottom linear gradient via offscreen compositing + `BlendMode.DstIn`, default 4.dp, 0.dp = none), `render/RollingNumberLayout.kt` (custom `Layout` sized by `RenderFrame.totalWidth`, draws layers in `drawWithCache`/`DrawScope` reading animation state only in layout/draw phases)
- [X] T049 [P] [US1] Implement Android frame driver `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/motion/FrameDriver.kt`: `LaunchedEffect` loop with `withFrameNanos` calling `session.step` only while animating, exposing a frame counter `State<Long>` read by the layout/draw phase (no recomposition of the caller per frame)
- [X] T050 [US1] Implement Android public API: `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingNumberStyle.kt` (`RollingNumberStyle(textStyle, tabularDigits = true, fadeMaskHeight = 4.dp)`), `RollingMotion.kt` (`RollingMotion`, `RollingCurve.Spring`/`Tween`, `RollingNumberDefaults.style()`/`motion()`), and `RollingNumber.kt` with the full signature of `contracts/android-api.md` (parameters not yet implemented are accepted and stored), overloads for `Long`, `Int`, `Double` (`BigDecimal.valueOf`); `Double.NaN`/infinite → keep last valid value and `Log.w` (depends on T025, T046, T048, T049)
- [ ] T051 [P] [US1] Implement iOS rendering `Sources/RollingNumber/Render/GlyphMeasurer.swift` (resolved `Text` size cache, tabular digit width = max of "0"~"9") and `Sources/RollingNumber/Render/RollingCanvas.swift` (`TimelineView(.animation(paused: !isAnimating))` + `Canvas` drawing layers, `.frame(width:)` from `RenderFrame.totalWidth`, top/bottom gradient mask default 4 pt, honoring environment `.font` and `.foregroundColor`/`.foregroundStyle`) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [ ] T052 [US1] Implement iOS public API: `Sources/RollingNumber/RollingMotion.swift` (`RollingMotion`, `RollingCurve`, `.default`) and replace placeholder `Sources/RollingNumber/RollingNumber.swift` with the `RollingNumber` view and inits for `Decimal`, `Int`, `Double` per `contracts/ios-api.md`; session held in a reference-type state object that does not publish per frame; `Double.nan`/infinite → keep last valid value and debug log (depends on T026, T047, T051) — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T053 [P] [US1] Update Android sample `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt`: one product row with quantity −/+ buttons (unit price 9,900 KRW) and a total `RollingNumber(value = total, format = RollingNumberFormat.currency("KRW"))`
- [ ] T054 [P] [US1] Update iOS sample `samples/ios/CouponCheckout/Sources/CheckoutView.swift` with the same screen as T053 using `RollingNumber(total, format: .currency(code: "KRW"))` — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

**Checkpoint**: US1 픽스처 전부 통과, 두 샘플 앱에서 quickstart §5 #1~#3 확인. 여기까지가 MVP

---

## Phase 4: User Story 2 - 로케일·통화 포맷으로 표시 (Priority: P1)

**Goal**: 통화·퍼센트·축약·소수 자리·구분자·반올림·로케일·접두/접미를 지원하고, 로케일이나 포맷이 바뀌면 굴러가지 않고 즉시 바뀐다.

**Independent Test**: 애니메이션 없이 여러 로케일·포맷 조합의 표시를 `format/` 픽스처 기대값과 비교한다. Android는 API 24·29·30·최신 기기, iOS는 iOS 15·최신 시뮬레이터에서 통과해야 한다.

### Tests for User Story 2 ⚠️

- [X] T055 [P] [US2] Create `fixtures/format/percent.json` (en-US 0.125 minFraction 1 → `12.5`, literal `Ls0` "%"; ko-KR; de-DE with space before %), `fixtures/format/compact.json` (en-US 1200 → `1.2` + `K`; en-US 999 → `999`; en-US 1250000 → `1.3` + `M`; ko-KR 1200 → "1.2천"; ko-KR 13000 → "1.3만"), `fixtures/format/fraction-rounding.json` (min/max fraction, `halfUp` 2.345→2.35, `halfEven` 2.345→2.34, `down`, `up`, grouping false), `fixtures/format/locale-latn.json` (ar-EG and hi-IN-u-nu-deva inputs still produce Latin digits), `fixtures/format/affix.json` (prefix `~`, suffix `/월` tokens `AP`/`AS`), `fixtures/format/large.json` (123456789012345 and 1000000000 exact)
- [X] T056 [P] [US2] Create `fixtures/session/us2-format-change.json`: same value with locale change (ko-KR → en-US) → no events, no announcement; locale change together with value change → no events, one announcement (announcement assertions activate in US5)
- [X] T057 [P] [US2] Write Android test `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/RollingNumberFormatTest.kt` for validation rules ("둘 다 0~15", min ≤ max, unknown currency code → `IllegalArgumentException`) and formatKey equality

### Implementation for User Story 2

- [X] T058 [US2] Extend `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/format/ModernIcuFormatter.kt` (API 30+) with percent (`unit(MeasureUnit.PERCENT)` + `scale(Scale.powerOfTen(2))`), compact (`notation(Notation.compactShort())`), `precision(Precision.minMaxFraction(min, max))`, `grouping(...)`, `roundingMode(...)` with mapping HalfUp→`HALF_UP`, HalfEven→`HALF_EVEN`, Down→`DOWN`, Up→`UP`
- [X] T059 [US2] Extend `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/format/LegacyIcuFormatter.kt` (API 24~29) with `getPercentInstance`, `CompactDecimalFormat.getInstance(ulocale, SHORT)`, `setMinimumFractionDigits`/`setMaximumFractionDigits`, `setGroupingUsed`, `setRoundingMode` with the same mapping as T058
- [X] T060 [US2] Run `format/compact.json` on API 29 and API 30+ (T028, T029); if Modern and Legacy compact outputs differ, switch both paths to `CompactDecimalFormat` in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/format/IcuFormatter.kt` and record the decision in `specs/001-rolling-number-display/research.md` R2
- [ ] T061 [P] [US2] Extend `Sources/RollingNumber/Format/FoundationFormatter.swift` with `Decimal.FormatStyle.Percent`, `.notation(.compactName)`, `.precision(.fractionLength(min...max))`, `.grouping(.automatic/.never)`, `.rounded(rule:)` mapping HalfUp→`.toNearestOrAwayFromZero`, HalfEven→`.toNearestOrEven`, Down→`.towardZero`, Up→`.awayFromZero` — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**
- [X] T062 [US2] Implement default-locale handling: Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingNumber.kt` uses `locale ?: LocalConfiguration.current.locales[0]` so runtime locale changes recompose; iOS `Sources/RollingNumber/RollingNumber.swift` uses `locale ?? @Environment(\.locale)` — Android 검증 완료, iOS 쪽은 macOS 검증 대기
- [X] T063 [US2] Implement FR-014 in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/RollingSession.kt` and `Sources/RollingNumber/Logic/RollingSession.swift`: when `formatKey` differs from the current target, replace immediately with no events ("formatKey 바뀜 → 즉시 교체, 이벤트 없음. 값도 바뀌었으면 알림"), and snap any running animation without emitting `end` — Android 검증 완료, iOS 쪽은 macOS 검증 대기
- [X] T064 [P] [US2] Add locale menu (ko-KR, en-US, de-DE, ja-JP) and coupon-percent display to `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt`
- [ ] T065 [P] [US2] Add the same locale menu and percent display to `samples/ios/CouponCheckout/Sources/CheckoutView.swift` — **코드 작성 완료, macOS에서 빌드·테스트 검증 대기**

**Checkpoint**: 모든 `format/` 픽스처가 Android(API 24·29·30·최신)와 iOS(15·최신)에서 통과

---

## Phase 5: User Story 5 - 접근성: 최종 값만 낭독, 동작 줄이기 존중 (Priority: P1)

**Goal**: 보조기술에는 라벨 + 최종 값만 노출하고, 값이 바뀌면 정착 후 한 번만 알린다. 동작 줄이기가 켜져 있으면 애니메이션 없이 즉시 바꾼다.

**Independent Test**: TalkBack/VoiceOver를 켜고 값을 바꿔 낭독 횟수와 내용을 기록한다(quickstart §6 #1, #2, #4~#7). 자동으로는 `session/us5-*.json`과 Compose 시맨틱 테스트가 통과한다. 그룹 병합 알림(US5 시나리오 6, 7)은 US4 단계에서 검증한다.

### Tests for User Story 5 ⚠️

- [ ] T066 [P] [US5] Create `fixtures/session/us5-announce.json` (single member with label "최종가"): one change → exactly one `announce` "최종가 ₩12,000" after `end`; rapid 15,000 → 12,000 → 9,000 → only "최종가 ₩9,000"; first display → none; same value → none; `announce: false` → none; `reduceMotion: true` then change → no events and immediate announce; `reduceMotion: true` during animation → `end` immediately
- [ ] T067 [P] [US5] Write Android Compose UI test `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/a11y/RollingSemanticsTest.kt`: exactly one semantics node with no children; `contentDescription` equals label + target full text immediately after a value change while animation is running; intermediate values never appear

### Implementation for User Story 5

- [ ] T068 [P] [US5] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/AnnouncementCoordinator.kt` (single-member mode now: `register`, `unregister`, announce callback when the member with a pending announcement becomes `idle`) and wire `pendingAnnouncement`, `label`, `announce` into `logic/RollingSession.kt` ("알림은 값 또는 표시 문자열이 바뀐 경우에만 예약한다. 첫 표시와 itemKey 변경은 알리지 않는다")
- [ ] T069 [P] [US5] Implement iOS `Sources/RollingNumber/Logic/AnnouncementCoordinator.swift` and the same `Sources/RollingNumber/Logic/RollingSession.swift` changes as T068
- [ ] T070 [US5] Implement `setAnimationAllowed(allowed)` in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/RollingSession.kt` and `Sources/RollingNumber/Logic/RollingSession.swift`: not allowed + update → immediate replace, no events, immediate announce; turning not-allowed while `animating` → `snapToEnd()` + onEnd + `idle`
- [ ] T071 [P] [US5] Implement Android reduce-motion detection `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/motion/ReduceMotion.kt`: composable `rememberReduceMotion(): State<Boolean>` = `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, updated via `ContentObserver` registered/unregistered with `DisposableEffect`; combine with `RollingMotion.enabled` and `respectReduceMotion` into `setAnimationAllowed` in `RollingNumber.kt`
- [ ] T072 [P] [US5] Implement iOS reduce-motion wiring in `Sources/RollingNumber/RollingNumber.swift` using `@Environment(\.accessibilityReduceMotion)` combined with `RollingMotion.enabled` and `respectReduceMotion`
- [ ] T073 [P] [US5] Implement Android accessibility `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/a11y/RollingSemantics.kt` (`Modifier.clearAndSetSemantics { contentDescription = session.accessibilityText }`) and `a11y/AnnouncementNode.kt` (1px invisible node with `liveRegion = LiveRegionMode.Polite` whose text changes only when the coordinator announces; fallback flag to use `View.announceForAccessibility` on API ≤ 35); wire `accessibilityLabel` and `announceChanges` parameters in `RollingNumber.kt`
- [ ] T074 [P] [US5] Implement iOS accessibility `Sources/RollingNumber/Accessibility/RollingAccessibility.swift` (`.accessibilityElement(children: .ignore)`, `.accessibilityLabel(label)`, `.accessibilityValue(target text)`), `Sources/RollingNumber/Accessibility/Announcer.swift` (`UIAccessibility.post(notification: .announcement, argument: NSAttributedString(string:, attributes: [.accessibilitySpeechQueueAnnouncement: true]))`), and modifiers `rollingAccessibilityLabel(_:)`, `rollingAnnouncements(_:)` in `Sources/RollingNumber/RollingNumberModifiers.swift`
- [ ] T075 [P] [US5] Add labels ("최종가") and an "동작 줄이기 무시" toggle (`respectReduceMotion = false`) to `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt`
- [ ] T076 [P] [US5] Add the same label and toggle to `samples/ios/CouponCheckout/Sources/CheckoutView.swift`
- [ ] T077 [US5] Manually verify quickstart §6 #1, #2, #4~#7 on an Android device (TalkBack) and iOS device/simulator (VoiceOver); if Android announcements are not spoken, enable the T073 fallback for API ≤ 35, re-verify, and record the result in `specs/001-rolling-number-display/research.md` R7

**Checkpoint**: P1 스토리(US1, US2, US5) 완료. 단독 숫자로 실사용 가능한 수준

---

## Phase 6: User Story 3 - 빠른 연속 변경에도 끊김 없는 전환 (Priority: P2)

**Goal**: 애니메이션 도중 값이 바뀌어도 모든 요소가 현재 위치·투명도·폭에서 새 목표로 이어지고, 요소가 중복으로 생기지 않으며, 종료 이벤트는 마지막에 한 번만 발생한다.

**Independent Test**: 진행 중(40% 지점) 새 값을 넣고 프레임 단위로 순간 이동과 중복이 없는지 확인한다. 샘플 앱에서 쿠폰을 빠르게 5번 연타한다(quickstart §5 #4).

### Tests for User Story 3 ⚠️

- [ ] T078 [P] [US3] Create `fixtures/session/us3-rapid.json` containing `rapid-taps-single-end` exactly as in `contracts/fixtures.md` §3 plus a 15,000 → 12,000 → 15,000 reversal case (one `start`, one `end`)
- [ ] T079 [P] [US3] Write Android continuity tests `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/motion/MotionContinuityTest.kt`: sample every 16 ms with a virtual clock for US3 scenario 1 (15,000 → 12,000, reverse at 40%) and scenario 2 (9,900 → 12,900, reverse at 40%); assert no element moves more than 0.35 line or 0.35 alpha or 0.35 widthFactor between frames, no two `ElementState`s share a key, no element has two layers with the same text, and the reversed "1" fades out from its current alpha
- [ ] T080 [P] [US3] Write iOS continuity tests `Tests/RollingNumberTests/Logic/MotionContinuityTests.swift` with exactly the same cases as T079

### Implementation for User Story 3

- [ ] T081 [US3] Implement retargeting in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/motion/MotionEngine.kt` and `logic/motion/ElementState.kt`: keep current value and velocity for springs; tween restarts from the current value; if the new target glyph already exists as a layer, return that layer to offsetY 0 / alpha 1 and send other layers out ("없을 때만 새 레이어를 만든다"); revive an exiting `ElementState` when its key reappears; recompute direction against the previous target value
- [ ] T082 [US3] Implement the same retargeting in `Sources/RollingNumber/Logic/Motion/MotionEngine.swift` and `Sources/RollingNumber/Logic/Motion/ElementState.swift`
- [ ] T083 [P] [US3] Add a coupon toggle button (10% coupon) to `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt` suitable for rapid tapping
- [ ] T084 [P] [US3] Add the same coupon toggle to `samples/ios/CouponCheckout/Sources/CheckoutView.swift`

**Checkpoint**: 연타 시 끊김·깜빡임·중복 없이 마지막 값으로 정착, 종료 이벤트 1회

---

## Phase 7: User Story 4 - 인접한 여러 숫자의 위치 변화 동기화 (Priority: P2)

**Goal**: 그룹으로 묶인 숫자들은 같은 프레임 시계로 같은 시점에 시작하고, 폭 변화로 밀리는 이웃도 같은 곡선으로 움직이며, 스크린리더 알림은 하나로 병합된다(US5 시나리오 6, 7 포함).

**Independent Test**: [원가][할인액][최종가]를 한 줄 그룹으로 두고 할인액 자릿수를 늘려 최종가가 순간 이동 없이 밀리는지 확인한다. VoiceOver/TalkBack으로 병합 알림 1회를 확인한다(quickstart §6 #3).

### Tests for User Story 4 ⚠️

- [ ] T085 [P] [US4] Create `fixtures/session/us4-group.json`: members `discount` (label "할인액") and `total` (label "최종가") in order; both change at the same `t` → both `start` at the same `t`, one `announce` "할인액 -₩1,900, 최종가 ₩12,900" and no individual announcements; only `total` changes → "최종가 ₩12,900"; member with `announce: false` excluded; a second member changing while the first is pending → single announcement after both settle
- [ ] T086 [P] [US4] Write Android Compose UI test `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/RollingNumberGroupTest.kt`: in a `Row` inside `RollingNumberGroup`, increase the middle number's digit count and assert, via `mainClock` manual advancing, that the right neighbor's x position changes by less than 35% of the width delta per frame and ends at the final position

### Implementation for User Story 4

- [ ] T087 [US4] Extend `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/AnnouncementCoordinator.kt` to multi-member groups per data-model §11 (order by `groupOrder` then registration; join `"{라벨} {전체 표시 문자열}"` with `", "`; announce once when all pending members are `idle`; exclude members with announce off) and add `logic/GroupClock.kt` (one shared frame time source so changes in the same frame start in the same frame)
- [ ] T088 [US4] Implement the same in `Sources/RollingNumber/Logic/AnnouncementCoordinator.swift` and `Sources/RollingNumber/Logic/GroupClock.swift`
- [ ] T089 [US4] Implement Android `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingNumberGroup.kt` (`CompositionLocal` providing coordinator + `GroupClock`; nearest group wins; group-level `AnnouncementNode`) and make `RollingNumber.kt` join the nearest group and honor `groupOrder`; `FrameDriver.kt` uses the group clock when present
- [ ] T090 [US4] Implement iOS `Sources/RollingNumber/RollingNumberGroup.swift` (environment-provided coordinator + clock, nearest group wins), `rollingGroupOrder(_:)` in `Sources/RollingNumber/RollingNumberModifiers.swift`, and make `RollingNumber.swift`/`Render/RollingCanvas.swift` use the group clock
- [ ] T091 [P] [US4] Add a grouped summary row [원가][할인액][배송비][최종가] with labels and shipping rule (free at ≥ 30,000 KRW) to `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/CheckoutScreen.kt`
- [ ] T092 [P] [US4] Add the same grouped summary row to `samples/ios/CouponCheckout/Sources/CheckoutView.swift`
- [ ] T093 [US4] Manually verify quickstart §5 #2 and §6 #3 on both platforms (neighbors move without jumps; merged announcement spoken once, not cut off)

**Checkpoint**: 결제 화면 요약 줄 전체가 같이 움직이고 알림이 한 번에 읽힘

---

## Phase 8: User Story 6 - 개발자 제어: 방향·타이밍·스타일·이벤트·끄기 (Priority: P3)

**Goal**: 방향 모드 4종, 곡선 커스터마이즈(스프링/트윈), 애니메이션 끄기, 시작/종료 이벤트, 항목 식별자, 첫 표시 애니메이션, 고정폭·페이드 마스크 옵션을 공개 API로 제공한다.

**Independent Test**: 옵션을 하나씩 바꿔 기본값 대비 동작 차이를 확인하고 이벤트 순서·횟수를 기록한다. 자동으로는 `diff/us6-trend.json`, `session/us6-control.json`이 통과한다.

### Tests for User Story 6 ⚠️

- [ ] T094 [P] [US6] Create `fixtures/diff/us6-trend.json`: `trend-always-up` (15,000 → 12,000, all changed digits `roll-up`), `trend-always-down` (12,000 → 15,000), `trend-per-digit` (19 → 21: `I1` `roll-up`, `I0` `roll-down`), per-digit with entering digit (99 → 100: entering digit uses direction `up`), `alwaysUp` with direction `none` (format-only change → `crossfade`, not roll)
- [ ] T095 [P] [US6] Create `fixtures/session/us6-control.json`: `disableAnimation` during animation → `end` immediately; changes while disabled → no events; `itemKey` change during animation → no `end`, no announcement, immediate new value; same `itemKey` → normal animation; `animateFirstDisplay` true → first value produces `start`/`end`
- [ ] T096 [P] [US6] Write Android tween tests `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/logic/motion/TweenTest.kt` (cubic-bezier progress at 0/0.5/1, completes at `durationMs`, interruption restarts from current value) and iOS `Tests/RollingNumberTests/Logic/TweenTests.swift` with the same cases

### Implementation for User Story 6

- [ ] T097 [US6] Implement `alwaysUp`, `alwaysDown`, `perDigit` in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/TransitionDiff.kt` and `Sources/RollingNumber/Logic/TransitionDiff.swift` ("`perDigit`면 매칭된 자리는 자기 숫자의 증감(같으면 Stay), 등장·퇴장 자리는 direction")
- [ ] T098 [US6] Implement Tween evaluation (cubic-bezier) in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/motion/MotionEngine.kt` and `Sources/RollingNumber/Logic/Motion/MotionEngine.swift`
- [ ] T099 [US6] Implement `setItemKey` in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/logic/RollingSession.kt` and `Sources/RollingNumber/Logic/RollingSession.swift` ("itemKey 변경 → 즉시 새 값 표시, onEnd 없음, 알림 취소") and `animateFirstDisplay` handling for the `empty` phase
- [ ] T100 [US6] Wire Android public options in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingNumber.kt`: `trend`, `motion` (curves, `enabled` → `setAnimationAllowed`, `animateFirstDisplay`), `itemKey`, `onAnimationStart`/`onAnimationEnd` on the main thread, `style.tabularDigits = false` path in `render/GlyphCache.kt`, `style.fadeMaskHeight` in `render/FadeMask.kt`; add `RollingTrend` enum in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/RollingTrend.kt`
- [ ] T101 [US6] Wire iOS public modifiers in `Sources/RollingNumber/RollingNumberModifiers.swift`: `rollingTrend`, `rollingMotion`, `rollingTabularDigits`, `rollingFadeMask(height:)`, `rollingItemKey`, `onRollingAnimationStart`, `onRollingAnimationEnd` (callbacks on main actor, nearest environment value wins); add `RollingTrend` in `Sources/RollingNumber/RollingTrend.swift`
- [ ] T102 [P] [US6] Add an "애니메이션 끄기" toggle and an "주문 내역" tab (100-row `LazyColumn` of `RollingNumber` with `itemKey = order.id`, a few values changing every second) to `android/sample/src/main/kotlin/io/github/komodgn/rollingnumber/sample/` (`CheckoutScreen.kt`, new `OrderHistoryScreen.kt`, `MainActivity.kt` tabs)
- [ ] T103 [P] [US6] Add the same toggle and an "주문 내역" tab (`List` with `.rollingItemKey(order.id)`) to `samples/ios/CouponCheckout/Sources/` (`CheckoutView.swift`, new `OrderHistoryView.swift`, `CouponCheckoutApp.swift` `TabView`)
- [ ] T104 [US6] Manually verify quickstart §5 #6 and §7 (list reuse: no stale values, no animation right after reuse) on both platforms

**Checkpoint**: 모든 사용자 스토리 완료

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 성능, 문서, 배포, 전체 검증

- [ ] T105 [P] Write Android recomposition test `android/rollingnumber/src/androidTest/kotlin/io/github/komodgn/rollingnumber/RecompositionTest.kt`: a caller composable containing `RollingNumber` recomposes at most once per value change while a 500 ms animation runs
- [ ] T106 [P] Implement Macrobenchmark `android/benchmark/src/main/kotlin/io/github/komodgn/rollingnumber/benchmark/CheckoutBenchmark.kt` with `FrameTimingMetric` tapping the coupon toggle 10 times on the checkout screen; target "프레임 드랍이 측정 구간의 1% 미만" (SC-002)
- [ ] T107 Profile iOS sample checkout screen with Instruments Animation Hitches (quickstart §8) and fix any hitch sources in `Sources/RollingNumber/Render/RollingCanvas.swift` / `Sources/RollingNumber/Render/GlyphMeasurer.swift`
- [ ] T108 [P] Add a fixture coverage check `android/rollingnumber/src/test/kotlin/io/github/komodgn/rollingnumber/fixtures/FixtureCoverageTest.kt` asserting every case id listed in `contracts/fixtures.md` "최소 수록 케이스" exists in `fixtures/diff/`
- [ ] T109 [P] Write root `README.md`: what the library does, Android install (`io.github.komodgn:rollingnumber`) and iOS install (SPM URL), one-line usage for both platforms, options table, accessibility and reduced-motion behavior, supported OS (Android 7.0+, iOS 15+)
- [ ] T110 [P] Add KDoc to every public declaration in `android/rollingnumber/src/main/kotlin/io/github/komodgn/rollingnumber/*.kt` and DocC comments to every public declaration in `Sources/RollingNumber/*.swift`
- [ ] T111 Configure Maven Central publishing in `android/rollingnumber/build.gradle.kts` (`maven-publish`, `signing`, POM metadata, sources/javadoc jars) and add release workflow `.github/workflows/release.yml` (on tag `v*`: publish Android artifact; SPM uses the same git tag)
- [ ] T112 Run the full quickstart (`specs/001-rolling-number-display/quickstart.md` §1~§8) on both platforms and record results in `specs/001-rolling-number-display/checklists/validation.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존 없음. 바로 시작
- **Foundational (Phase 2)**: Setup 완료 후. **모든 스토리를 막음**
- **US1 (Phase 3)**: Foundational 완료 후. MVP
- **US2 (Phase 4)**: Foundational 완료 후 시작 가능. T062·T063은 US1의 `RollingNumber`·`RollingSession`(T046~T052)을 수정하므로 US1 뒤에 한다
- **US5 (Phase 5)**: US1 완료 후(세션·공개 컴포넌트 필요)
- **US3 (Phase 6)**: US1 완료 후(모션 엔진 필요). US5와 병렬 가능
- **US4 (Phase 7)**: US5 완료 후(알림 조정기를 다중 멤버로 확장)
- **US6 (Phase 8)**: US1 완료 후. T099는 US5의 알림 취소를 쓰므로 US5 뒤가 안전
- **Polish (Phase 9)**: 원하는 스토리 모두 완료 후

### User Story Dependencies

```text
Setup → Foundational → US1 ─┬─▶ US2
                            ├─▶ US5 ──▶ US4
                            ├─▶ US3
                            └─▶ US6 (US5 이후 권장)
                                        └──▶ Polish
```

### Within Each User Story

- 픽스처·테스트 먼저 작성하고 실패 확인 → 로직(logic/Logic) → 렌더링 → 공개 API → 샘플 앱 → 수동 검증
- 같은 이름의 Android/iOS 작업은 서로 독립적이라 병렬 가능하지만, **같은 PR에서 함께 끝내** 두 플랫폼이 같은 픽스처를 통과하는 상태를 유지한다

### Parallel Opportunities

- Setup: T003~T009 모두 병렬
- Foundational: 픽스처 로더(T010~T012), DecimalValue 테스트·구현(T013~T016), 타입(T019, T020), 포맷 공개 타입(T023, T024), 포맷 러너(T028~T030) 병렬
- 각 스토리 안에서 Android와 iOS 짝 작업, 픽스처 파일 작성은 병렬
- US1 완료 후 US2, US3, US5, US6은 서로 다른 사람이 병렬로 진행 가능(위 의존 주의)

---

## Parallel Example: User Story 1

```bash
# 픽스처와 테스트 러너를 동시에:
Task: "Create fixtures/diff/us1-basic.json"                         # T031
Task: "Create fixtures/diff/us1-edge-cases.json"                    # T032
Task: "Write Android diff fixture runner DiffFixtureTest.kt"        # T033
Task: "Write iOS diff fixture runner DiffFixtureTests.swift"        # T034
Task: "Write Android MotionEngineTest.kt"                           # T035
Task: "Write iOS MotionEngineTests.swift"                           # T036

# 로직은 플랫폼별로 동시에:
Task: "Implement Android logic/TransitionDiff.kt"                   # T040
Task: "Implement iOS Logic/TransitionDiff.swift"                    # T041
Task: "Implement Android motion/MotionCurve.kt + SpringIntegrator"  # T042
Task: "Implement iOS Motion/MotionCurve.swift + SpringIntegrator"   # T043
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational (모든 스토리를 막는 단계)
3. Phase 3 US1
4. **멈추고 검증**: 두 플랫폼에서 `diff/us1-*.json`, `session/us1-basic.json` 통과, 샘플 앱 quickstart §5 #1~#3
5. 데모 가능

### Incremental Delivery

1. Setup + Foundational → 기반 완료
2. US1 → 검증 → 데모 (MVP)
3. US2 → 로케일·포맷 완성 → 데모
4. US5 → 접근성 완성. **여기까지가 P1, 외부 배포 가능한 최소 수준**
5. US3 → 연타 안정성
6. US4 → 결제 요약 줄 그룹
7. US6 → 커스터마이즈 옵션
8. Polish → 성능 측정, 문서, Maven Central·SPM 배포

### Windows 환경 메모

- Android 작업(Phase 1~9의 Android 쪽)은 현재 PC에서 바로 빌드·테스트할 수 있다.
- iOS 작업은 코드 작성까지만 여기서 하고, `xcodebuild test`는 CI(T008)의 macOS 러너나 Mac에서 돌린다. PR이 CI의 iOS 잡을 통과해야 그 작업을 끝난 것으로 본다.

---

## Notes

- [P] = 다른 파일, 끝나지 않은 작업에 의존하지 않음
- [Story] 라벨로 작업을 스토리에 연결
- 테스트가 먼저 실패하는 것을 확인한 뒤 구현
- 규칙을 바꿀 때는 픽스처를 먼저 고치고 두 플랫폼 구현을 같은 PR에서 고친다
- 각 체크포인트에서 멈추고 스토리를 독립적으로 검증할 수 있다
