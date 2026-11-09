# Implementation Plan: 롤링 숫자 표시 컴포넌트 (Rolling Number Display)

**Branch**: `001-rolling-number-display` (스펙 디렉터리 기준, 현재 git 브랜치는 `main`) | **Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-rolling-number-display/spec.md`

## Summary

결제·장바구니 화면의 금액처럼 바뀌는 숫자를 자리별로 굴려 전환하는 Android/iOS UI 컴포넌트 라이브러리를 만든다.

핵심 접근은 **"같은 규칙을 두 플랫폼에서 각각 구현하고, 공유 JSON 테스트 케이스로 같음을 증명한다"**이다.

- **Android**: Kotlin + Jetpack Compose. 포맷은 `android.icu` 계열(API 30+ `android.icu.number.NumberFormatter`, 미만은 `android.icu.text.NumberFormat`/`CompactDecimalFormat` 폴백).
- **iOS**: Swift + SwiftUI. 포맷은 `FormatStyle`(기본)과 `NumberFormatter`(기호 조회·보조).
- **순수 로직**: 포맷된 문자열을 조각(정수 자리, 소수 자리, 구분자, 부호, 통화 기호 등 리터럴, 접두·접미)으로 분해하고, 소수점 기준 위치 키로 이전/다음 상태를 diff하는 로직을 각 플랫폼에 UI와 분리된 순수 코드로 구현한다.
- **공유 JSON 테스트 케이스** (`fixtures/`): "입력값·로케일·포맷 → 기대 조각 목록과 diff 결과"를 두 플랫폼 테스트가 같은 파일로 검증한다(FR-023, SC-003).
- **모션**: 두 플랫폼이 같은 스프링 공식·상수·정착 기준으로 자체 모션 엔진을 구현한다. iOS 15에는 SwiftUI 애니메이션 완료 콜백이 없어 FR-020 이벤트를 위해 자체 엔진이 필요하다.
- **배포**: Android는 Maven Central, iOS는 Swift Package Manager.
- **샘플 앱**: 각 플랫폼에 쿠폰 적용 결제 화면 데모.

근거와 대안 비교는 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Kotlin 2.x (K2) / Swift 5.9+ (Swift 6 툴체인 호환). 정확한 버전은 `gradle/libs.versions.toml`과 `Package.swift`에 고정한다.

**Primary Dependencies**:
- Android: Jetpack Compose (BOM 최신 안정판), OS 내장 `android.icu`. 런타임 서드파티 의존성 0개.
- iOS: SwiftUI, Foundation. 서드파티 의존성 0개.
- 테스트 전용: Android는 kotlinx-serialization-json(픽스처 읽기), JUnit, Robolectric, Compose UI Test, Macrobenchmark. iOS는 XCTest + Foundation `JSONDecoder`.

**Storage**: N/A

**Testing**: Android(JUnit + Robolectric + Compose UI Test, 기기 테스트, Macrobenchmark), iOS(XCTest, Instruments). 두 플랫폼 모두 `fixtures/`의 같은 JSON을 읽는다.

**Target Platform**: Android 7.0+ (minSdk 24), iOS 15+

**Project Type**: 모바일 UI 라이브러리 2개(Android 라이브러리 + Swift Package) + 샘플 앱 2개

**Performance Goals**: 전환 중 60fps 유지, 프레임 드랍 1% 미만 (SC-002). 모션 계산은 숫자 3개 × 요소 20개 기준 프레임당 0.5ms 이하.

**Constraints**:
- 전환 중 값마다 재구성(recomposition)이나 SwiftUI body 재평가를 일으키지 않는다. 애니메이션 값은 레이아웃·그리기 단계에서만 읽는다.
- Android API 24~29는 `NumberFormatter`가 없어 폴백 경로를 쓴다. 두 경로가 같은 픽스처를 통과해야 한다.
- 개발 PC가 Windows이므로 iOS 빌드·테스트는 macOS(로컬 Mac 또는 CI의 macOS 러너)가 필요하다.
- SPM 배포를 위해 `Package.swift`는 저장소 루트에 있어야 한다.

**Scale/Scope**: 공개 컴포넌트 2개(`RollingNumber`, `RollingNumberGroup`) × 2개 플랫폼. 숫자 하나에 표시 요소 최대 약 30개.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md`가 아직 채워지지 않은 템플릿 상태라 적용할 원칙이나 게이트가 없다. **결과: 통과 (검사할 게이트 없음).**

참고로 흔히 쓰이는 원칙과의 대조:

| 일반 원칙 | 이 설계의 상태 |
|-----------|---------------|
| 라이브러리 우선, 독립 테스트 가능 | 분해·diff·모션 로직은 UI 없이 단독 테스트 가능한 순수 코드 |
| 테스트 우선 | 공유 JSON 픽스처를 먼저 작성하고 두 구현이 이를 통과해야 함 |
| 단순성 | 런타임 서드파티 의존성 0개, 공개 컴포넌트 2개, 크로스플랫폼 빌드 도구 없음 |

**Phase 1 이후 재검사**: 설계 산출물(data-model, contracts)이 위 표와 어긋나지 않음. 통과.

