import RollingNumber
import SwiftUI

/// Settings shared by the demo screens.
final class DemoSettings: ObservableObject {
    static let locales = ["ko-KR", "en-US", "de-DE", "ja-JP"]

    @Published var localeID = locales[0]
    @Published var animationEnabled = true
    @Published var ignoreReduceMotion = false

    var locale: Locale { Locale(identifier: localeID) }

    var motion: RollingMotion {
        RollingMotion(enabled: animationEnabled, respectReduceMotion: !ignoreReduceMotion)
    }
}

@main
struct CouponCheckoutApp: App {
    @StateObject private var settings = DemoSettings()

    var body: some Scene {
        WindowGroup {
            TabView {
                CheckoutView().tabItem { Label("Checkout", systemImage: "cart") }
                OrderHistoryView().tabItem { Label("Order History", systemImage: "list.bullet") }
            }
            .environmentObject(settings)
            .rollingMotion(settings.motion)
        }
    }
}
