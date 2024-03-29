package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseRepository
import no.nav.personoversikt.common.logging.TjenestekallLogg

interface HendelseService {
    fun init(behandlingService: BehandlingService)

    suspend fun onNewHendelse(innkommendeHendelse: InnkommendeHendelse)

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Hendelse>

    suspend fun getAllForIdent(
        userToken: String,
        ident: String,
    ): List<SoknadsstatusDomain.Hendelse>
}

class HendelseServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val hendelseRepository: HendelseRepository,
    private val behandlingEierService: BehandlingEierService,
    private val hendelseEierService: HendelseEierService,
) : HendelseService {
    private lateinit var behandlingService: BehandlingService

    override fun init(behandlingService: BehandlingService) {
        this.behandlingService = behandlingService
    }

    override suspend fun onNewHendelse(innkommendeHendelse: InnkommendeHendelse) {
        hendelseRepository.useTransactionConnection {
            val behandling = behandlingService.upsert(it, hendelseToBehandlingDAO(innkommendeHendelse))
            if (behandling != null) {
                val hendelse =
                    hendelseRepository.upsert(
                        it,
                        hendelseToHendelseDAO(requireNotNull(behandling.id), innkommendeHendelse),
                    )

                var identer = innkommendeHendelse.identer
                if (identer.isEmpty()) {
                    identer = fetchIdents(innkommendeHendelse.aktoerer)
                }

                for (ident in identer) {
                    behandlingEierService.upsert(
                        it,
                        BehandlingEierDAO(ident = ident, behandlingId = requireNotNull(behandling.id)),
                    )
                    hendelseEierService.upsert(
                        it,
                        HendelseEierDAO(ident = ident, hendelseId = requireNotNull(hendelse?.id)),
                    )
                }
            } else {
                TjenestekallLogg.warn(
                    header = "ingnorerer eldre behandling. Nyere behandling finnes i databasen",
                    fields =
                        mapOf(
                            "behandlingsId" to innkommendeHendelse.behandlingsId,
                            "hendelsesId" to innkommendeHendelse.hendelsesId,
                        ),
                )
            }
        }
    }

    override suspend fun getAllForIdent(
        userToken: String,
        ident: String,
    ): List<SoknadsstatusDomain.Hendelse> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdents(idents)
    }

    override suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Hendelse> =
        hendelseRepository.getByIdents(
            idents.toTypedArray(),
        )

    private suspend fun fetchIdents(aktoerer: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (aktoer in aktoerer) {
            if (isAktoerId(aktoer)) {
                try {
                    val ident =
                        pdlOppslagService.hentFnrMedSystemToken(aktoer)
                    if (ident == null) {
                        TjenestekallLogg.warn(
                            "Ignorerer at PDL ikke returnerte ident for aktoer: $aktoer",
                            fields = mapOf("aktoer" to aktoer),
                        )
                        return result
                    } else {
                        result.add(ident)
                    }
                } catch (e: IllegalArgumentException) {
                    TjenestekallLogg.warn(
                        "Ignorerer at PDL ikke returnerte ident for aktoer: $aktoer",
                        fields = mapOf("aktoer" to aktoer),
                    )
                    return result
                }
            } else {
                result.add(aktoer)
            }
        }
        return result
    }

    private fun isAktoerId(aktoerId: String): Boolean {
        val regex = Regex("^\\d{13}$")

        return aktoerId.matches(regex)
    }

    private fun hendelseToHendelseDAO(
        modiaBehandlingId: String,
        hendelse: InnkommendeHendelse,
    ): SoknadsstatusDomain.Hendelse =
        SoknadsstatusDomain.Hendelse(
            modiaBehandlingId = modiaBehandlingId,
            hendelseId = hendelse.hendelsesId,
            behandlingId = hendelse.behandlingsId,
            behandlingsTema = hendelse.behandlingsTema,
            behandlingsType = hendelse.behandlingsType,
            hendelseProdusent = hendelse.hendelsesProdusent,
            hendelseType = hendelse.hendelsesType,
            hendelseTidspunkt = hendelse.hendelsesTidspunkt,
            status = hendelse.status,
            ansvarligEnhet = hendelse.ansvarligEnhet,
        )

    private fun hendelseToBehandlingDAO(hendelse: InnkommendeHendelse): SoknadsstatusDomain.Behandling {
        val sluttTidspunkt =
            if (hendelse.hendelsesType != SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET) {
                hendelse.hendelsesTidspunkt
            } else {
                hendelse.hendelsesTidspunkt
            }

        val startTidspunkt =
            if (hendelse.hendelsesType == SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET) {
                hendelse.hendelsesTidspunkt
            } else if (hendelse.hendelsesType == SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET_OG_AVSLUTTET) {
                hendelse.opprettelsesTidspunkt
            } else {
                hendelse.hendelsesTidspunkt
            }

        return SoknadsstatusDomain.Behandling(
            behandlingId = hendelse.behandlingsId,
            produsentSystem = hendelse.hendelsesProdusent,
            startTidspunkt = startTidspunkt,
            sluttTidspunkt = sluttTidspunkt,
            sistOppdatert = hendelse.hendelsesTidspunkt,
            primaerBehandlingId = hendelse.primaerBehandling?.behandlingId,
            primaerBehandlingType = hendelse.primaerBehandling?.type,
            applikasjonSak = hendelse.applikasjonSak,
            applikasjonBehandling = hendelse.applikasjonBehandling,
            status = hendelse.status,
            behandlingsTema = hendelse.behandlingsTema,
            ansvarligEnhet = hendelse.ansvarligEnhet,
            sakstema = hendelse.sakstema,
            behandlingsType = hendelse.behandlingsType,
        )
    }
}
