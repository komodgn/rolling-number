# Research: 롤링 숫자 표시 컴포넌트

**Date**: 2026-11-10 | **Plan**: [plan.md](./plan.md)

기술 스택(Kotlin/Compose minSdk 24, Swift/SwiftUI iOS 15+, android.icu, FormatStyle/NumberFormatter, Maven Central, SPM, 플랫폼별 구현 + 공유 JSON 테스트)은 사용자가 지정했다. 아래는 그 안에서 정한 세부 결정과 근거다. Technical Context에 남은 미결정 사항은 없다.

---

## R1. 로직 공유 방식: 플랫폼별 구현 + 공유 JSON 테스트

- **Decision**: 조각 분해(Tokenizer), diff(TransitionDiff), 모션, 세션 상태 기계, 알림 조정을 Kotlin과 Swift로 **각각** 구현한다. 두 구현은 [contracts/logic-api.md](./contracts/logic-api.md)의 같은 API 모양과 [data-model.md](./data-model.md)의 같은 규칙을 따르고, `fixtures/`의 JSON 케이스를 모두 통과해야 한다.
- **Rationale**: (사용자 지정) 각 플랫폼 사용자가 순수 Kotlin/Swift 라이브러리 하나만 받으며, 크로스플랫폼 빌드 도구나 인터롭 계층이 없다. iOS 패키지가 가볍고 Windows에서도 Android 쪽 개발이 독립적이다.
- **Risk와 대응**: 로직이 두 벌이라 미묘한 차이가 생길 수 있다.
  - 모든 엣지 케이스와 수용 시나리오를 `diff/` 픽스처로 만든다(SC-003).
  - 규칙을 고칠 때는 픽스처를 먼저 고치고 두 구현을 같은 PR에서 고친다.
  - 로직 폴더는 UI 프레임워크를 import하지 않게 해 테스트가 빠르고 결정적이다.
- **Alternatives considered**: Kotlin Multiplatform 공유 코어(동등성은 구조적으로 보장되지만 iOS에 Kotlin/Native 바이너리가 들어가고 빌드가 복잡해짐. 사용자 결정으로 제외).

## R2. 숫자 포맷 (FR-002, FR-003)

- **Decision**:
  - **Android API 30+**: `android.icu.number.NumberFormatter`. 스타일별 설정: 통화 `unit(Currency)`, 퍼센트 `unit(MeasureUnit.PERCENT)` + `scale(Scale.powerOfTen(2))`, 축약 `notation(Notation.compactShort())`, 소수 자리 `precision(Precision.minMaxFraction(…))`, 구분자 `grouping(...)`, 반올림 `roundingMode(...)`.
  - **Android API 24~29 폴백**: `android.icu.text.NumberFormat`(일반 `getInstance`, 통화 `getCurrencyInstance`, 퍼센트 `getPercentInstance`) + `android.icu.text.CompactDecimalFormat`(축약, `SHORT`). 소수 자리·구분자·반올림은 `setMinimum/MaximumFractionDigits`, `setGroupingUsed`, `setRoundingMode`.
  - **iOS 15+**: `Decimal.FormatStyle`(일반), `Decimal.FormatStyle.Percent`, `Decimal.FormatStyle.Currency`, 축약 `.notation(.compactName)`. 자리·구분자·반올림은 `.precision(.fractionLength(min...max))`, `.grouping(...)`, `.rounded(rule:)`. 기호 조회는 `NumberFormatter`(`decimalSeparator`, `groupingSeparator`, `currencyDecimalSeparator`, `minusSign`).
  - 로케일 기본 숫자가 비라틴이면 라틴 숫자를 강제한다. Android `ULocale`에 `numbers=latn`, iOS `Locale(identifier: "<id>@numbers=latn")`.
  - 반올림 매핑: HalfUp → Android `RoundingMode.HALF_UP`, iOS `.toNearestOrAwayFromZero`. HalfEven → `HALF_EVEN` / `.toNearestOrEven`. Down → `DOWN` / `.towardZero`. Up → `UP` / `.awayFromZero`.
