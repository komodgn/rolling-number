import Foundation

// Tolerances from research R5 (same values as Android ElementState.kt).
let offsetTolerance = 0.001
let offsetVelocityTolerance = 0.01
let alphaTolerance = 0.005
let alphaVelocityTolerance = 0.05
let presenceTolerance = 0.001
let presenceVelocityTolerance = 0.05

/// One glyph layer inside an element. Mirrors Android `Layer`.
/// - offset: vertical position in line heights (0 resting, +1 one line below, -1 one line above)
/// - alpha: 0...1
/// - weight: 0...1 share of this layer's text width in the element width (layout curve)
final class Layer {
    let text: String
    let offset: AnimatedValue
    let alpha: AnimatedValue
    let weight: AnimatedValue
    private(set) var leaving = false

    init(text: String, spec: MotionSpec, offset: Double, alpha: Double, weight: Double) {
        self.text = text
        self.offset = AnimatedValue(offset, curve: spec.roll, positionTolerance: offsetTolerance, velocityTolerance: offsetVelocityTolerance)
        self.alpha = AnimatedValue(alpha, curve: spec.fade, positionTolerance: alphaTolerance, velocityTolerance: alphaVelocityTolerance)
        self.weight = AnimatedValue(weight, curve: spec.layout, positionTolerance: presenceTolerance, velocityTolerance: presenceVelocityTolerance)
    }

    var isSettled: Bool { offset.isSettled && alpha.isSettled && weight.isSettled }

    func rest(_ spec: MotionSpec) {
        leaving = false
        offset.animate(to: 0, curve: spec.roll)
        alpha.animate(to: 1, curve: spec.fade)
        weight.animate(to: 1, curve: spec.layout)
    }

    /// Leave toward `exitOffset`; nil keeps the current vertical target (fade in place).
    func leave(_ spec: MotionSpec, exitOffset: Double?) {
        leaving = true
        if let e = exitOffset { offset.animate(to: e, curve: spec.roll) }
        alpha.animate(to: 0, curve: spec.fade)
        weight.animate(to: 0, curve: spec.layout)
    }

    func snapToRest() {
        leaving = false
        offset.snap(to: 0)
        alpha.snap(to: 1)
        weight.snap(to: 1)
    }

    func step(_ dt: Double) {
        offset.step(dt)
        alpha.step(dt)
        weight.step(dt)
    }
}

/// One on-screen element; at most one per key (data-model §8). Mirrors Android `ElementState`.
final class ElementState {
    let key: String
    var kind: TokenKind
    let presence: AnimatedValue
    var layers: [Layer] = []
    private(set) var exiting = false

    init(key: String, kind: TokenKind, spec: MotionSpec, presence: Double) {
        self.key = key
        self.kind = kind
        self.presence = AnimatedValue(presence, curve: spec.layout, positionTolerance: presenceTolerance, velocityTolerance: presenceVelocityTolerance)
    }

    var isSettled: Bool { presence.isSettled && layers.allSatisfy { $0.isSettled } }

    func show(_ spec: MotionSpec) {
        exiting = false
        presence.animate(to: 1, curve: spec.layout)
    }

    func exit(_ spec: MotionSpec, exitOffset: Double?) {
        exiting = true
        presence.animate(to: 0, curve: spec.layout)
        layers.forEach { $0.leave(spec, exitOffset: exitOffset) }
    }

    func snapToRest() {
        exiting = false
        presence.snap(to: 1)
        layers.removeAll { $0.leaving }
        layers.forEach { $0.snapToRest() }
    }

    func step(_ dt: Double) {
        presence.step(dt)
        layers.forEach { $0.step(dt) }
    }

    /// Drops finished layers. Returns true when the whole element has finished exiting.
    func prune() -> Bool {
        if exiting { return isSettled }
        if layers.contains(where: { !$0.leaving }) { layers.removeAll { $0.leaving && $0.isSettled } }
        return false
    }
}
