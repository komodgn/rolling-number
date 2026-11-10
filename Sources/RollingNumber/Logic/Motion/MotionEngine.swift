import Foundation

struct RenderLayer: Equatable {
    var text: String
    var offsetY: Double
    var alpha: Double
}

struct RenderItem: Equatable {
    var key: String
    var kind: TokenKind
    var x: Double
    var width: Double
    var layers: [RenderLayer]
}

struct RenderFrame: Equatable {
    var totalWidth: Double
    var items: [RenderItem]
    var isAnimating: Bool

    static let empty = RenderFrame(totalWidth: 0, items: [], isAnimating: false)
}

/// Animates display elements toward the latest diff (research R5, data-model §8).
/// Mirrors Android `MotionEngine`. Time advances in fixed 4 ms steps.
final class MotionEngine {
    var spec: MotionSpec
    private(set) var elements: [ElementState] = []
    private var lastNanos: Int64?
    private var accumulatedNanos: Int64 = 0

    init(spec: MotionSpec = MotionSpec()) { self.spec = spec }

    var isAnimating: Bool { elements.contains { !$0.isSettled } }

    /// Replaces everything with resting `tokens`; no animation.
    func showImmediately(_ tokens: [Token]) {
        elements = tokens.map { t in
            let e = ElementState(key: t.key, kind: t.kind, spec: spec, presence: 1)
            e.layers.append(Layer(text: t.text, spec: spec, offset: 0, alpha: 1, weight: 1))
            return e
        }
        resetClock()
    }

    /// Applies a diff, continuing from the current visual state when already animating (FR-011).
    func apply(_ diff: DiffResult) {
        if diff.isEmpty { return }
        if !isAnimating { resetClock() }
        var byKey: [String: ElementState] = [:]
        for e in elements { byKey[e.key] = e }
        var ordered: [ElementState] = []
        var mentioned = Set<String>()

        for d in diff.elements {
            mentioned.insert(d.key)
            let isEnter = d.action == .enterUp || d.action == .enterDown || d.action == .enterFade
            let element = byKey[d.key] ?? ElementState(key: d.key, kind: d.kind, spec: spec, presence: isEnter ? 0 : 1)
            element.kind = d.kind
            switch d.action {
            case .stay, .crossfade, .enterFade:
                settle(element, on: d.to!, entryOffset: 0, exitOffset: nil)
            case .rollUp, .enterUp:
                settle(element, on: d.to!, entryOffset: 1, exitOffset: -1)
            case .rollDown, .enterDown:
                settle(element, on: d.to!, entryOffset: -1, exitOffset: 1)
            case .exitUp: element.exit(spec, exitOffset: -1)
            case .exitDown: element.exit(spec, exitOffset: 1)
            case .exitFade: element.exit(spec, exitOffset: nil)
            }
            ordered.append(element)
        }

        // Elements still leaving from an earlier transition keep their place after their old predecessor.
        var previous: ElementState? = nil
        for e in elements {
            if !mentioned.contains(e.key) {
                if !e.exiting { e.exit(spec, exitOffset: nil) }
                let anchor = previous.flatMap { p in ordered.firstIndex { $0 === p } } ?? -1
                ordered.insert(e, at: anchor + 1)
            }
            previous = e
        }
        elements = ordered
    }

    /// Makes `text` the resting layer. Reuses an existing layer with the same text (no duplicates,
    /// FR-011); otherwise adds one at `entryOffset`. Other layers leave toward `exitOffset`.
    private func settle(_ element: ElementState, on text: String, entryOffset: Double, exitOffset: Double?) {
        element.show(spec)
        let existing = element.layers.last { $0.text == text }
        for layer in element.layers where layer !== existing {
            if !layer.leaving || exitOffset != nil { layer.leave(spec, exitOffset: exitOffset) }
        }
        if let existing = existing {
            existing.rest(spec)
            element.layers.removeAll { $0 === existing }
            element.layers.append(existing) // resting layer drawn on top
        } else {
            let startWeight: Double = element.layers.isEmpty ? 1 : 0
            let layer = Layer(text: text, spec: spec, offset: entryOffset, alpha: 0, weight: startWeight)
            layer.rest(spec)
            element.layers.append(layer)
        }
    }

    /// Advances to `nowNanos`. Returns true while animating.
    @discardableResult
    func step(_ nowNanos: Int64) -> Bool {
        let last = lastNanos
        lastNanos = nowNanos
        guard let l = last, isAnimating else { return isAnimating }
        accumulatedNanos += max(nowNanos - l, 0)
        while accumulatedNanos >= stepNanos && isAnimating {
            accumulatedNanos -= stepNanos
            for e in elements { e.step(stepSeconds) }
            elements.removeAll { $0.prune() }
        }
        if !isAnimating { accumulatedNanos = 0 }
        return isAnimating
    }

    /// Jumps every element to its final state.
    func snapToEnd() {
        elements.removeAll { $0.exiting }
        elements.forEach { $0.snapToRest() }
        resetClock()
    }

    private func resetClock() {
        lastNanos = nil
        accumulatedNanos = 0
    }

    /// Lays elements out left to right. Element width = presence × weight-blended text width.
    func frame(measure: (String) -> Double, tabularDigitWidth: Double?) -> RenderFrame {
        var x = 0.0
        var items: [RenderItem] = []
        for e in elements {
            var weighted = 0.0, totalWeight = 0.0, widest = 0.0
            for layer in e.layers {
                let w = (e.kind.isDigit ? tabularDigitWidth : nil) ?? measure(layer.text)
                let weight = min(max(layer.weight.value, 0), 1)
                weighted += w * weight
                totalWeight += weight
                widest = max(widest, w)
            }
            let textWidth = totalWeight > 1e-6 ? weighted / totalWeight : widest
            let width = textWidth * min(max(e.presence.value, 0), 1)
            items.append(RenderItem(
                key: e.key, kind: e.kind, x: x, width: width,
                layers: e.layers.map { RenderLayer(text: $0.text, offsetY: $0.offset.value, alpha: min(max($0.alpha.value, 0), 1)) }
            ))
            x += width
        }
        return RenderFrame(totalWidth: x, items: items, isAnimating: isAnimating)
    }
}
