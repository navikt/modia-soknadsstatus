package no.nav.modia.soknadstatus

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.modia.soknadstatus.behandling.Behandling
import no.nav.modia.soknadstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadstatus.behandling.BehandlingOpprettet

object BehandlingSerializer : JsonContentPolymorphicSerializer<Behandling>(Behandling::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Behandling> = when {
        "avslutningsstatus" in element.jsonObject -> BehandlingAvsluttet.serializer()
        else -> BehandlingOpprettet.serializer()
    }
}
