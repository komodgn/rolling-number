import RollingNumber
import SwiftUI

struct CheckoutView: View {
    private let unitPrice = Decimal(9_900)
    @State private var quantity = 1

    private var total: Decimal { unitPrice * Decimal(quantity) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("결제").font(.title2)

            HStack {
                VStack(alignment: .leading) {
                    Text("기본 티셔츠").font(.headline)
                    RollingNumber(unitPrice, format: .currency(code: "KRW")).font(.subheadline)
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
                RollingNumber(total, format: .currency(code: "KRW"))
                    .font(.system(size: 28, weight: .bold))
            }
            Spacer()
        }
        .padding(24)
    }
}
