package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.InnkommendeBehandling
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

    suspend fun onNewBehandling(innkommendeBehandling: InnkommendeBehandling)

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Hendelse>

    suspend fun getAllForIdent(
        userToken: String,
        ident: String,
    ): List<SoknadsstatusDomain.Hendelse>
}

class HendelseServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val pdlOppslagServiceQ1: PdlOppslagService,
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

    override suspend fun onNewBehandling(innkommendeBehandling: InnkommendeBehandling) {
        hendelseRepository.useTransactionConnection {
            val behandling = behandlingService.upsert(it, toBehandlingDAO(innkommendeBehandling))
            if (behandling != null) {
                val hendelse =
                    hendelseRepository.upsert(
                        it,
                        behandlingToHendelseDAO(requireNotNull(behandling.id), innkommendeBehandling),
                    )

                behandlingEierService.upsert(
                    it,
                    BehandlingEierDAO(aktorId = innkommendeBehandling.aktoerId, behandlingId = requireNotNull(behandling.id)),
                )
                hendelseEierService.upsert(
                    it,
                    HendelseEierDAO(aktorId = innkommendeBehandling.aktoerId, hendelseId = requireNotNull(hendelse?.id)),
                )
            } else {
                TjenestekallLogg.warn(
                    header = "ingnorerer eldre behandling. Nyere behandling finnes i databasen",
                    fields =
                        mapOf(
                            "behandlingsId" to innkommendeBehandling.behandlingId,
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

    private fun behandlingToHendelseDAO(
        modiaBehandlingId: String,
        behandling: InnkommendeBehandling,
    ): SoknadsstatusDomain.Hendelse =
        SoknadsstatusDomain.Hendelse(
            modiaBehandlingId = modiaBehandlingId,
            hendelseId = behandling.behandlingId,
            behandlingId = behandling.behandlingId,
            behandlingsTema = behandling.behandlingsTema,
            behandlingsType = behandling.behandlingsType,
            hendelseProdusent = behandling.produsentSystem,
            hendelseType = mapToHendelseType(behandling.status),
            hendelseTidspunkt = behandling.startTidspunkt,
            status = mapStatus(behandling.status),
            ansvarligEnhet = behandling.ansvarligEnhet,
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

    private fun toBehandlingDAO(behandling: InnkommendeBehandling): SoknadsstatusDomain.Behandling =
        SoknadsstatusDomain.Behandling(
            behandlingId = behandling.behandlingId,
            produsentSystem = behandling.produsentSystem,
            startTidspunkt = behandling.startTidspunkt,
            sluttTidspunkt = behandling.sluttTidspunkt,
            sistOppdatert = behandling.sistOppdatert,
            primaerBehandlingId = behandling.primaerBehandlingId,
            primaerBehandlingType = behandling.primaerBehandlingType,
            applikasjonSak = behandling.applikasjonSak,
            applikasjonBehandling = behandling.applikasjonBehandling,
            status = mapStatus(behandling.status),
            behandlingsTema = behandling.behandlingsTema,
            ansvarligEnhet = behandling.ansvarligEnhet,
            sakstema = behandling.sakstema,
            behandlingsType = behandling.behandlingsType,
            sobFlag = true,
        )

    private fun mapStatus(status: String?): SoknadsstatusDomain.Status =
        when (status) {
            "opprettet" -> SoknadsstatusDomain.Status.UNDER_BEHANDLING
            "avsluttet" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
            else -> throw IllegalArgumentException("Mottok ukjent status i mapping av statuser")
        }

    private fun mapToHendelseType(status: String?): SoknadsstatusDomain.HendelseType =
        when (status) {
            "opprettet" -> SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET
            "avsluttet", "avbrutt" -> SoknadsstatusDomain.HendelseType.BEHANDLING_AVSLUTTET
            else -> throw IllegalArgumentException("Mottok ukjent status i mapping av statuser")
        }
}
