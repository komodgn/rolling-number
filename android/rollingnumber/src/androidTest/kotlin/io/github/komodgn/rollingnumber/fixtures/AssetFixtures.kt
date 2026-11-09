package io.github.komodgn.rollingnumber.fixtures

import androidx.test.platform.app.InstrumentationRegistry

/** Reads the shared fixtures packaged as androidTest assets (same rules as the JVM loader). */
object AssetFixtures {
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets

    private fun read(kind: String): List<Pair<String, String>> =
        assets.list(kind).orEmpty().filter { it.endsWith(".json") }.sorted().map { name ->
            val text = assets.open("$kind/$name").bufferedReader().use { it.readText() }
            val header = fixtureJson.decodeFromString(
                kotlinx.serialization.json.JsonObject.serializer(), text,
            )
            val schema = header["schema"].toString().toInt()
            require(schema == 1) { "$name: unknown schema $schema" }
            name to text
        }

    fun format(): List<Pair<String, FormatCase>> = read("format").flatMap { (name, text) ->
        fixtureJson.decodeFromString(FormatFile.serializer(), text).cases
            .filter { runsOn("android", it.id, it.platforms, it.reason) }
            .map { name to it }
    }
}
