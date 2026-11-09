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
                Text("결제").font(.title2)

                Picker("Locale", selection: $settings.localeID) {
                    ForEach(DemoSettings.locales, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.segmented)

                HStack {
                    VStack(alignment: .leading) {
                        Text("기본 티셔츠").font(.headline)
                        RollingNumber(unitPrice, format: krw, locale: settings.locale).font(.subheadline)
                    }
                    Spacer()
                    Button("−") { if quantity > 1 { quantity -= 1 } }.buttonStyle(.bordered)
                    RollingNumber(quantity).font(.headline).rollingAccessibilityLabel("수량")
                    Button("+") { if quantity < 99 { quantity += 1 } }.buttonStyle(.bordered)
                }

                Button(couponApplied ? "10% 쿠폰 해제" : "10% 쿠폰 적용") { couponApplied.toggle() }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)

                Divider()

                // US4: one group. Members changed together start together, neighbors in the HStack move
                // with the width change, and VoiceOver hears one merged announcement.
                RollingNumberGroup {
                    VStack(spacing: 8) {
                        row("상품 금액") {
                            RollingNumber(subtotal, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("상품 금액").rollingGroupOrder(0)
                        }
                        row("쿠폰 할인") {
                            RollingNumber(-discount, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("할인액").rollingGroupOrder(1)
                        }
                        row("배송비") {
                            RollingNumber(shipping, format: krw, locale: settings.locale)
                                .rollingAccessibilityLabel("배송비").rollingGroupOrder(2)
                        }
                        Divider()
                        HStack {
                            Text("최종 결제 금액").font(.headline)
                            Spacer()
                            RollingNumber(total, format: krw, locale: settings.locale)
                                .font(.system(size: 28, weight: .bold))
                                .rollingAccessibilityLabel("최종가").rollingGroupOrder(3)
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

                Toggle("애니메이션", isOn: $settings.animationEnabled)
                Toggle("동작 줄이기 설정 무시", isOn: $settings.ignoreReduceMotion)
                Button(debugAdjustment == 0 ? "디버그: 금액을 음수로" : "디버그: 음수 해제") {
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
