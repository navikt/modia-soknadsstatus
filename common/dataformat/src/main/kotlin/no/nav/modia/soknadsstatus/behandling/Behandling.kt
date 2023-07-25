package no.nav.modia.soknadsstatus.behandling

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.modia.soknadsstatus.Encoding
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.nio.charset.StandardCharsets

@Serializable
sealed class Behandling {
    abstract val aktoerREF: List<AktoerREF>
    abstract val ansvarligEnhetREF: String
    abstract val applikasjonBehandlingREF: String?
    abstract val applikasjonSakREF: String
    abstract val behandlingsID: String
    abstract val behandlingstema: Behandlingstema?
    abstract val behandlingstype: Behandlingstype?
    abstract val hendelseType: String
    abstract val hendelsesId: String
    abstract val hendelsesTidspunkt: LocalDateTime
    abstract val hendelsesprodusentREF: HendelsesprodusentREF
    abstract val primaerBehandlingREF: PrimaerBehandlingREF?
    abstract val sakstema: Sakstema
    abstract val sekundaerBehandlingREF: List<SekundaerBehandlingREF>
    abstract val styringsinformasjonListe: List<StyringsinformasjonListe>
}

object BehandlingJsonSerdes {
    class BehandlingJsonSerializer : Serializer<Behandling> {
        override fun serialize(topic: String?, data: Behandling?): ByteArray {
            if (data == null) {
                return ByteArray(0)
            }
            val encodedBehandling = Encoding.encode(Behandling.serializer(), data)
            return encodedBehandling.encodeToByteArray()
        }
    }

    class BehandlingJsonDeserializer : Deserializer<Behandling> {
        override fun deserialize(topic: String?, data: ByteArray?): Behandling? {
            if (data == null) {
                return null
            }

            val encodedString = String(data, StandardCharsets.UTF_8)
            return Json.decodeFromString(BehandlingSerializer, encodedString)
        }
    }

    class JsonSerde : Serde<Behandling> {
        override fun serializer() = BehandlingJsonSerializer()

        override fun deserializer() = BehandlingJsonDeserializer()
    }
}

object BehandlingSerializer : JsonContentPolymorphicSerializer<Behandling>(Behandling::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Behandling> = when {
        "avslutningsstatus" in element.jsonObject -> BehandlingAvsluttet.serializer()
        else -> BehandlingOpprettet.serializer()
    }
}
