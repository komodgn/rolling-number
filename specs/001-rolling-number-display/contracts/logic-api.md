# Contract: 순수 로직 API (두 플랫폼 공통 모양)

Android `io.github.komodgn.rollingnumber.logic` 패키지와 iOS `Sources/RollingNumber/Logic/`가 **같은 이름·같은 의미**로 구현한다. 둘 다 라이브러리 내부(`internal`)이며 공개 API가 아니다. 동등성은 [fixtures.md](./fixtures.md)로 검증한다.

UI 프레임워크(Compose, SwiftUI, UIKit)와 OS 포맷터를 import하지 않는다. 테스트는 JVM 단위 테스트와 XCTest에서 기기 없이 돈다.

시그니처는 의미 전달용 의사 코드이며, 각 언어의 관례(Kotlin `data class`/`sealed interface`, Swift `struct`/`enum`)로 옮긴다.

## DecimalValue

```text
DecimalValue.parse(text: String) -> DecimalValue?      // 형식 밖이면 null/nil
DecimalValue.compare(a, b) -> -1 | 0 | 1
DecimalValue.canonical -> String                        // "-12.5", "0", "1000000000"
```

## Tokenizer

```text
Tokenizer.tokenize(number: FormattedNumber) -> [Token]
normalize(tokens: [Token]) -> String                    // fixtures의 "normalized"
```

- 입력 `FormattedNumber`(text, value, symbols, prefix, suffix, formatKey)는 [data-model.md §4](../data-model.md#4-formattednumber).
- 규칙은 data-model §5. 숫자가 하나도 없는 text는 오류(포맷 계층 버그)로 본다.

## TransitionDiff

```text
TransitionDiff.compute(
    from: FormattedNumber?,      // null = 첫 표시
    to: FormattedNumber,
    trend: Trend,                // auto | alwaysUp | alwaysDown | perDigit
    animateFirstDisplay: Bool
) -> DiffResult                  // direction, elements
```

- 규칙은 data-model §6. 출력은 fixtures `diff/`의 `expect`와 필드 단위로 같아야 한다.

## Motion

```text
MotionCurve = Spring(stiffness, dampingRatio) | Tween(durationMs, x1, y1, x2, y2)
MotionSpec(layout, roll, fade, enabled, respectReduceMotion, animateFirstDisplay)

MotionEngine(spec: MotionSpec)
  apply(diff: DiffResult, nowNanos: Long)   // 진행 중이면 현재 값·속도에서 이어감
  step(nowNanos: Long) -> Bool              // 계속 애니메이션 중이면 true
  snapToEnd()                               // 모든 요소를 최종 상태로
  frame(measure: (String) -> Float, tabularDigitWidth: Float?) -> RenderFrame
```

- 적분·정착 기준·기본 상수는 [research.md R5](../research.md)와 같아야 한다.

## RollingSession

```text
RollingSession(spec: MotionSpec, trend: Trend, coordinator: AnnouncementCoordinator)
  update(number: FormattedNumber, nowNanos: Long)
  setItemKey(key: Any?)
  setAnimationAllowed(allowed: Bool)        // enabled && !(reduceMotion && respectReduceMotion)
  step(nowNanos: Long) -> Bool
  listener: { onStart(), onEnd() }
  accessibilityText -> String               // 라벨 + 목표 전체 표시 문자열 (FR-018)
```

- 상태 전이표는 [data-model.md §10](../data-model.md#10-rollingsession-컴포넌트-하나의-상태).

## AnnouncementCoordinator

```text
AnnouncementCoordinator(announce: (String) -> Unit)
  register(session, order: Int?) / unregister(session)
  // 세션이 알림을 예약하고 정착하면 내부적으로 병합 규칙(data-model §11)에 따라 announce 호출
```

- 단독 컴포넌트는 멤버 1명짜리 조정기를 쓴다.
