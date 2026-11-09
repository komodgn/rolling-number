# Contract: 공유 JSON 테스트 케이스

**위치**: 저장소 루트 `fixtures/` | **소비자**: Android 테스트(`android/rollingnumber/src/test`, `androidTest`), iOS 테스트(`Tests/RollingNumberTests`)

두 플랫폼은 같은 파일을 읽고 같은 기대값을 검증한다. 파일 하나에 케이스 여러 개가 들어간다. 모든 파일은 UTF-8이고, 보이지 않는 공백은 이스케이프(` `, ` `)로 쓴다.

## 공통 머리

```json
{
  "schema": 1,
  "kind": "format | diff | session",
  "cases": [ ... ]
}
```

`schema`가 모르는 값이면 테스트는 실패해야 한다(조용히 건너뛰지 않음).

## 1. `format/*.json`: 입력값·로케일·포맷 → 기대 조각 목록

OS 포맷터 + 조각 분해를 함께 검증한다.

```json
{
  "id": "krw-basic",
  "spec": "US2-1",
  "input": {
    "value": "12900",
    "locale": "ko-KR",
    "format": { "style": "currency", "currency": "KRW" },
    "prefix": "",
    "suffix": ""
  },
  "expect": {
    "normalized": "12,900",
    "tokens": [
      { "kind": "literal", "key": "Lp0", "text": "₩" },
      { "kind": "int", "key": "I4", "text": "1" },
      { "kind": "int", "key": "I3", "text": "2" },
      { "kind": "group", "key": "G3", "text": "," },
      { "kind": "int", "key": "I2", "text": "9" },
      { "kind": "int", "key": "I1", "text": "0" },
      { "kind": "int", "key": "I0", "text": "0" }
    ]
  }
}
```

- `value`는 항상 문자열(정확한 10진).
- `format`: `style`(`number`/`currency`/`percent`/`compact`), `currency?`, `minFraction?`, `maxFraction?`, `grouping?`(기본 true), `rounding?`(`halfUp` 기본, `halfEven`/`down`/`up`).
- `normalized`: 자리는 그대로, 소수점은 `.`, 구분자는 `,`, 부호는 `-`, 리터럴·접두·접미 제거. **두 플랫폼에서 반드시 같아야 한다**(SC-003).
- 조각 비교 규칙:
  - `int`, `frac`, `sign`, `prefix`, `suffix`: kind, key, text 모두 정확히 비교.
  - `decimal`, `group`: kind와 key를 비교하고, text는 해당 로케일 기호와 같은지 확인.
  - `literal`: kind와 key를 비교. text는 공백 문자(U+0020, U+00A0, U+202F)와 방향 표시 문자(U+200E, U+200F, U+061C)를 없앤 뒤 비교한다(FR-003이 허용한 OS 차이). 정확한 비교가 필요하면 `"textMatch": "exact"`, 기호 모양 자체가 OS마다 다를 수 있으면(예: 엔화 `¥`/`￥`) `"textMatch": "any"`(종류·키만 비교).
  - `decimal`, `group`도 위의 느슨한 비교를 쓴다(프랑스어 구분자 공백 종류 차이 대응).
- 선택 필드 `"platforms": ["android", "ios"]`: 한 플랫폼에서만 검증할 케이스(가능한 한 쓰지 않음, 쓰면 `reason` 필수).

## 2. `diff/*.json`: 이전·다음 → 기대 diff 결과

OS 포맷터를 거치지 않는 순수 로직 검증이다. **두 플랫폼 결과가 글자 단위까지 같아야 한다**(FR-023).

