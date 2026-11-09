# Shared fixtures

Android와 iOS 테스트가 함께 읽는 JSON 테스트 케이스입니다. 형식의 기준 문서는
[`specs/001-rolling-number-display/contracts/fixtures.md`](../specs/001-rolling-number-display/contracts/fixtures.md)입니다.

| 폴더 | 검증 대상 |
|------|-----------|
| `format/` | 입력값·로케일·포맷 → 기대 조각 목록 (각 OS 포맷터 + 조각 분해) |
| `diff/` | 이전/다음 표시 → 기대 diff 결과 (순수 로직, 두 플랫폼이 글자 단위까지 같아야 함) |
| `session/` | 시간 순 입력 → 기대 이벤트·알림 |

모든 파일은 다음 머리를 가집니다. `schema`가 1이 아니면 테스트가 실패합니다.

```json
{ "schema": 1, "kind": "format | diff | session", "cases": [] }
```

규칙을 바꿀 때는 이 폴더의 기대값을 먼저 고치고, Android와 iOS 구현을 같은 PR에서 고칩니다.