- **Rationale**: 스펙 FR-003 결정(각 OS 로케일 규칙). API 30 경계는 `NumberFormatter` 도입 시점이다. 두 Android 경로가 같은 결과를 내는지는 같은 `format/` 픽스처로 API 24, 29, 30, 최신 기기에서 각각 검증한다.
- **Known risk**: 폴백 `CompactDecimalFormat`과 `NumberFormatter.compactShort()`가 반올림·소수 자리 처리에서 다를 수 있다. 다르면 축약 표기는 API 레벨과 상관없이 `CompactDecimalFormat` 하나로 통일하는 것을 우선 검토한다.
  - **검증 결과 (2026-11-10, T060)**: 두 경로 모두 기본 소수 자리를 명시적으로 지정하고 `CompactDecimalFormat`의 유효 숫자 모드를 끄면 `format/compact.json`(en-US K/M, ko-KR 천/만, half-up/half-even) 결과가 같다. Robolectric API 29·33과 실제 기기 API 36에서 확인했다. 통일은 필요 없었다.
  - **알려진 차이 (미해결)**: Android는 구분자를 항상 패턴대로 넣고(`ON_ALIGNED`, 레거시와 동일), iOS `.automatic`은 로케일의 최소 그룹 자릿수를 따른다. es-ES처럼 4자리 수에 구분자를 넣지 않는 로케일에서 두 플랫폼 결과가 다를 수 있다. 현재 픽스처 로케일(ko, en, de, ja, hi, ar)에는 해당하지 않는다.

## R3. 정확한 10진 값 (FR-001, 큰 수 엣지 케이스)

- **Decision**:
  - 공개 입력: Android `BigDecimal`(권장)·`Long`·`Int`·`Double`, iOS `Decimal`(권장)·`Int`·`Double`.
  - `Double`은 Android `BigDecimal.valueOf(d)`, iOS `Decimal(string: "\(d)")`로 최단 10진 표현으로 바꾼다.
  - 포맷은 10진 타입 그대로 넘긴다(Android `BigDecimal`, iOS `Decimal`). 15자리 이상도 오차가 없다.
  - 로직 계층은 정규화된 문자열 기반 `DecimalValue`로 증감만 비교한다. 연산은 하지 않는다.
- **Rationale**: 로직이 필요한 것은 비교뿐이고, 문자열 정규형은 두 플랫폼에서 똑같이 구현·검증하기 쉽다(픽스처 입력도 문자열).
- **Alternatives considered**: `Double` 저장(오차와 15자리 초과 손실로 탈락).

## R4. 조각 분해와 위치 키 (FR-006, FR-008)