```json
{
  "id": "digit-count-grows",
  "spec": "US1-2",
  "trend": "auto",
  "symbols": { "decimal": ".", "group": ",", "minus": ["-", "−"] },
  "from": { "text": "₩9,900", "value": "9900", "prefix": "", "suffix": "" },
  "to":   { "text": "₩12,900", "value": "12900", "prefix": "", "suffix": "" },
  "expect": {
    "direction": "up",
    "elements": [
      { "key": "Lp0", "kind": "literal", "from": "₩", "to": "₩", "action": "stay" },
      { "key": "I4", "kind": "int", "from": null, "to": "1", "action": "enter-up" },
      { "key": "I3", "kind": "int", "from": "9", "to": "2", "action": "roll-up" },
      { "key": "G3", "kind": "group", "from": ",", "to": ",", "action": "stay" },
      { "key": "I2", "kind": "int", "from": "9", "to": "9", "action": "stay" },
      { "key": "I1", "kind": "int", "from": "0", "to": "0", "action": "stay" },
      { "key": "I0", "kind": "int", "from": "0", "to": "0", "action": "stay" }
    ]
  }
}
```

- `trend`: `auto` / `alwaysUp` / `alwaysDown` / `perDigit`.
- `from`이 `null`(또는 생략)이면 첫 표시. 케이스의 `"animateFirstDisplay"`(기본 false)가 false면 `elements`는 빈 목록이고, true면 모든 자리 조각이 `enter-up`, 나머지는 `enter-fade`다.
- `symbols`는 생략하면 `{ "decimal": ".", "group": ",", "minus": ["-", "−"] }`.
- `from`/`to`의 `formatKey`는 생략하면 `"default"`.
- `elements` 순서까지 비교한다. action 값과 순서 규칙은 [data-model.md §6](../data-model.md#6-transitiondiff-diff-결과).

### 최소 수록 케이스 (스펙 엣지 케이스·수용 시나리오 전부)

| id | 내용 | 출처 |
|----|------|------|
| roll-down-single | 15,000 → 12,000 | US1-1 |
| digit-count-grows | 9,900 → 12,900 | US1-2 |
| group-appears | 900 → 1,900 | US1-3 |
| digit-count-shrinks | 12,900 → 9,900 | US1-4 |
| first-display | null → 12,900 (animate 꺼짐/켜짐) | US1-5, 엣지 |
| same-value | 12,900 → 12,900 | US1-6, FR-012 |
| zero-to-positive / positive-to-zero | 0 ↔ 5,000 | 엣지 |
| positive-to-negative / negative-to-positive | 500 ↔ -300 (`-₩300`) | 엣지 |
| trailing-zero-same-value | 12.5 → 12.50 | 엣지 |
| fraction-drops | 12.99 → 13 | 엣지 |
| billion | 999,999,999 → 1,000,000,000 | 엣지 |
| compact-unit-appears | 999 → 1K | 엣지 |
| compact-unit-changes | 1.2K → 1.2M | 엣지 |
| affix-changes | "/월" → "/년" | 엣지 |
| trend-always-up | 15,000 → 12,000, alwaysUp | US6-1 |
| trend-per-digit | 19 → 21, perDigit | US6-2 |
| de-de-currency | 1.234,56 € → 1.299,00 € | US2-3 |

## 3. `session/*.json`: 시간 순 입력 → 기대 이벤트·알림

세션 상태 기계와 알림 조정기를 가상 시계로 검증한다.

```json
{
  "id": "rapid-taps-single-end",
  "spec": "US3-3, US5-2",
  "members": [ { "id": "total", "label": "최종가", "announce": true } ],
  "steps": [
    { "t": 0,   "set": { "total": { "text": "₩15,000", "value": "15000" } } },
    { "t": 100, "set": { "total": { "text": "₩12,000", "value": "12000" } } },
    { "t": 250, "set": { "total": { "text": "₩9,000", "value": "9000" } } },
    { "t": 2000, "advance": true }
  ],
  "expect": [
    { "event": "start", "member": "total", "t": 100 },
    { "event": "end", "member": "total" },
    { "announce": "최종가 ₩9,000" }
  ]
}
```

- `t`는 ms. 첫 `set`은 첫 표시다.
- 다른 step 종류: `{ "disableAnimation": "total" }`, `{ "reduceMotion": true }`, `{ "itemKey": { "total": "row-7" } }`.
- `members`가 2명 이상이면 그룹이다(알림 병합 검증, US5-6, US5-7).
- 비교: 이벤트·알림의 종류·순서·횟수는 정확히 비교. `end`의 시각은 `t`가 있으면 ±1프레임(16.7ms) 허용.
