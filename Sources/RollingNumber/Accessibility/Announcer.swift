import UIKit

/// Speaks a message through VoiceOver without cutting off what is being read (research R7).
enum Announcer {
    static func announce(_ text: String) {
        guard UIAccessibility.isVoiceOverRunning else { return }
        let message = NSAttributedString(string: text, attributes: [.accessibilitySpeechQueueAnnouncement: true])
        UIAccessibility.post(notification: .announcement, argument: message)
    }
}
