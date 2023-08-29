package no.nav.modia.soknadsstatus

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.modia.soknadsstatus.behandling.Hendelse
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet

object BehandlingSerializer : JsonContentPolymorphicSerializer<Hendelse>(Hendelse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Hendelse> = when {
        "avslutningsstatus" in element.jsonObject -> BehandlingAvsluttet.serializer()
        else -> BehandlingOpprettet.serializer()
    }
}
