package no.nav.modia.soknadsstatus

object ArenaInfotrygdTemaTypeMapper {
    private val mapper = AremaInfotrygdMapper()

    fun getMappedArkivTema(tema: String): String = mapper.getMappedArkivTema(tema)

    fun getMappedBehandlingTema(tema: String): String = mapper.getMappedBehandlingTema(tema)

    fun getMappedBehandlingsType(behandlingsType: String): String = mapper.getMappedBehandlingsType(behandlingsType)
}

private class AremaInfotrygdMapper : CSVLoader(FILE_NAME) {
    private var arkivTemaMap: Map<String, String>? = null
    private var behandlingTemaMap: Map<String, String>? = null
    private var behandlingsTypeMap: Map<String, String>? = null

    companion object {
        private const val FILE_NAME = "/kodeverks_mapping.csv"
    }

    init {
        loadTema()
    }

    private fun loadTema() {
        val arkivTemaMap = mutableMapOf<String, String>()
        val behandlingTemaMap = mutableMapOf<String, String>()
        val behandlingsTypeMap = mutableMapOf<String, String>()
        loadResult {
            val (_, kodeverkTil, orginalVerdi, nyVerdi) = it.split(",")
            when (kodeverkTil) {
                "Arkivtema" -> arkivTemaMap[orginalVerdi] = nyVerdi
                "Behandlingstema" -> behandlingTemaMap[orginalVerdi] = nyVerdi
                "Behandlingstype" -> behandlingsTypeMap[orginalVerdi] = nyVerdi
                "Avslutningsstatus" -> null
                else -> throw IllegalArgumentException("Mottok ukjent mapping: $kodeverkTil")
            }
        }
        this.arkivTemaMap = arkivTemaMap
        this.behandlingTemaMap = behandlingTemaMap
        this.behandlingsTypeMap = behandlingsTypeMap
    }

    fun getMappedArkivTema(originaltTema: String): String {
        if (arkivTemaMap == null) {
            throw IllegalStateException("ArkivTemaMap er ikke initialisert")
        }

        return arkivTemaMap!![originaltTema]
            ?: throw IllegalArgumentException("Hadde ikke mapping for origintaltTema: $originaltTema")
    }

    fun getMappedBehandlingTema(originaltTema: String): String {
        if (behandlingTemaMap == null) {
            throw IllegalStateException("BehandlingTemaMap er ikke initialisert")
        }

        return behandlingTemaMap!![originaltTema]
            ?: throw IllegalArgumentException("Hadde ikke mapping for originaltTema: $originaltTema")
    }

    fun getMappedBehandlingsType(originalType: String): String {
        if (behandlingsTypeMap == null) {
            throw IllegalStateException("BehandlingsTypeMap er ikke initialisert")
        }

        return behandlingsTypeMap!![originalType]
            ?: throw IllegalArgumentException("Hadde ikke mapping for originalType: $originalType")
    }
}
