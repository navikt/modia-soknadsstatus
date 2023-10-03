package no.nav.modia.soknadsstatus

object ArenaTemaTypeMapper {
    private val mapper = ArenaMapper()

    fun getMappedArkivTema(
        tema: String,
        type: String,
    ): String = mapper.getMappedArkivTema(tema, type)

    fun getMappedBehandlingTema(
        tema: String,
        type: String,
    ): String = mapper.getMappedBehandlingTema(tema, type)

    fun getMappedBehandlingsType(
        tema: String,
        type: String,
    ): String = mapper.getMappedBehandlingsType(tema, type)
}

private class ArenaMapper : CSVLoader(FILE_NAME) {
    private lateinit var temaTypeMap: Map<String, TemaType>

    companion object {
        private const val FILE_NAME = "/kodeverks_mapping_arena.csv"
    }

    init {
        loadTema()
    }

    private fun loadTema() {
        val temaTypeMap = mutableMapOf<String, TemaType>()
        loadResult(skipFirstLine = true) {
            val (arkivTemaBehandlingstype, arkivTema, behandlingsTema, behandlingsType) = it.split(",", limit = 4)

            temaTypeMap[arkivTemaBehandlingstype] = TemaType(
                arkivTema = arkivTema,
                behandlingsTema = behandlingsTema,
                behandlingsType = behandlingsType
            )
        }
        this.temaTypeMap = temaTypeMap
    }

    fun getMappedArkivTema(
        arkivTema: String,
        behandlingsType: String,
    ): String {
        val concatString = getArkivtemaBehandlingstype(arkivTema, behandlingsType)
        return temaTypeMap[concatString]?.arkivTema
            ?: throw IllegalArgumentException("Hadde ikke mapping for arkivtema: $concatString")
    }

    fun getMappedBehandlingTema(
        arkivTema: String,
        behandlingsType: String,
    ): String {
        val concatString = getArkivtemaBehandlingstype(arkivTema, behandlingsType)
        return temaTypeMap[concatString]?.behandlingsTema
            ?: throw IllegalArgumentException("Hadde ikke mapping for behandlingstema: $concatString")
    }

    fun getMappedBehandlingsType(
        arkivTema: String,
        behandlingsType: String,
    ): String {
        val concatString = getArkivtemaBehandlingstype(arkivTema, behandlingsType)
        return temaTypeMap[concatString]?.behandlingsType
            ?: throw IllegalArgumentException("Hadde ikke mapping for behandlingstype: $concatString")
    }

    private fun getArkivtemaBehandlingstype(
        tema: String,
        type: String,
    ) = "$tema-$type"
}

private data class TemaType(
    val arkivTema: String,
    val behandlingsTema: String,
    val behandlingsType: String
)