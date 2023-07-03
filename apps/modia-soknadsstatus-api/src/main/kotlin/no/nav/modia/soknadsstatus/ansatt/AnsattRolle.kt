package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.AzureObjectId

data class AnsattRolle(
    val gruppeNavn: String,
    val gruppeId: AzureObjectId,
) {
    override fun hashCode() = gruppeId.hashCode()
    override fun equals(other: Any?) = other?.let { gruppeId == (it as AnsattRolle).gruppeId } ?: false
}
