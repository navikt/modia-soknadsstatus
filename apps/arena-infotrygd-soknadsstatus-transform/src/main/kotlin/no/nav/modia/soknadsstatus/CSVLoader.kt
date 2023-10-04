package no.nav.modia.soknadsstatus

import java.net.URL

open class CSVLoader(
    private val fileName: String,
) {
    private var resource: URL? = null

    private fun loadFile() {
        resource = this::class.java.getResource(fileName)
    }

    private fun readCsv(
        skipFirstLine: Boolean,
        mapper: (line: String) -> Unit,
    ) {
        val stream = resource?.openStream() ?: throw IllegalStateException("Failed to load csv")
        stream.use {
            val reader = it.bufferedReader()
            var firstLine = true
            for (line in reader.lineSequence()) {
                if (firstLine && skipFirstLine) {
                    firstLine = false
                    continue
                }
                if (line.isBlank()) continue
                mapper(line)
            }
            reader.close()
        }
    }

    fun loadResult(
        skipFirstLine: Boolean = false,
        mapper: (line: String) -> Unit,
    ) {
        loadFile()
        readCsv(skipFirstLine, mapper)
    }
}
