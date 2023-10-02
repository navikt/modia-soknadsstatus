package no.nav.modia.soknadsstatus

object HendelseAvslutningsstatusMapper : AvslutningsStatusMapper {
    override fun getAvslutningsstatus(produsentSystem: String, status: String): SoknadsstatusDomain.Status {
        return when (status) {
            "avsluttet", "ok", "ja" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt", "nei", "no" -> SoknadsstatusDomain.Status.AVBRUTT
            else -> throw IllegalArgumentException("Ukjent behandlingsstatus mottatt: $status")
        }
    }
}
