package no.nav.modia.soknadsstatus

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet

object BehandlingSerializer : JsonContentPolymorphicSerializer<Behandling>(Behandling::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Behandling> = when {
        "avslutningsstatus" in element.jsonObject -> BehandlingAvsluttet.serializer()
        else -> BehandlingOpprettet.serializer()
    }
}