- **Decision**: 포맷 결과 문자열과 그 포맷에 쓰인 **기호 정보**(소수 구분자, 천 단위 구분자, 마이너스 기호 목록)로 글자를 분류하고, 소수점 기준 위치 키를 붙인다. 조각 종류는 정수 자리, 소수 자리, 소수점, 구분자, 부호, 리터럴(통화 기호·퍼센트·축약 단위·공백), 접두, 접미다. 키 규칙은 [data-model.md §5](./data-model.md#5-token-조각).
  - 기호 출처: Android `android.icu.text.DecimalFormatSymbols.getInstance(ulocale)`(통화 스타일이면 `monetaryDecimalSeparator`, `monetaryGroupingSeparator`), iOS `NumberFormatter`(같은 로케일·스타일로 생성).
- **Rationale**: 통화 기호 위치("1.234,56 €" / "₩12,900"), 부호 위치("-₩300"), 축약 단위("1.2K", "1.2천")가 로케일마다 달라도 같은 규칙으로 처리된다. 순수 문자열 처리라 두 플랫폼 구현이 단순하고 픽스처로 검증하기 쉽다.
- **Alternatives considered**: 포맷터의 필드 위치 정보(Android `FormattedNumber.nextFieldPosition`, iOS `AttributedString` 속성) 사용. 정확하지만 API 30 미만에는 같은 API가 없고 플랫폼마다 모양이 달라 탈락.

## R5. 모션 엔진 (FR-009~FR-011, FR-016, FR-020)

- **Decision**: 두 플랫폼에 **같은 알고리즘의 자체 모션 엔진**을 둔다. 플랫폼 프레임 시계가 매 프레임 시각(ns)을 넘기면 엔진이 요소별 폭 비율·세로 위치·투명도를 계산한다.
  - 곡선: `Spring(stiffness, dampingRatio)`(질량 1), `Tween(durationMs, cubic-bezier)`.
  - 스프링 적분: 반암시적 오일러, 고정 하위 스텝 최대 4ms. 프레임 간격이 흔들려도 결과가 안정적이다.
  - 중단 처리: 목표가 바뀌어도 현재 값과 속도를 유지한다(스프링). 트윈은 현재 값에서 새 트윈을 시작한다.
  - 숫자 자리는 글자 레이어 목록으로 표현한다. 새 목표 글자가 이미 레이어에 있으면 되돌리고, 없을 때만 새로 만든다(중복 금지, US3-2).
  - 정착 판정: 위치 오차 < 0.001줄, 속도 < 0.01줄/초, 투명도 오차 < 0.005, 폭 비율 오차 < 0.001.
  - 기본값(초기 제안, 디자인 검토에서 조정):

    | 곡선 | stiffness | dampingRatio | 대략 정착 시간 |
    |------|-----------|--------------|---------------|
    | 레이아웃(폭·위치) | 300 | 1.0 | ~0.45초 |
    | 숫자 회전 | 380 | 0.8 (약간 튕김) | ~0.5초 |
    | 페이드 | 500 | 1.0 | ~0.3초 |

- **Rationale**:
  - iOS 15에는 SwiftUI 애니메이션 완료 콜백(iOS 17+)이 없어 FR-020 종료 이벤트를 정확히 만들 수 없다.
  - Compose와 SwiftUI의 내장 스프링은 공식과 정착 기준이 달라, 같은 엔진을 쓰지 않으면 두 플랫폼의 체감과 이벤트 시점이 달라진다.
  - 가상 시계로 프레임 연속성(SC-006)을 단위 테스트할 수 있다. `session/` 픽스처로 두 엔진의 이벤트 시점도 비교한다(허용 오차 1프레임).
- **Alternatives considered**: Compose `Animatable` + SwiftUI `withAnimation`(iOS 15 완료 콜백 없음, 플랫폼 간 차이로 탈락).

## R6. 그리기와 성능 (FR-015, FR-021, FR-022, SC-002)

- **Decision**:
  - Android: 커스텀 `Layout`이 엔진이 계산한 요소 폭의 합으로 크기를 정하고, `drawWithCache`에서 `TextMeasurer`로 미리 측정한 글자를 그린다. 애니메이션 상태는 레이아웃·그리기 단계에서만 읽는다. 프레임은 애니메이션 중에만 `withFrameNanos`로 구동한다.
  - iOS: `TimelineView(.animation(paused: !isAnimating))` 안의 `Canvas`(둘 다 iOS 15+)가 해석된 `Text`를 그린다. 폭은 `.frame(width:)`로 반영한다. 글꼴과 색은 표준 `.font`, `.foregroundColor`를 따른다(`.foregroundStyle`도 지원).
  - 고정폭 숫자(FR-021): 0~9 중 가장 넓은 글자 폭으로 숫자 칸을 고정하고 가운데 정렬한다. 글꼴의 `tnum` 지원 여부와 상관없이 흔들리지 않는다.
  - 페이드 마스크(FR-022): 숫자 칸 위아래 선형 그라디언트. Android는 오프스크린 합성 + `BlendMode.DstIn`, iOS는 `GraphicsContext` 레이어 마스크.
- **Rationale**: 매 프레임 UI 트리를 다시 만들지 않으므로 리스트 셀이 많아도 60fps 여유가 있다.
- **Alternatives considered**: 자리마다 `Text`를 두고 offset 애니메이션(재구성 비용이 커서 탈락).

## R7. 접근성 (FR-018, FR-019, FR-019a)

- **Decision**:
  - **값 노출**: 컴포넌트 전체가 하나의 접근성 요소. "라벨 + 목표 값 전체 문자열"만 노출한다. Android `clearAndSetSemantics { contentDescription = … }`, iOS `.accessibilityElement(children: .ignore)` + `.accessibilityLabel` / `.accessibilityValue`.
  - **변경 알림**: 로직 계층의 `AnnouncementCoordinator`가 "언제, 무엇을" 알릴지 정한다(정착 후 1회, 그룹은 병합 1회). 플랫폼 계층은 받은 문자열을 읽게만 한다.
    - Android: 숫자(또는 그룹)마다 화면에 보이지 않는 1px 알림 전용 시맨틱 노드를 `liveRegion = Polite`로 두고, 알림 시점에만 텍스트를 바꾼다. API 36에서 `announceForAccessibility`가 사용 중단되어 라이브 리전을 기본으로 한다.
    - iOS: `UIAccessibility.post(notification: .announcement, argument:)`에 `.accessibilitySpeechQueueAnnouncement: true` 속성 문자열을 넘겨, 읽던 내용을 끊지 않고 뒤에 이어 읽게 한다.
- **Risk / 검증 필요**: TalkBack이 숨김 노드의 라이브 리전 변경을 무시할 수 있다. 기기 검증(quickstart 접근성 절차)에서 실패하면 API 35 이하에서는 `View.announceForAccessibility`로 대체한다.
- **구현 중 변경 (2026-11-10)**: Android는 숨김 라이브 리전 노드 대신 `View.announceForAccessibility`를 모든 API 레벨에서 쓴다. 숨김 노드는 메인 노드의 `clearAndSetSemantics`에 지워지거나, 지우지 않으면 TalkBack이 초점을 줄 수 있는 별도 노드가 되어 FR-018("노드 하나")과 충돌했다. 이 API는 API 36에서 사용 중단 표시만 됐고 여전히 TYPE_ANNOUNCEMENT를 보낸다. 알림 판단 규칙(정착 후 1회, 마지막 값만, 그룹 병합)은 `session/` 픽스처로 검증했다. **실제 TalkBack 낭독은 아직 기기에서 확인하지 않았다(T077)**. API 36 이상에서 낭독되지 않으면 라이브 리전 방식으로 되돌린다.
- **Alternatives considered**: 메인 노드를 라이브 리전으로 지정(목표가 바뀌는 즉시 읽혀 "정착 후 1회" 규칙을 지키기 어려워 탈락).

## R8. 동작 줄이기 감지 (FR-017)

- **Decision**:
  - Android: `Settings.Global.ANIMATOR_DURATION_SCALE == 0`(설정의 "애니메이션 삭제")을 동작 줄이기로 본다. `ContentObserver`로 실행 중 변경도 반영한다.
  - iOS: `@Environment(\.accessibilityReduceMotion)`.
  - 켜지는 순간 진행 중 전환은 즉시 완료(종료 이벤트 발생).
- **Rationale**: Android에는 iOS "동작 줄이기"와 같은 단일 공개 API가 없고, 애니메이터 배율 0이 시스템 전체에서 같은 의미로 쓰인다.

## R9. 그룹 동기화 (FR-015)

- **Decision**: `RollingNumberGroup`이 그룹원에게 **공유 프레임 시계**와 `AnnouncementCoordinator`를 제공한다(Android `CompositionLocal`, iOS `Environment`). 같은 프레임에 들어온 변경은 같은 프레임에서 시작한다. 각 숫자 폭이 매 프레임 부드럽게 변하므로 같은 줄의 이웃은 일반 `Row`/`HStack` 레이아웃으로 같은 곡선을 따라 움직인다.
- **Rationale**: 그룹이 레이아웃을 소유하지 않아 개발자가 배치를 자유롭게 정할 수 있다.

## R10. 공유 JSON 테스트 케이스 (SC-003~SC-007)

- **Decision**: 픽스처는 세 종류로 나눈다. 형식은 [contracts/fixtures.md](./contracts/fixtures.md).
  1. **`format/`**: 입력값·로케일·포맷 → 기대 조각 목록. OS 포맷터를 거치므로 FR-003이 허용한 차이(공백 종류 등)는 리터럴 조각의 텍스트 비교에서 제외한다. 숫자·소수점·구분자·부호 조각은 종류·키·정규화 텍스트를 정확히 비교한다.
  2. **`diff/`**: 이전·다음 조각(또는 포맷된 문자열 + 기호) → 기대 diff 결과. OS 포맷터를 거치지 않으므로 두 플랫폼 결과가 **글자 단위까지 같아야** 한다(FR-023의 핵심 검증).
  3. **`session/`**: 시간 순 입력(값 변경, 끄기, 식별자 변경, 프레임 진행) → 기대 이벤트·알림 순서.
  - 읽기: Android는 `test` 리소스 경로에 루트 `fixtures/`를 추가(`sourceSets.test.resources.srcDir(rootProject.file("fixtures"))`, 기기 테스트는 `androidTest` assets에 추가). iOS는 `#filePath` 기준 상대 경로로 루트 `fixtures/`를 읽는다(SPM 리소스는 대상 폴더 밖 파일을 포함할 수 없어서).
  - 실행 위치: Android 로직·diff 픽스처는 JVM 단위 테스트, `format/` 픽스처는 Robolectric과 기기(API 24, 29, 30, 최신) 양쪽. iOS는 XCTest(iOS 15 시뮬레이터와 최신 시뮬레이터).
  - 연속성(SC-006): 가상 시계로 프레임을 샘플링해 어떤 요소도 한 프레임에 0.35줄 또는 투명도 0.35 넘게 변하지 않고, 같은 키의 요소·레이어가 둘 생기지 않음을 확인한다.
  - 성능(SC-002): Android Macrobenchmark `FrameTimingMetric`, iOS Instruments(Animation Hitches)로 샘플 결제 화면을 측정한다.

## R11. 배포

- **Decision**:
  - Android: Maven Central에 `io.github.komodgn:rollingnumber` AAR 하나. `maven-publish` + Central Portal 업로드, 서명 필요.
  - iOS: 저장소 루트 `Package.swift`의 library product `RollingNumber`. 버전은 git 태그(semver). Android와 같은 버전 번호를 쓴다.
- **Alternatives considered**: CocoaPods(신규 배포 중단 추세라 탈락).

## R12. 샘플 앱

- **Decision**: 두 플랫폼에 같은 시나리오의 **쿠폰 적용 결제 화면**을 만든다.
  - 결제 화면: 상품 수량 ±, 쿠폰 2종 토글, 배송비 조건(예: 30,000원 이상 무료), [원가][할인액][배송비][최종가] 그룹(라벨 지정).
  - 설정: 로케일 전환 메뉴(ko-KR, en-US, de-DE, ja-JP), 애니메이션 끄기 토글, 동작 줄이기 무시 토글.
  - 디버그: 금액을 음수로 만드는 버튼(부호 엣지 케이스).
  - "주문 내역" 탭: 숫자 셀 100개 리스트, 1초마다 일부 값 변경(셀 재사용 검증, SC-007).
  - 시나리오 검증([quickstart.md](./quickstart.md))과 성능 측정(R10)에 같이 쓴다.

## R13. 개발 환경 제약

- **Decision**: Android 개발·테스트는 현재 Windows PC에서 한다. iOS 빌드·테스트는 macOS가 필요하므로 CI(GitHub Actions macOS 러너)나 Mac에서 실행한다. CI는 PR마다 Android 단위·Robolectric 테스트와 iOS XCTest를 둘 다 돌려 픽스처 동등성을 확인한다.
