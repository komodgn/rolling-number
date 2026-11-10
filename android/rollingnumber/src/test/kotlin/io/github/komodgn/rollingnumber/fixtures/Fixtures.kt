package io.github.komodgn.rollingnumber.fixtures

import java.io.File

/** Reads the repo-root `fixtures/` folder (path passed by Gradle as a system property). */
object Fixtures {
    private val root: File by lazy {
        val path = System.getProperty("rollingnumber.fixtures")
            ?: error("System property 'rollingnumber.fixtures' is not set (configured in build.gradle.kts)")
        File(path).also { require(it.isDirectory) { "Fixtures folder not found: $it" } }
    }

    fun files(kind: String): List<File> {
        val dir = File(root, kind)
        require(dir.isDirectory) { "Fixture folder missing: $dir" }
        return dir.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
    }

    private fun checkHeader(file: File, text: String, kind: String) {
        val header = fixtureJson.decodeFromString(HeaderOnly.serializer(), text)
        require(header.schema == 1) { "${file.name}: unknown schema ${header.schema}" }
        require(header.kind == kind) { "${file.name}: kind '${header.kind}' in '$kind' folder" }
    }

    fun format(): List<Pair<String, FormatCase>> = files("format").flatMap { f ->
        val text = f.readText()
        checkHeader(f, text, "format")
        fixtureJson.decodeFromString(FormatFile.serializer(), text).cases
            .filter { runsOn("android", it.id, it.platforms, it.reason) }
            .map { f.name to it }
    }

    fun diff(): List<Pair<String, DiffCase>> = files("diff").flatMap { f ->
        val text = f.readText()
        checkHeader(f, text, "diff")
        fixtureJson.decodeFromString(DiffFile.serializer(), text).cases
            .filter { runsOn("android", it.id, it.platforms, it.reason) }
            .map { f.name to it }
    }

    fun session(): List<Pair<String, SessionCase>> = files("session").flatMap { f ->
        val text = f.readText()
        checkHeader(f, text, "session")
        fixtureJson.decodeFromString(SessionFile.serializer(), text).cases
            .filter { runsOn("android", it.id, it.platforms, it.reason) }
            .map { f.name to it }
    }
}

@kotlinx.serialization.Serializable
private data class HeaderOnly(val schema: Int, val kind: String, val cases: kotlinx.serialization.json.JsonArray)
