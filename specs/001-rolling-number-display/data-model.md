# Data Model: 롤링 숫자 표시 컴포넌트

**Date**: 2026-11-10 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

저장소(DB)는 없다. 아래는 Android(Kotlin)와 iOS(Swift)가 **같은 규칙으로 각각 구현**하는 메모리 내 모델이다. §3~§6의 규칙은 공유 JSON 테스트 케이스([contracts/fixtures.md](./contracts/fixtures.md))로 두 구현의 동등성을 검증한다. 공개 API 모양은 [contracts/](./contracts/)에 있다.

## 1. DecimalValue

표시할 정확한 10진 값 (FR-001, R3).

| 필드 | 타입 | 규칙 |
|------|------|------|
| negative | Boolean | 0은 항상 false (-0 금지) |
| integerDigits | String | `[0-9]+`, 앞자리 0 제거(값이 0이면 "0") |
| fractionDigits | String | `[0-9]*`, 끝자리 0 제거 (비교용 정규형) |

- `parse(text)`: `-?[0-9]+(\.[0-9]+)?` 형식만 받는다. NaN, 무한대, 빈 문자열은 `null`. 호출한 쪽은 마지막 유효 값을 유지하고 개발 경고를 남긴다.
- `compareTo`: 부호 → 정수 자릿수 길이 → 정수 문자열 → 소수 문자열 순으로 비교한다.
- 12.5와 12.50은 같은 값이다(정규형이 같음).

## 2. RollingNumberFormat (포맷 설정)

FR-002~FR-004. 플랫폼 포맷 계층이 사용한다.

| 필드 | 타입 | 기본값 |
|------|------|--------|
| style | Number / Currency(code) / Percent / Compact | Number |
| minFractionDigits | Int? | 스타일·통화 기본값 |
| maxFractionDigits | Int? | 스타일·통화 기본값 |
| useGrouping | Boolean | true |
| rounding | HalfUp / HalfEven / Down / Up | HalfUp |
| locale | Locale? | null = 기기 로케일 |

- `minFractionDigits ≤ maxFractionDigits`, 둘 다 0~15. 어기면 생성 시 예외.
- 통화 코드는 ISO 4217 3글자 대문자. 플랫폼이 모르는 코드면 예외.
- Percent는 0.125 → "12.5%"처럼 입력값에 100을 곱해 표시한다.
- **formatKey**: 로케일·스타일·통화·자릿수·구분자·반올림을 이어 붙인 식별 문자열. 값이 같고 formatKey가 바뀌면 FR-014(즉시 교체)를 적용한다.

## 3. NumberSymbols

포맷에 쓰인 로케일 기호 (R4). 플랫폼 포맷 계층이 포맷터에서 읽는다.

| 필드 | 타입 | 예 (de-DE, 통화) |
|------|------|-----------------|
| decimalSeparator | String | "," |
| groupingSeparator | String | "." |
| minusSigns | Set<String> | {"-", "−"} (U+2212 포함) |

## 4. FormattedNumber

로직 계층의 입력 단위. 값 하나를 포맷할 때마다 만든다.

| 필드 | 타입 | 설명 |
|------|------|------|
| text | String | OS 포맷터 결과 (접두·접미 제외) |
| value | DecimalValue | 방향 판단용 |
| symbols | NumberSymbols | 조각 분류용 |
| prefix | String | 개발자 접두 (FR-004) |
| suffix | String | 개발자 접미 |
| formatKey | String | §2 참고 |

## 5. Token (조각)

`FormattedNumber`를 분해한 표시 요소 하나. 조각 목록은 `format/` 픽스처의 비교 대상이다.

| 필드 | 타입 | 설명 |
|------|------|------|
| kind | `int` / `frac` / `decimal` / `group` / `sign` / `literal` / `prefix` / `suffix` | 픽스처 표기와 같음 |
| text | String | 자리 조각은 한 글자, 리터럴은 연속된 글자 묶음 |
| key | String | 위치 키 (아래 규칙) |

### 위치 키 규칙 (FR-006)

1. **숫자 영역**: `text`에서 첫 숫자부터 마지막 숫자까지(사이의 소수점·구분자 포함).
2. 숫자 영역 안:
   - 소수점: `D`
   - 정수 자리: 소수점(없으면 숫자 영역 끝)에서 왼쪽으로 0부터 → `I0`, `I1`, `I2`…
   - 소수 자리: 소수점 오른쪽으로 0부터 → `F0`, `F1`…
   - 구분자: 오른쪽에 있는 정수 자리 개수 k → `G{k}` (예: "12,900"의 ","는 `G3`)
