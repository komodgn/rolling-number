import RollingNumber
import SwiftUI

/// Locales offered in the demo; the currency stays KRW so only the presentation changes (FR-014).
private let demoLocales = ["ko-KR", "en-US", "de-DE", "ja-JP"]

struct CheckoutView: View {
    private let unitPrice = Decimal(9_900)
    @State private var quantity = 1
    @State private var localeID = demoLocales[0]

    private var total: Decimal { unitPrice * Decimal(quantity) }
    private var locale: Locale { Locale(identifier: localeID) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("결제").font(.title2)

            Picker("Locale", selection: $localeID) {
                ForEach(demoLocales, id: \.self) { Text($0).tag($0) }
            }
            .pickerStyle(.segmented)

            HStack {
                VStack(alignment: .leading) {
                    Text("기본 티셔츠").font(.headline)
                    RollingNumber(unitPrice, format: .currency(code: "KRW"), locale: locale).font(.subheadline)
                }
                Spacer()
                Button("−") { if quantity > 1 { quantity -= 1 } }.buttonStyle(.bordered)
                RollingNumber(quantity).font(.headline)
                Button("+") { if quantity < 99 { quantity += 1 } }.buttonStyle(.bordered)
            }

            Divider()

            HStack {
                Text("최종 결제 금액").font(.headline)
                Spacer()
                RollingNumber(total, format: .currency(code: "KRW"), locale: locale)
                    .font(.system(size: 28, weight: .bold))
            }
            Spacer()
        }
        .padding(24)
    }
}
