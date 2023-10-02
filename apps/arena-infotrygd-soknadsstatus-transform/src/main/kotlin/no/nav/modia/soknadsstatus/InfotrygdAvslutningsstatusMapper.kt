package no.nav.modia.soknadsstatus

data class MappedStatusEntry(
    private val key: String,
    val status: SoknadsstatusDomain.Status,
)

object InfotrygdAvslutningsstatusMapper : AvslutningsStatusMapper {
    private val mapper = AvslutningsMapper()

    override fun getAvslutningsstatus(status: String): SoknadsstatusDomain.Status = mapper.getMappedStatus(status)
}

private class AvslutningsMapper : CSVLoader(FILE_NAME) {
    private var statusMap: Map<String, MappedStatusEntry>? = null

    init {
        loadStatuses()
    }

    companion object {
        const val FILE_NAME = "/avslutningsstatus-mapping.csv"
    }

    private fun loadStatuses() {
        val res = mutableMapOf<String, MappedStatusEntry>()
        loadResult {
            val (key, _, status) = it.split(",", ignoreCase = false, limit = 3)
            val mappedStatus = mapStatus(status)
            val lowerCaseKey = key.lowercase()
            res[lowerCaseKey] = MappedStatusEntry(lowerCaseKey, mappedStatus)
        }
        statusMap = res
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
        val mappedStatus =
            statusMap!![status.lowercase()]
                ?: throw IllegalArgumentException("StatusMap inneholdt ikke følgende status: $status")
        return mappedStatus.status
    }
}
