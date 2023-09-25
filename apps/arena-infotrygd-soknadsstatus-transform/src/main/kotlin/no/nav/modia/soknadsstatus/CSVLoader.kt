package no.nav.modia.soknadsstatus

import java.net.URL

open class CSVLoader(private val fileName: String) {
    private var resource: URL? = null

    private fun loadFile() {
        resource = this::class.java.getResource(fileName)
    }

    private fun readCsv(mapper: (line: String) -> Unit) {
        val stream = resource?.openStream() ?: throw IllegalStateException("Failed to load csv")
        stream.use {
            val reader = it.bufferedReader()
            for (line in reader.lineSequence()) {
                if (line.isBlank()) continue
                mapper(line)
            }
        }
    }

    fun loadResult(mapper: (line: String) -> Unit) {
        loadFile()
        readCsv(mapper)
    }
}
