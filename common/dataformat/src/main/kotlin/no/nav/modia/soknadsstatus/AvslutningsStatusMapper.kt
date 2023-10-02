package no.nav.modia.soknadsstatus

interface AvslutningsStatusMapper {
    fun getAvslutningsstatus(produsentSystem: String, status: String): SoknadsstatusDomain.Status
}
