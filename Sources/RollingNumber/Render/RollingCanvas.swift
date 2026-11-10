import SwiftUI

/// Owns the session for one `RollingNumber`. Publishes only when animation starts or stops,
/// never per frame; frames are pulled by `TimelineView`.
final class RollingModel: ObservableObject {
    let session = RollingSession()
    @Published private(set) var isAnimating = false
    @Published var metrics = GlyphMetrics()
    lazy var standaloneCoordinator = AnnouncementCoordinator { text in Announcer.announce(text) }
    var onStart: (() -> Void)?
    var onEnd: (() -> Void)?

    init() {
        session.onStart = { [weak self] in
            self?.isAnimating = true
            self?.onStart?()
        }
        session.onEnd = { [weak self] in
            self?.isAnimating = false
            self?.onEnd?()
        }
    }

    func update(_ number: FormattedNumber) {
        session.update(number)
        syncAnimating()
        // A format/locale change (or an animation-off replacement) swaps the content immediately
        // without starting an animation, so `isAnimating` stays false and the `TimelineView` in
        // `RollingCanvas` stays paused. Nothing would repaint until the next external re-render,
        // which shows the change one interaction late. Nudge observers to redraw the new frame once.
        if !isAnimating { objectWillChange.send() }
    }

    func syncAnimating() {
        if session.isAnimating != isAnimating { isAnimating = session.isAnimating }
    }

    func step(_ date: Date) {
        session.step(Int64(date.timeIntervalSinceReferenceDate * 1_000_000_000))
    }

    func frame(tabular: Bool) -> RenderFrame {
        let m = metrics
        return session.frame(measure: { m.width($0) }, tabularDigitWidth: tabular ? m.tabularDigitWidth : nil)
    }

    /// Strings that need measuring: all digits plus every visible layer text.
    func neededStrings(_ frame: RenderFrame) -> [String] {
        var set = Set(GlyphMetrics.digits)
        for item in frame.items { for layer in item.layers { set.insert(layer.text) } }
        if let current = session.current, let tokens = try? Tokenizer.tokenize(current) {
            tokens.forEach { set.insert($0.text) }
        }
        return set.sorted()
    }
}

/// Draws the current frame of a session with `Canvas`, re-evaluated every display frame by
/// `TimelineView` only while animating.
struct RollingCanvas: View {
    @ObservedObject var model: RollingModel
    let tabularDigits: Bool
    let fadeMaskHeight: CGFloat

    var body: some View {
        // Only mount `TimelineView(.animation)` while actually animating. A paused TimelineView still
        // runs the animation scheduler and stays in the tree, which swallows the first tap of nearby
        // controls (two-tap toggles) and keeps a resting canvas from repainting on a plain re-render.
        // At rest we draw a plain Canvas that updates through normal SwiftUI invalidation.
        Group {
            if model.isAnimating {
                TimelineView(.animation) { timeline in
                    canvas(stepTo: timeline.date)
                }
            } else {
                canvas(stepTo: nil)
            }
        }
        .onPreferenceChange(GlyphSizesKey.self) { sizes in
            if sizes != model.metrics.sizes { model.metrics.sizes = sizes }
        }
    }

    @ViewBuilder
    private func canvas(stepTo date: Date?) -> some View {
        let _ = date.map { model.step($0) }
        let frame = model.frame(tabular: tabularDigits)
        let lineHeight = model.metrics.lineHeight
        Canvas { context, _ in
            for item in frame.items {
                for layer in item.layers where layer.alpha > 0 {
                    var ctx = context
                    ctx.opacity = layer.alpha
                    let text = ctx.resolve(Text(verbatim: layer.text))
                    ctx.draw(
                        text,
                        at: CGPoint(x: item.x + item.width / 2, y: lineHeight / 2 + layer.offsetY * lineHeight),
                        anchor: .center
                    )
                }
            }
        }
        .frame(width: CGFloat(ceil(frame.totalWidth)), height: CGFloat(lineHeight))
        .clipped()
        .mask(FadeMask(height: fadeMaskHeight))
        .background(GlyphMeasurer(strings: model.neededStrings(frame)))
    }
}

/// Top/bottom fade on rolling digits (FR-022). Height 0 disables it.
struct FadeMask: View {
    let height: CGFloat

    var body: some View {
        GeometryReader { g in
            if height <= 0 || g.size.height <= 0 {
                Color.black
            } else {
                let edge = min(height / g.size.height, 0.5)
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: .black, location: edge),
                        .init(color: .black, location: 1 - edge),
                        .init(color: .clear, location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
    }
}
