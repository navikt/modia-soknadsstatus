package no.nav.modia.soknadsstatus

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.descriptors.SerialDescriptor

@Serializable
data class InnkommendeBehandling(
    @SerialName("aktorId") val aktoerId: String,
    val behandlingId: String,
    val produsentSystem: String? = null,
    @Serializable(with = DateIntArraySerializer::class) val startTidspunkt: LocalDateTime,
    @Serializable(with = DateIntArraySerializer::class) val sluttTidspunkt: LocalDateTime? = null,
    @Serializable(with = DateIntArraySerializer::class) val sistOppdatert: LocalDateTime,
    val sakstema: String? = null,
    val behandlingsTema: String? = null,
    val behandlingsType: String? = null,
    val status: String? = null,
    val ansvarligEnhet: String? = null,
    val primaerBehandlingId: String? = null,
    val primaerBehandlingType: String? = null,
    val applikasjonSak: String? = null,
    val applikasjonBehandling: String? = null,
)

class DateIntArraySerializer : KSerializer<LocalDateTime> {
    private val delegateSerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("ArrayDateTime", delegateSerializer.descriptor)
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val data = intArrayOf(
            value.year,
            value.monthNumber,
            value.dayOfMonth,
            value.hour,
            value.minute,
            value.second
        )
        encoder.encodeSerializableValue(delegateSerializer, data)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        val date = LocalDate(array[0], array[1], array[2])
        val time = LocalTime(array.getOrNull(3) ?: 0,  array.getOrNull(4 )?: 0, array.getOrNull(5 ) ?: 0, array.getOrNull(6) ?: 0)
        return LocalDateTime(date, time)
    }
}