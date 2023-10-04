package no.nav.modia.soknadsstatus

data class MappedStatusEntry(
    private val key: String,
    val status: SoknadsstatusDomain.Status,
)

object ArenaInfotrygdAvslutningsstatusMapper : AvslutningsStatusMapper {
    private val mapper = AvslutningsMapper()

    override fun getAvslutningsstatus(
        produsentSystem: String,
        status: String,
    ): SoknadsstatusDomain.Status = mapper.getMappedStatus(produsentSystem, status)
}

private class AvslutningsMapper : CSVLoader(FILE_NAME) {
    private var infotrygdStatusMap: Map<String, MappedStatusEntry>? = null
    private var arenaStatusMap =
        mapOf<String, SoknadsstatusDomain.Status>(
            "avbrutt" to SoknadsstatusDomain.Status.AVBRUTT,
            "avvist" to SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
            "delvis" to SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
            "ja" to SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
            "nei" to SoknadsstatusDomain.Status.AVBRUTT,
            "opphevet" to SoknadsstatusDomain.Status.AVBRUTT,
            "trukk" to SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
        )

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
        infotrygdStatusMap = res
    }

    private fun mapStatus(status: String): SoknadsstatusDomain.Status =
        when (status) {
            "avsluttet" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
            "under_behandling" -> SoknadsstatusDomain.Status.UNDER_BEHANDLING
            else -> throw IllegalArgumentException("Mottok ukjent status i mapping av infotrygd arena statuser")
        }

    fun getMappedStatus(
        produsentSystem: String,
        status: String,
    ): SoknadsstatusDomain.Status =
        if (produsentSystem == ARENA) {
            getMappedArenaStatus(status)
        } else if (produsentSystem == INFOTRYGD) {
            getMappedInfotrygdStatus(status)
        } else {
            throw IllegalArgumentException("Mottok ukjent produsent system: $produsentSystem")
        }

    fun getMappedArenaStatus(status: String) =
        arenaStatusMap[status.lowercase()]
            ?: throw IllegalArgumentException("Fant ikke mapping for følgende arena status: $status")

    fun getMappedInfotrygdStatus(status: String): SoknadsstatusDomain.Status {
        if (infotrygdStatusMap == null) {
            throw IllegalStateException("StatusMap var null da status: $status skulle konverteres")
        }
        val mappedStatus =
            infotrygdStatusMap!![status.lowercase()]
                ?: throw IllegalArgumentException("StatusMap inneholdt ikke følgende status: $status")
        return mappedStatus.status
    }
}
