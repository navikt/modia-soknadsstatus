package no.nav.modia.soknadsstatus

object ArenaTemaTypeMapper {
    private val mapper = ArenaMapper()

    fun getMappedArkivTema(tema: String, type: String): String = mapper.getMappedArkivTema(tema, type)

    fun getMappedBehandlingTema(tema: String, type: String): String = mapper.getMappedBehandlingTema(tema, type)

    fun getMappedBehandlingsType(tema: String, type: String): String = mapper.getMappedBehandlingsType(tema, type)
}

private class ArenaMapper : CSVLoader(FILE_NAME) {
    private var arkivTemaMap: Map<String, String>? = null
    private var behandlingTemaMap: Map<String, String>? = null
    private var behandlingsTypeMap: Map<String, String>? = null

    companion object {
        private const val FILE_NAME = "/kodeverks_mapping_arena.csv"
    }

    init {
        loadTema()
    }

    private fun loadTema() {
        val arkivTemaMap = mutableMapOf<String, String>()
        val behandlingTemaMap = mutableMapOf<String, String>()
        val behandlingsTypeMap = mutableMapOf<String, String>()
        loadResult {
            val (_, _, orginalVerdi, kodeverkTil, nyVerdi) = it.split(",", limit = 6)
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

    fun getMappedArkivTema(arkivTema: String, behandlingsType: String): String {
        if (arkivTemaMap == null) {
            throw IllegalStateException("ArkivTemaMap er ikke initialisert")
        }

        return arkivTemaMap!![getArkivtemaBehandlingstype(arkivTema, behandlingsType)]
            ?: throw IllegalArgumentException("Hadde ikke mapping for origintaltTema: ${getArkivtemaBehandlingstype(arkivTema, behandlingsType)}")
    }

    fun getMappedBehandlingTema(arkivTema: String, behandlingsType: String): String {
        if (behandlingTemaMap == null) {
            throw IllegalStateException("BehandlingTemaMap er ikke initialisert")
        }

        return behandlingTemaMap!![getArkivtemaBehandlingstype(arkivTema, behandlingsType)]
            ?: throw IllegalArgumentException("Hadde ikke mapping for originaltTema: ${getArkivtemaBehandlingstype(arkivTema, behandlingsType)}")
    }

    fun getMappedBehandlingsType(arkivTema: String, behandlingsType: String): String {
        if (behandlingsTypeMap == null) {
            throw IllegalStateException("BehandlingsTypeMap er ikke initialisert")
        }

        return behandlingsTypeMap!![getArkivtemaBehandlingstype(arkivTema, behandlingsType)]
            ?: throw IllegalArgumentException("Hadde ikke mapping for originalType: ${getArkivtemaBehandlingstype(arkivTema, behandlingsType)}")
    }

    private fun getArkivtemaBehandlingstype(tema: String, type: String) = "$tema-$type"
}
