import RollingNumber
import SwiftUI

private struct Order: Identifiable {
    let id: Int
    var amount: Decimal
}

/// SC-007: 100 rows whose amounts change every second while scrolling. Each row's RollingNumber
/// has `.rollingItemKey(order.id)`, so a reused row never animates from another order's value.
struct OrderHistoryView: View {
    @EnvironmentObject private var settings: DemoSettings
    @State private var orders = (1...100).map { Order(id: $0, amount: Decimal(Int.random(in: 5...300) * 100)) }
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        List(orders) { order in
            HStack {
                Text("주문 #\(order.id)")
                Spacer()
                RollingNumber(order.amount, format: .currency(code: "KRW"), locale: settings.locale)
                    .rollingItemKey(order.id)
                    .rollingAnnouncements(false)
            }
        }
        .onReceive(timer) { _ in
            for _ in 0..<10 {
                let i = Int.random(in: 0..<orders.count)
                orders[i].amount = Decimal(Int.random(in: 5...300) * 100)
            }
        }
    }
}
