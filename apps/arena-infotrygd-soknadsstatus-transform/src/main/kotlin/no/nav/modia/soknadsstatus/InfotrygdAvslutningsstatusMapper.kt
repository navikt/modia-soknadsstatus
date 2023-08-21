package no.nav.modia.soknadsstatus

import java.io.InputStream

data class MappedStatusEntry(
    private val key: String,
    val status: SoknadsstatusDomain.Status,
)

object InfotrygdAvslutningsstatusMapper : AvslutningsStatusMapper {
    private val mapper = AvslutningsMapper()

    override fun getAvslutningsstatus(status: String): SoknadsstatusDomain.Status = mapper.getMappedStatus(status)
}

private class AvslutningsMapper {
    private var statusMap: Map<String, MappedStatusEntry>? = null

    init {
        loadMappedStatus()
    }

    companion object {
        const val FILE_NAME = "/avslutningsstatus-mapping.csv"
    }

    private fun loadMappedStatus() {
        val resource = this::class.java.getResource(FILE_NAME)
        statusMap = readCsv(resource.openStream())
    }

    private fun readCsv(inputStream: InputStream): Map<String, MappedStatusEntry> = inputStream.use {
        val res = mutableMapOf<String, MappedStatusEntry>()
        val reader = it.bufferedReader()
        for (line in reader.lineSequence()) {
            if (line.isBlank()) continue
            val (key, _, status) = line.split(",", ignoreCase = false, limit = 3)
            val mappedStatus = mapStatus(status)
            val lowerCaseKey = key.lowercase()
            res[lowerCaseKey] = MappedStatusEntry(lowerCaseKey, mappedStatus)
        }
        return res
    }

    private fun mapStatus(status: String): SoknadsstatusDomain.Status {
        return when (status) {
            "avsluttet" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
            else -> throw IllegalArgumentException("Mottok ukjent status i mapping av infotrygd arena statuser")
        }
    }

    fun getMappedStatus(status: String): SoknadsstatusDomain.Status {
        if (statusMap == null) {
            throw IllegalStateException("StatusMap var null da status: $status skulle konverteres")
        }
        val mappedStatus = statusMap!![status.lowercase()]
            ?: throw IllegalArgumentException("StatusMap inneholdt ikke følgende status: $status")
        return mappedStatus.status
    }
}