## Project Structure

### Documentation (this feature)

```text
specs/001-rolling-number-display/
├── plan.md              # 이 문서
├── research.md          # Phase 0: 기술 결정과 근거
├── data-model.md        # Phase 1: 엔티티, 조각 키 규칙, diff 규칙, 상태 전이
├── quickstart.md        # Phase 1: 빌드·테스트·수동 검증 절차
├── contracts/
│   ├── android-api.md   # Android 공개 API (Compose)
│   ├── ios-api.md       # iOS 공개 API (SwiftUI)
│   ├── logic-api.md     # 두 플랫폼이 같은 모양으로 구현할 순수 로직 API
│   └── fixtures.md      # 공유 JSON 테스트 케이스 형식
└── tasks.md             # Phase 2 (/speckit-tasks에서 생성)
```

### Source Code (repository root)

```text
rolling-number/
├── settings.gradle.kts               # Android 모듈 포함
├── build.gradle.kts
├── gradle/libs.versions.toml
├── Package.swift                     # SPM 패키지 (저장소 루트 필수)
│
├── fixtures/                         # 두 플랫폼 공유 JSON 테스트 케이스
│   ├── format/                       # 값·로케일·포맷 → 기대 조각 목록 (OS 포맷터 포함)
│   ├── diff/                         # 이전/다음 조각 → 기대 diff 결과 (순수 로직만)
│   └── session/                      # 시간 순 입력 → 기대 이벤트·알림 (선택)
│
├── android/
│   ├── rollingnumber/                # Android 라이브러리 (Maven Central 배포)
│   │   └── src/
│   │       ├── main/kotlin/io/github/komodgn/rollingnumber/
│   │       │   ├── RollingNumber.kt          # 공개 컴포저블
│   │       │   ├── RollingNumberGroup.kt
│   │       │   ├── RollingNumberFormat.kt    # 공개 포맷 설정
│   │       │   ├── logic/                    # 순수 Kotlin (Android API 의존 없음)
│   │       │   │   ├── DecimalValue.kt
│   │       │   │   ├── Tokenizer.kt          # 문자열 → 조각
│   │       │   │   ├── TransitionDiff.kt     # 조각 diff
│   │       │   │   ├── motion/               # 스프링/트윈, 요소 상태, 레이어
│   │       │   │   ├── RollingSession.kt     # 상태 기계, 이벤트
│   │       │   │   └── AnnouncementCoordinator.kt
│   │       │   ├── format/                   # ICU 포맷터 (API 30+ / 폴백)
│   │       │   ├── render/                   # 커스텀 Layout + 그리기, 페이드 마스크
│   │       │   ├── a11y/                     # 시맨틱, 알림
│   │       │   └── motion/                   # 프레임 구동, 동작 줄이기 감지
│   │       ├── test/                         # JVM: 로직 + diff 픽스처 / Robolectric: 포맷 픽스처
│   │       └── androidTest/                  # 기기: 포맷 픽스처(API 24·29·30·최신), Compose UI
│   ├── benchmark/                    # Macrobenchmark (SC-002)
│   └── sample/                       # 쿠폰 적용 결제 화면 데모 (Compose)
│
├── Sources/RollingNumber/            # Swift Package 소스
│   ├── RollingNumber.swift           # 공개 SwiftUI 뷰와 수정자
│   ├── RollingNumberGroup.swift
│   ├── RollingNumberFormat.swift
│   ├── Logic/                        # 순수 Swift (SwiftUI 의존 없음)
│   │   ├── DecimalValue.swift
│   │   ├── Tokenizer.swift
│   │   ├── TransitionDiff.swift
│   │   ├── Motion/
│   │   ├── RollingSession.swift
│   │   └── AnnouncementCoordinator.swift
│   ├── Format/                       # FormatStyle / NumberFormatter
│   ├── Render/                       # TimelineView + Canvas
│   └── Accessibility/
├── Tests/RollingNumberTests/         # XCTest: diff·포맷·세션 픽스처, 로직 단위 테스트
│
└── samples/ios/CouponCheckout/       # 쿠폰 적용 결제 화면 데모 (SwiftUI, 로컬 패키지 참조)
```

**Structure Decision**: 저장소 하나에 Android(Gradle)와 iOS(SPM)를 나란히 둔다. `Package.swift`는 SPM 원격 의존성 요구로 루트에 두고, Gradle 모듈은 `android/` 아래에 둔다. 두 플랫폼 모두 `logic/`(Logic/) 폴더에 UI 프레임워크를 import하지 않는 순수 코드를 두고, 이 부분이 공유 픽스처의 검증 대상이다. 픽스처는 루트 `fixtures/` 한 곳에만 있고 두 플랫폼 테스트가 직접 읽는다. 패키지 네임스페이스 `io.github.komodgn.rollingnumber`는 임시이며 배포 전에 확정한다.

## Complexity Tracking

헌법 위반 사항이 없어 기록할 항목이 없다. 참고: 로직을 두 벌 구현하는 대가(차이 발생 위험)는 공유 픽스처로 막는다. 득실은 [research.md](./research.md) R1에 있다.
