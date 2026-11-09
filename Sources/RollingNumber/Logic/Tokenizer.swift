import Foundation

/// Splits a `FormattedNumber` into display pieces with decimal-point-relative keys (data-model §5).
/// Same rules as Android `Tokenizer.kt`; parity is checked by `fixtures/format` and `fixtures/diff`.
enum Tokenizer {
    enum TokenizerError: Error { case noDigit(String) }

    static func tokenize(_ number: FormattedNumber) throws -> [Token] {
        let chars = Array(number.text)
        guard let first = chars.firstIndex(where: isAsciiDigit),
              let last = chars.lastIndex(where: isAsciiDigit) else {
            throw TokenizerError.noDigit(number.text)
        }
        var tokens: [Token] = []
        if !number.prefix.isEmpty { tokens.append(Token(kind: .prefix, text: number.prefix, key: "AP")) }
        tokens += outside(Array(chars[0..<first]), number.symbols, leading: true)
        tokens += core(Array(chars[first...last]), number.symbols)
        tokens += outside(Array(chars[(last + 1)...]), number.symbols, leading: false)
        if !number.suffix.isEmpty { tokens.append(Token(kind: .suffix, text: number.suffix, key: "AS")) }
        return tokens
    }

    /// Numeric parts only, with canonical symbols: digits, "." decimal, "," group, "-" sign.
    static func normalize(_ tokens: [Token]) -> String {
        var s = ""
        for t in tokens {
            switch t.kind {
            case .intDigit, .fracDigit: s += t.text
            case .decimal: s += "."
            case .group: s += ","
            case .sign: s += "-"
            default: break
            }
        }
        return s
    }

    private static func isAsciiDigit(_ c: Character) -> Bool { ("0"..."9").contains(c) }

    private static func starts(_ chars: [Character], at i: Int, with s: String) -> Bool {
        let needle = Array(s)
        guard !needle.isEmpty, i + needle.count <= chars.count else { return false }
        return Array(chars[i..<(i + needle.count)]) == needle
    }

    private static func core(_ area: [Character], _ symbols: NumberSymbols) -> [Token] {
        var pieces: [(kind: TokenKind, text: String)] = []
        var i = 0
        var decimalSeen = false
        while i < area.count {
            let c = area[i]
            if isAsciiDigit(c) {
                pieces.append((.intDigit, String(c))); i += 1
            } else if !decimalSeen && starts(area, at: i, with: symbols.decimalSeparator) {
                pieces.append((.decimal, symbols.decimalSeparator))
                decimalSeen = true
                i += symbols.decimalSeparator.count
            } else if starts(area, at: i, with: symbols.groupingSeparator) {
                pieces.append((.group, symbols.groupingSeparator))
                i += symbols.groupingSeparator.count
            } else {
                pieces.append((.literal, String(c))); i += 1
            }
        }

        let decimalIndex = pieces.firstIndex { $0.kind == .decimal }
        let integerEnd = decimalIndex ?? pieces.count
        var result = [Token?](repeating: nil, count: pieces.count)

        var digitsToRight = 0
        for p in stride(from: integerEnd - 1, through: 0, by: -1) {
            let piece = pieces[p]
            switch piece.kind {
            case .intDigit:
                result[p] = Token(kind: .intDigit, text: piece.text, key: "I\(digitsToRight)")
                digitsToRight += 1
            case .group:
                result[p] = Token(kind: .group, text: piece.text, key: "G\(digitsToRight)")
            default:
                result[p] = Token(kind: .literal, text: piece.text, key: "Li\(digitsToRight)")
            }
        }
        if let d = decimalIndex {
            result[d] = Token(kind: .decimal, text: pieces[d].text, key: "D")
            var fraction = 0
            for p in (d + 1)..<pieces.count {
                let piece = pieces[p]
                switch piece.kind {
                case .intDigit:
                    result[p] = Token(kind: .fracDigit, text: piece.text, key: "F\(fraction)")
                    fraction += 1
                case .group:
                    result[p] = Token(kind: .group, text: piece.text, key: "H\(fraction)")
                default:
                    result[p] = Token(kind: .literal, text: piece.text, key: "Lf\(fraction)")
                }
            }
        }
        return result.map { $0! }
    }

    private static func outside(_ region: [Character], _ symbols: NumberSymbols, leading: Bool) -> [Token] {
        if region.isEmpty { return [] }
        var parts: [(isSign: Bool, text: String)] = []
        var run = ""
        var i = 0
        while i < region.count {
            let sign = symbols.minusSigns
                .filter { !$0.isEmpty && starts(region, at: i, with: $0) }
                .max { $0.count < $1.count }
            if let sign = sign {
                if !run.isEmpty { parts.append((false, run)); run = "" }
                parts.append((true, sign))
                i += sign.count
            } else {
                run.append(region[i]); i += 1
            }
        }
        if !run.isEmpty { parts.append((false, run)) }

        let literalCount = parts.filter { !$0.isSign }.count
        var literalSeen = 0
        var signSeen = 0
        return parts.map { part in
            if part.isSign {
                let key = signSeen == 0 ? "S" : "S\(signSeen)"
                signSeen += 1
                return Token(kind: .sign, text: part.text, key: key)
            }
            let index = leading ? literalCount - 1 - literalSeen : literalSeen
            literalSeen += 1
            return Token(kind: .literal, text: part.text, key: leading ? "Lp\(index)" : "Ls\(index)")
        }
    }
}