3. 숫자 영역 밖:
   - `minusSigns`에 속한 글자: `S` (한 문자열에 하나)
   - 나머지 연속 글자 묶음은 리터럴. 숫자 영역 앞쪽은 영역에서 바깥 방향으로 `Lp0`, `Lp1`…, 뒤쪽은 `Ls0`, `Ls1`…. 부호는 세지 않으며, 부호로 끊긴 리터럴은 별개 묶음이다.
4. 개발자 접두는 `AP`, 접미는 `AS` (빈 문자열이면 조각 없음).

예시:

| 표시 | 조각 (키) |
|------|-----------|
| `₩12,900` | ₩(Lp0) 1(I4) 2(I3) ,(G3) 9(I2) 0(I1) 0(I0) |
| `-₩300` | -(S) ₩(Lp0) 3(I2) 0(I1) 0(I0) |
| `1.234,56 €` (de-DE) | 1(I3) .(G3) 2(I2) 3(I1) 4(I0) ,(D) 5(F0) 6(F1) " €"(Ls0) |
| `1.2K` | 1(I0) .(D) 2(F0) K(Ls0) |
| `~₩12,900/월` | ~(AP) ₩(Lp0) … 0(I0) /월(AS) |

## 6. TransitionDiff (diff 결과)

이전·다음 조각을 키로 매칭한 결과 (FR-005~FR-008, FR-012). `diff/` 픽스처의 비교 대상이며 **두 플랫폼이 글자 단위까지 같아야 한다**(FR-023).

| 필드 | 타입 | 설명 |
|------|------|------|
| direction | `up` / `down` / `none` | 값 전체의 증감 (none = 값이 같음) |
| elements | List<DiffElement> | 그리는 순서대로 |

**DiffElement**: `key`, `kind`, `from`(이전 텍스트, 없으면 null), `to`(다음 텍스트, 없으면 null), `action`

### action 결정 규칙

`d` = 요소의 회전 방향. Trend가 `auto`면 direction, `alwaysUp`/`alwaysDown`이면 고정값, `perDigit`면 매칭된 자리는 자기 숫자의 증감(같으면 Stay), 등장·퇴장 자리는 direction. **direction이 `none`이면 Trend와 상관없이 d = none.**

| 상황 | action |
|------|--------|
| 양쪽에 있고 텍스트 같음 | `stay` |
| 양쪽 자리 조각, 텍스트 다름, d ≠ none | `roll-up` / `roll-down` |
| 양쪽 자리 조각, 텍스트 다름, d = none | `crossfade` |
| 양쪽 비자리 조각, 텍스트 다름 | `crossfade` |
| 다음에만 있는 자리 조각, d ≠ none | `enter-up` / `enter-down` (굴러 들어오며 페이드 인) |
| 다음에만 있는 자리 조각(d = none) 또는 비자리 조각 | `enter-fade` (제자리) |
| 이전에만 있는 자리 조각, d ≠ none | `exit-up` / `exit-down` (굴러 나가며 페이드 아웃) |
| 이전에만 있는 자리 조각(d = none) 또는 비자리 조각 | `exit-fade` (제자리) |

- 자리 조각 = `int`, `frac`. 비자리 조각 = 나머지.
- 방향 의미: `up` = 아래에서 들어와 위로 나감, `down` = 반대.
- 이전·다음 전체 표시 문자열(접두·접미 포함)이 같으면 diff 없음(`elements` 빈 목록, FR-012).
- **순서**: 다음 조각 순서를 기본으로 하고, 퇴장 요소는 이전 목록에서 바로 앞에 있던 요소 뒤에 끼운다. 앞 요소가 없으면 맨 앞.

## 7. MotionCurve / MotionSpec

| 타입 | 필드 |
|------|------|
| MotionCurve.Spring | stiffness (> 0), dampingRatio (> 0) |
| MotionCurve.Tween | durationMs (> 0), easing (cubic-bezier x1, y1, x2, y2) |
| MotionSpec | layout, roll, fade: MotionCurve / enabled = true / respectReduceMotion = true / animateFirstDisplay = false |

기본 곡선 값과 적분·정착 기준은 [research.md R5](./research.md)에 있으며 두 플랫폼이 같은 값을 쓴다.

## 8. ElementState (모션 엔진 내부)

