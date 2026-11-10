import SwiftUI

/// Shows a value formatted with `format` and rolls changed digits when the value changes.
///
/// ```swift
/// RollingNumber(total, format: .currency(code: "KRW"))
/// ```
///
/// Font, size, weight and color follow the standard SwiftUI modifiers (`.font`, `.foregroundColor`).
public struct RollingNumber: View {
    private let value: Decimal?
    private let format: RollingNumberFormat
    private let locale: Locale?
    private let prefix: String
    private let suffix: String

    @Environment(\.locale) private var environmentLocale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.rollingOptions) private var options
    @Environment(\.rollingGroup) private var group
    @StateObject private var model = RollingModel()

    /// - Parameters:
    ///   - value: the exact value to show.
    ///   - format: how the value becomes text.
    ///   - locale: nil follows the environment (device) locale (FR-003).
    ///   - prefix: text shown before the number, never rolled (FR-004).
    ///   - suffix: text shown after the number, never rolled (FR-004).
    public init(_ value: Decimal, format: RollingNumberFormat = .number(), locale: Locale? = nil, prefix: String = "", suffix: String = "") {
        self.value = value.isFinite ? value : nil
        self.format = format
        self.locale = locale
        self.prefix = prefix
        self.suffix = suffix
    }

    /// `Int` convenience initializer.
    public init(_ value: Int, format: RollingNumberFormat = .number(), locale: Locale? = nil, prefix: String = "", suffix: String = "") {
        self.init(Decimal(value), format: format, locale: locale, prefix: prefix, suffix: suffix)
    }

    /// `Double` convenience initializer (shortest decimal form). NaN and infinity are ignored and
    /// the last valid value stays.
    public init(_ value: Double, format: RollingNumberFormat = .number(), locale: Locale? = nil, prefix: String = "", suffix: String = "") {
        let decimal = value.isFinite ? Decimal(string: "\(value)", locale: Locale(identifier: "en_US_POSIX")) : nil
        self.value = decimal
        self.format = format
        self.locale = locale
        self.prefix = prefix
        self.suffix = suffix
        #if DEBUG
        if decimal == nil { print("RollingNumber: ignoring non-finite value \(value)") }
        #endif
    }

    private var formatted: FormattedNumber? {
        guard let value = value else { return nil }
        guard var f = FoundationFormatter.format(value, format: format, locale: locale ?? environmentLocale) else { return nil }
        f.prefix = prefix
        f.suffix = suffix
        return f
    }

    public var body: some View {
        let current = formatted
        let shown = current?.displayText ?? model.session.current?.displayText ?? ""
        RollingCanvas(model: model, tabularDigits: options.tabularDigits, fadeMaskHeight: options.fadeMaskHeight)
            // One accessibility element: label + final value only (FR-018).
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: options.accessibilityLabel ?? shown))
            .accessibilityValue(Text(verbatim: options.accessibilityLabel == nil ? "" : shown))
            .onAppear { apply(current) }
            .onDisappear { coordinator.unregister(model.session) }
            .onChange(of: current) { apply($0) }
            .onChange(of: reduceMotion) { _ in applyAnimationAllowed() }
            .onChange(of: options.motion) { _ in applyAnimationAllowed() }
    }

    private var coordinator: AnnouncementCoordinator { group?.coordinator ?? model.standaloneCoordinator }

    private func applyAnimationAllowed() {
        let m = options.motion
        model.session.setAnimationAllowed(m.enabled && !(reduceMotion && m.respectReduceMotion))
        model.syncAnimating()
    }

    private func apply(_ number: FormattedNumber?) {
        let session = model.session
        session.spec = options.motion.spec
        session.trend = options.trend.internalTrend
        session.label = options.accessibilityLabel
        session.announce = options.announcements
        model.onStart = options.onStart
        model.onEnd = options.onEnd
        if session.coordinator !== coordinator {
            session.coordinator?.unregister(session)
            coordinator.register(session, order: options.groupOrder)
        }
        coordinator.setOrder(session, options.groupOrder)
        applyAnimationAllowed()
        session.setItemKey(options.itemKey)
        if let number = number { model.update(number) }
    }
}
