package io.github.komodgn.rollingnumber.fixtures

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Kotlin mirror of specs/001-rolling-number-display/contracts/fixtures.md.
 * Unknown keys fail decoding so typos in fixtures are caught.
 */
val fixtureJson: Json = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
}

@Serializable
data class FormatFile(val schema: Int, val kind: String, val cases: List<FormatCase>)

@Serializable
data class FormatCase(
    val id: String,
    val spec: String = "",
    val input: FormatInput,
    val expect: FormatExpect,
    val platforms: List<String>? = null,
    val reason: String? = null,
)

@Serializable
data class FormatInput(
    val value: String,
    val locale: String,
    val format: FormatSpecJson,
    val prefix: String = "",
    val suffix: String = "",
)

@Serializable
data class FormatSpecJson(
    val style: String,
    val currency: String? = null,
    val minFraction: Int? = null,
    val maxFraction: Int? = null,
    val grouping: Boolean = true,
    val rounding: String = "halfUp",
)

@Serializable
data class FormatExpect(val normalized: String, val tokens: List<TokenJson>)

@Serializable
data class TokenJson(
    val kind: String,
    val key: String,
    val text: String,
    /** "loose" (default for literals), "exact", or "any". */
    val textMatch: String? = null,
)

@Serializable
data class DiffFile(val schema: Int, val kind: String, val cases: List<DiffCase>)

@Serializable
data class DiffCase(
    val id: String,
    val spec: String = "",
    val trend: String = "auto",
    val symbols: SymbolsJson = SymbolsJson(),
    val from: NumberJson? = null,
    val to: NumberJson,
    val animateFirstDisplay: Boolean = false,
    val expect: DiffExpect,
    val platforms: List<String>? = null,
    val reason: String? = null,
)

@Serializable
data class SymbolsJson(
    val decimal: String = ".",
    val group: String = ",",
    val minus: List<String> = listOf("-", "−"),
)

@Serializable
data class NumberJson(
    val text: String,
    val value: String,
    val prefix: String = "",
    val suffix: String = "",
    val formatKey: String = "default",
)

@Serializable
data class DiffExpect(val direction: String, val elements: List<DiffElementJson>)

@Serializable
data class DiffElementJson(
    val key: String,
    val kind: String,
    val from: String?,
    val to: String?,
    val action: String,
)

@Serializable
data class SessionFile(val schema: Int, val kind: String, val cases: List<SessionCase>)

@Serializable
data class SessionCase(
    val id: String,
    val spec: String = "",
    val trend: String = "auto",
    val symbols: SymbolsJson = SymbolsJson(),
    val members: List<MemberJson>,
    val steps: List<StepJson>,
    val expect: List<ExpectJson>,
    val platforms: List<String>? = null,
    val reason: String? = null,
)

@Serializable
data class MemberJson(
    val id: String,
    val label: String? = null,
    val announce: Boolean = true,
    val animateFirstDisplay: Boolean = false,
)

@Serializable
data class StepJson(
    val t: Long,
    val set: Map<String, NumberJson>? = null,
    val advance: Boolean? = null,
    val disableAnimation: String? = null,
    val enableAnimation: String? = null,
    val reduceMotion: Boolean? = null,
    val itemKey: Map<String, String>? = null,
)

@Serializable
data class ExpectJson(
    val event: String? = null,
    val member: String? = null,
    val t: Long? = null,
    val announce: String? = null,
)

/** True when this case should run on the given platform; fails when a restriction has no reason. */
fun runsOn(platform: String, id: String, platforms: List<String>?, reason: String?): Boolean {
    if (platforms == null) return true
    require(!reason.isNullOrBlank()) { "Fixture '$id' restricts platforms without a reason" }
    return platform in platforms
}
