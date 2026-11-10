import RollingNumber
import SwiftUI

struct CheckoutView: View {
    @EnvironmentObject private var settings: DemoSettings

    private let unitPrice = Decimal(9_900)
    private let freeShippingFrom = Decimal(30_000)
    private let shippingFee = Decimal(3_000)

    @State private var quantity = 1
    @State private var couponApplied = false
    @State private var debugAdjustment = Decimal(0)

    // The app computes prices; the component only displays them.
    private var subtotal: Decimal { unitPrice * Decimal(quantity) }
    private var discount: Decimal {
        guard couponApplied else { return 0 }
        var raw = subtotal * Decimal(string: "0.10")!
        var rounded = Decimal()
        NSDecimalRound(&rounded, &raw, 0, .down)
        return rounded
    }
    private var shipping: Decimal { subtotal - discount >= freeShippingFrom ? 0 : shippingFee }
    private var total: Decimal { subtotal - discount + shipping + debugAdjustment }

    private let krw = RollingNumberFormat.currency(code: "KRW")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Checkout").font(.title2)

                Picker("Locale", selection: $settings.localeID) {
                    ForEach(DemoSettings.locales, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.segmented)

                HStack {
                    VStack(alignment: .leading) {
                        Text("Basic T-Shirt").font(.headline)
                        RollingNumber(unitPrice, format: krw, locale: settings.locale).font(.subheadline)
                    }
                    Spacer()
                    Button("−") { if quantity > 1 { quantity -= 1 } }.buttonStyle(.bordered)
                    RollingNumber(quantity).font(.headline).rollingAccessibilityLabel("Quantity")
                    Button("+") { if quantity < 99 { quantity += 1 } }.buttonStyle(.bordered)
                }

                Button(couponApplied ? "Remove 10% coupon" : "Apply 10% coupon") { couponApplied.toggle() }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)

                Divider()

                // US4: one group. Members changed together start together, neighbors in the HStack move
                // with the width change, and VoiceOver hears one merged announcement.
                RollingNumberGroup {
                    VStack(spacing: 8) {
                        row("Subtotal") {
                            RollingNumber(subtotal, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("Subtotal").rollingGroupOrder(0)
                        }
                        row("Discount") {
                            RollingNumber(-discount, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("Discount").rollingGroupOrder(1)
                        }
                        row("Shipping") {
                            RollingNumber(shipping, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("Shipping").rollingGroupOrder(2)
                        }
                        Divider()
                        HStack {
                            Text("Total").font(.headline)
                            Spacer()
                            RollingNumber(total, format: krw, locale: settings.locale)
                                .font(.system(size: 28, weight: .bold))
                                .rollingAccessibilityLabel("Total").rollingGroupOrder(3)
                        }
                        HStack(spacing: 0) {
                            RollingNumber(subtotal, format: krw, locale: settings.locale)
                            Text(" − ")
                            RollingNumber(discount, format: krw, locale: settings.locale)
                            Text(" + ")
                            RollingNumber(shipping, format: krw, locale: settings.locale)
                            Text(" = ")
                            RollingNumber(total, format: krw, locale: settings.locale)
                            Spacer()
                        }
                        .font(.footnote)
                        .rollingAnnouncements(false)
                    }
                }

                Divider()

                Toggle("Animation", isOn: $settings.animationEnabled)
                Toggle("Ignore Reduce Motion", isOn: $settings.ignoreReduceMotion)
                Button(debugAdjustment == 0 ? "Debug: make total negative" : "Debug: reset") {
                    debugAdjustment = debugAdjustment == 0 ? -50_000 : 0
                }
                .buttonStyle(.bordered)
            }
            .padding(24)
        }
    }

    private func row<Amount: View>(_ label: String, @ViewBuilder amount: () -> Amount) -> some View {
        HStack {
            Text(label)
            Spacer()
            amount()
        }
    }
}
