package no.nav.modia.soknadsstatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

object SoknadsstatusDomain {
    @Serializable
    enum class Status {
        UNDER_BEHANDLING,
        FERDIG_BEHANDLET,
        AVBRUTT,
    }

    @Serializable
    data class SoknadsstatusOppdatering(
        val ident: String,
        val behandlingsId: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    ) {
        private val systemUrl = mapOf<String, String>()
        fun url() = "https://${systemUrl[systemRef]}/$behandlingsId"
    }

    @Serializable
    data class SoknadsstatusInnkommendeOppdatering(
        val aktorIder: List<String>,
        val behandlingsId: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    )

    class SoknadsstatusInkommendeOppdateringSerde : Serde<SoknadsstatusInnkommendeOppdatering> {
        override fun serializer() = SoknadsstatusInnkommendeOppdateringSerializer()
        override fun deserializer() = SoknadsstatusInnkommendeOppdateringDeserializer()
    }

    @Serializable
    data class Soknadsstatuser(
        val ident: String,
        val tema: Map<String, Soknadsstatus>,
    )

    @Serializable
    data class Soknadsstatus(
        var underBehandling: Int = 0,
        var ferdigBehandlet: Int = 0,
        var avbrutt: Int = 0,
    )
}

class SoknadsstatusInnkommendeOppdateringSerializer :
    Serializer<SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering> {
    override fun serialize(topic: String?, data: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?): ByteArray {
        if (data == null) {
            return ByteArray(0)
        }
        val encodedSoknadsstatus =
            Encoding.encode(SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(), data)
        return encodedSoknadsstatus.encodeToByteArray()
    }
}

class SoknadsstatusInnkommendeOppdateringDeserializer :
    Deserializer<SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering> {
    override fun deserialize(
        topic: String?,
        data: ByteArray?,
    ): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering? {
        if (data == null) {
            return null
        }
        val encodedString = data.toString(Charsets.UTF_8)
        return Encoding.decode(SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(), encodedString)
    }
}
