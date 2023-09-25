package no.nav.modia.soknadsstatus

interface AvslutningsStatusMapper {
    fun getAvslutningsstatus(status: String): SoknadsstatusDomain.Status
}
