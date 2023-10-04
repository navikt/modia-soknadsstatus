package no.nav.api.pdl.converters

import com.expediagroup.graphql.client.converter.ScalarConverter

@JvmInline
value class PdlLong(
    val value: Long,
)

class LongScalarConverter : ScalarConverter<PdlLong> {
    override fun toJson(value: PdlLong): String = value.value.toString()

    override fun toScalar(rawValue: Any): PdlLong = PdlLong(rawValue.toString().toLong())
}
