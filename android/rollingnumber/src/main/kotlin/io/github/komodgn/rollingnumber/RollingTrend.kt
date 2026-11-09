package io.github.komodgn.rollingnumber

import io.github.komodgn.rollingnumber.logic.Trend

/** Which way changed digits roll (FR-005). */
public enum class RollingTrend {
    /** Up when the value increases, down when it decreases. */
    Auto,

    /** Always roll up. */
    AlwaysUp,

    /** Always roll down. */
    AlwaysDown,

    /** Each digit rolls up or down by its own change; entering/leaving digits follow the value. */
    PerDigit,
    ;

    internal fun toInternal(): Trend = when (this) {
        Auto -> Trend.Auto
        AlwaysUp -> Trend.AlwaysUp
        AlwaysDown -> Trend.AlwaysDown
        PerDigit -> Trend.PerDigit
    }
}
