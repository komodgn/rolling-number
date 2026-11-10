import Foundation

/// Roll direction rule (FR-005). Mirrors Android `Trend`.
enum Trend: String {
    case auto
    case alwaysUp
    case alwaysDown
    case perDigit
}

enum Direction: String { case up, down, none }

enum DiffAction: String {
    case stay
    case rollUp = "roll-up"
    case rollDown = "roll-down"
    case crossfade
    case enterUp = "enter-up"
    case enterDown = "enter-down"
    case enterFade = "enter-fade"
    case exitUp = "exit-up"
    case exitDown = "exit-down"
    case exitFade = "exit-fade"
}

struct DiffElement: Equatable {
    var key: String
    var kind: TokenKind
    var from: String?
    var to: String?
    var action: DiffAction
}

struct DiffResult: Equatable {
    var direction: Direction
    var elements: [DiffElement]
    var isEmpty: Bool { elements.isEmpty }
}

/// Matches previous and next tokens by key and decides each element's action (data-model §6).
/// Same rules as Android `TransitionDiff.kt`; parity is checked by `fixtures/diff`.
enum TransitionDiff {
    static func compute(from: FormattedNumber?, to: FormattedNumber, trend: Trend, animateFirstDisplay: Bool = false) throws -> DiffResult {
        let newTokens = try Tokenizer.tokenize(to)

        guard let from = from else {
            if !animateFirstDisplay { return DiffResult(direction: .none, elements: []) }
            let d: Direction = trend == .alwaysDown ? .down : .up
            return DiffResult(direction: d, elements: newTokens.map {
                DiffElement(key: $0.key, kind: $0.kind, from: nil, to: $0.text, action: enterAction($0.kind, d))
            })
        }

        let c = DecimalValue.compare(to.value, from.value)
        let direction: Direction = c > 0 ? .up : (c < 0 ? .down : .none)
        if from.displayText == to.displayText { return DiffResult(direction: direction, elements: []) }

        let oldTokens = try Tokenizer.tokenize(from)
        var oldByKey: [String: Token] = [:]
        for t in oldTokens { oldByKey[t.key] = t }
        let newKeys = Set(newTokens.map { $0.key })

        var result: [DiffElement] = []
        for token in newTokens {
            if let old = oldByKey[token.key] {
                if old.text == token.text {
                    result.append(DiffElement(key: token.key, kind: token.kind, from: old.text, to: token.text, action: .stay))
                } else if token.kind.isDigit && old.kind.isDigit {
                    let d = matchedDigitDirection(trend, direction, old.text, token.text)
                    result.append(DiffElement(key: token.key, kind: token.kind, from: old.text, to: token.text, action: rollAction(d)))
                } else {
                    result.append(DiffElement(key: token.key, kind: token.kind, from: old.text, to: token.text, action: .crossfade))
                }
            } else {
                result.append(DiffElement(key: token.key, kind: token.kind, from: nil, to: token.text,
                                          action: enterAction(token.kind, elementDirection(trend, direction))))
            }
        }

        // Exiting elements go right after their predecessor in the previous list (or first).
        var previousKey: String? = nil
        for old in oldTokens {
            if !newKeys.contains(old.key) {
                let element = DiffElement(key: old.key, kind: old.kind, from: old.text, to: nil,
                                          action: exitAction(old.kind, elementDirection(trend, direction)))
                let anchor = previousKey.flatMap { key in result.firstIndex { $0.key == key } } ?? -1
                result.insert(element, at: anchor + 1)
            }
            previousKey = old.key
        }
        return DiffResult(direction: direction, elements: result)
    }

    private static func elementDirection(_ trend: Trend, _ direction: Direction) -> Direction {
        if direction == .none { return .none }
        switch trend {
        case .alwaysUp: return .up
        case .alwaysDown: return .down
        default: return direction
        }
    }

    private static func matchedDigitDirection(_ trend: Trend, _ direction: Direction, _ old: String, _ new: String) -> Direction {
        if direction == .none { return .none }
        if trend == .perDigit { return new > old ? .up : .down }
        return elementDirection(trend, direction)
    }

    private static func rollAction(_ d: Direction) -> DiffAction {
        switch d {
        case .up: return .rollUp
        case .down: return .rollDown
        case .none: return .crossfade
        }
    }

    private static func enterAction(_ kind: TokenKind, _ d: Direction) -> DiffAction {
        if !kind.isDigit || d == .none { return .enterFade }
        return d == .up ? .enterUp : .enterDown
    }

    private static func exitAction(_ kind: TokenKind, _ d: Direction) -> DiffAction {
        if !kind.isDigit || d == .none { return .exitFade }
        return d == .up ? .exitUp : .exitDown
    }
}
