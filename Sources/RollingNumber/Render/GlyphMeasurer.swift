import SwiftUI

/// Sizes of rendered strings in the current environment font, reported via preference.
struct GlyphSizesKey: PreferenceKey {
    static var defaultValue: [String: CGSize] = [:]
    static func reduce(value: inout [String: CGSize], nextValue: () -> [String: CGSize]) {
        value.merge(nextValue()) { _, new in new }
    }
}

/// Invisible view that lays out each string with the environment font and reports its size.
/// SwiftUI `Font` cannot be converted to a measurable `UIFont`, so measuring through real `Text`
/// keeps `.font(...)` and Dynamic Type working.
struct GlyphMeasurer: View {
    let strings: [String]

    var body: some View {
        ZStack {
            ForEach(strings, id: \.self) { s in
                Text(verbatim: s)
                    .fixedSize()
                    .background(GeometryReader { g in
                        Color.clear.preference(key: GlyphSizesKey.self, value: [s: g.size])
                    })
            }
        }
        .hidden()
        .accessibilityHidden(true)
    }
}

/// Cached sizes plus the derived metrics used for layout.
struct GlyphMetrics: Equatable {
    var sizes: [String: CGSize] = [:]

    static let digits = (0...9).map(String.init)

    var tabularDigitWidth: Double {
        Double(Self.digits.compactMap { sizes[$0]?.width }.max() ?? 0)
    }

    var lineHeight: Double { Double(sizes["0"]?.height ?? 0) }

    /// Measured width, or a digit-width estimate until the string has been measured.
    func width(_ s: String) -> Double {
        if let w = sizes[s]?.width { return Double(w) }
        return tabularDigitWidth * Double(s.count)
    }
}