화면의 요소 하나 (키당 최대 하나).

| 필드 | 설명 |
|------|------|
| key | 위치 키 |
| widthFactor | 0~1, layout 곡선 (등장 0→1, 퇴장 1→0) |
| layers | 글자 레이어 목록. 레이어 = text, offsetY(줄 단위 -1~1, roll 곡선), alpha(fade 곡선) |
| exiting | 퇴장 중 여부. 정착하면 제거 |

- 새 목표 글자가 이미 레이어에 있으면 그 레이어를 offsetY 0, alpha 1로 되돌리고 나머지 레이어는 퇴장 위치로 보낸다. 없을 때만 새 레이어를 만든다(FR-011 중복 금지).
- 퇴장 중인 요소의 키가 새 diff에 다시 나오면 같은 ElementState를 되살린다.

## 9. RenderFrame

모션 엔진이 매 프레임 그리기 계층에 주는 정보.

| 필드 | 설명 |
|------|------|
| totalWidth | 요소 폭 합 (px/pt) |
| items | 요소마다 x, width, 레이어별 text·offsetY·alpha |
| isAnimating | false면 프레임 구동 중지 |

글자 폭은 그리기 계층의 측정 함수(text → 폭)로 구한다. 고정폭 숫자 설정이면 자리 칸 폭은 0~9 중 최댓값이다(FR-021).

## 10. RollingSession (컴포넌트 하나의 상태)

| 필드 | 설명 |
|------|------|
| current | 마지막 목표 `FormattedNumber?` |
| itemKey | 항목 식별자 (FR-013) |
| phase | `empty` / `idle` / `animating` |
| pendingAnnouncement | 정착 후 알릴 것이 있는지 |
| label | 접근성 라벨 |
| announce | 알림 켜짐 여부 (기본 true) |

### 상태 전이

```text
          첫 값 (애니메이션 없음)
 empty ─────────────────────────────▶ idle
   │                                   │  ▲
   │ 첫 값 + animateFirstDisplay        │  │ 모든 요소 정착 → onEnd, 알림 처리
   ▼                                   ▼  │
 animating ◀──── 값 변경 (애니메이션 가능) ──┘
   │  ▲
   │  └── 값 변경 (진행 중) → 현재 상태에서 이어감 (이벤트 없음)
   │
   ├── 애니메이션 끄기 / 동작 줄이기 켜짐 → 즉시 완료 → onEnd → idle
   └── itemKey 변경 → 즉시 새 값 표시, onEnd 없음, 알림 취소 → idle
```

| 입력 | 조건 | 결과 |
|------|------|------|
| update(new) | 표시 문자열 같음 | 변화 없음 (이벤트·알림 없음) |
| update(new) | formatKey 바뀜 | 즉시 교체, 이벤트 없음. 값도 바뀌었으면 알림 |
| update(new) | 애니메이션 불가(꺼짐 / 동작 줄이기) | 즉시 교체, 이벤트 없음, 알림 즉시 |
| update(new) | idle, 애니메이션 가능 | diff 적용 → animating, onStart |
| update(new) | animating | diff 재적용(이어가기), 이벤트 없음 |
| setEnabled(false) / 동작 줄이기 켜짐 | animating | 즉시 완료 → onEnd → idle |
| setItemKey(k) | k 다름 | 즉시 표시, onEnd·알림 없음 |
| step(now) | 모든 요소 정착 | idle, onEnd, pendingAnnouncement 처리 |

알림은 값 또는 표시 문자열이 바뀐 경우에만 예약한다. 첫 표시와 itemKey 변경은 알리지 않는다.

## 11. AnnouncementCoordinator / Group

| 필드 | 설명 |
|------|------|
| members | 순서 있는 세션 목록 (등록 순서, 또는 개발자가 정한 order) |
| clock | 그룹 공유 프레임 시계 (R9) |
| pending | 알림 대기 중인 멤버 집합 |

- 그룹에 속하지 않은 세션은 자기 전용 조정기를 가진다(멤버 1명인 그룹과 같음).
- 그룹원은 개별 알림을 내지 않는다(FR-019a).
- 멤버가 알림을 예약하면 pending에 추가한다. pending의 모든 멤버가 idle이 되면, 멤버 순서대로 `"{라벨} {전체 표시 문자열}"`(라벨 없으면 표시 문자열만)을 `", "`로 이어 붙여 한 번 알리고 pending을 비운다.
- 알림을 끈 멤버는 pending에 들어가지 않는다.
