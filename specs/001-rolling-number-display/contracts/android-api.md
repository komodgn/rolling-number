# Contract: Android 공개 API (Jetpack Compose)

**배포**: Maven Central `io.github.komodgn:rollingnumber:<version>` | **minSdk**: 24 | **패키지**: `io.github.komodgn.rollingnumber` (임시)

시그니처만 정의한다. 구현은 tasks 단계에서 다룬다.

## 최소 사용 (SC-001, FR-024)

```kotlin
RollingNumber(value = total, format = RollingNumberFormat.currency("KRW"))
```

## RollingNumber

```kotlin
@Composable
fun RollingNumber(
    value: BigDecimal,
    format: RollingNumberFormat = RollingNumberFormat.number(),
    modifier: Modifier = Modifier,
    locale: Locale? = null,                       // null = 기기 로케일 (FR-003)
    prefix: String = "",                          // FR-004
    suffix: String = "",
    trend: RollingTrend = RollingTrend.Auto,      // FR-005
    style: RollingNumberStyle = RollingNumberDefaults.style(),
    motion: RollingMotion = RollingNumberDefaults.motion(),
    itemKey: Any? = null,                         // FR-013 리스트 재사용
    accessibilityLabel: String? = null,           // FR-019 라벨
    announceChanges: Boolean = true,              // FR-019
    groupOrder: Int? = null,                      // 그룹 알림 순서 (null = 등록 순서)
    onAnimationStart: (() -> Unit)? = null,       // FR-020
    onAnimationEnd: (() -> Unit)? = null,
)
```

편의 오버로드: `value: Long`, `value: Int`, `value: Double`(내부에서 `BigDecimal.valueOf`). 나머지 매개변수는 같다.

## RollingNumberFormat

```kotlin
class RollingNumberFormat private constructor(...) {
    companion object {
        fun number(minFraction: Int? = null, maxFraction: Int? = null, grouping: Boolean = true, rounding: RollingRounding = RollingRounding.HalfUp): RollingNumberFormat
        fun currency(code: String, minFraction: Int? = null, maxFraction: Int? = null, grouping: Boolean = true, rounding: RollingRounding = RollingRounding.HalfUp): RollingNumberFormat
        fun percent(minFraction: Int? = null, maxFraction: Int? = null, rounding: RollingRounding = RollingRounding.HalfUp): RollingNumberFormat
        fun compact(maxFraction: Int? = 1, rounding: RollingRounding = RollingRounding.HalfUp): RollingNumberFormat
    }
}
enum class RollingRounding { HalfUp, HalfEven, Down, Up }
```

- 잘못된 값(min > max, 범위 밖, 모르는 통화 코드)은 `IllegalArgumentException`.
- 동등성(`equals`)은 data-model의 formatKey 기준.

## RollingTrend

```kotlin
enum class RollingTrend { Auto, AlwaysUp, AlwaysDown, PerDigit }
```

## RollingNumberStyle (FR-021, FR-022)

```kotlin
@Immutable
data class RollingNumberStyle(
    val textStyle: TextStyle,          // 글꼴, 크기, 색상, 굵기
    val tabularDigits: Boolean = true, // 고정폭 숫자
    val fadeMaskHeight: Dp = 4.dp,     // 0.dp = 마스크 없음
)
```

## RollingMotion (FR-010, FR-016, FR-017, FR-013)

```kotlin
@Immutable
data class RollingMotion(
    val layout: RollingCurve,
    val roll: RollingCurve,
    val fade: RollingCurve,
    val enabled: Boolean = true,
    val respectReduceMotion: Boolean = true,
    val animateFirstDisplay: Boolean = false,
)

sealed interface RollingCurve {
    data class Spring(val stiffness: Float, val dampingRatio: Float) : RollingCurve
    data class Tween(val durationMillis: Int, val easing: CubicBezierEasing) : RollingCurve
}

object RollingNumberDefaults {
    @Composable fun style(): RollingNumberStyle      // LocalTextStyle.current 기반
    fun motion(): RollingMotion                      // research R5 기본 곡선
}
```

## RollingNumberGroup (FR-015, FR-019a)

```kotlin
@Composable
fun RollingNumberGroup(content: @Composable () -> Unit)
```

- 레이아웃을 강제하지 않는다. `content` 안의 모든 `RollingNumber`가 같은 그룹이 된다(중첩 그룹은 가장 가까운 그룹).
- 그룹원은 개별 알림 대신 병합 알림을 낸다.

## 동작 보장

| 항목 | 보장 |
|------|------|
| 접근성 | 노드 하나, `contentDescription` = 라벨 + 목표 값 전체 문자열. 자식 노드 없음 |
| 스레드 | 모든 콜백은 메인 스레드 |
| 재구성 | 애니메이션 프레임마다 호출자 컴포저블을 재구성하지 않음 |
| 잘못된 값 | `Double.NaN`/무한대는 무시하고 마지막 유효 값 유지, `Log.w` 경고 |
