package no.nav.api.pdl.converters

import com.expediagroup.graphql.client.converter.ScalarConverter
import kotlinx.datetime.LocalDateTime

class DateTimeScalarConverter : ScalarConverter<LocalDateTime> {
    override fun toJson(value: LocalDateTime): String = value.toString()

    override fun toScalar(rawValue: Any): LocalDateTime = LocalDateTime.parse(rawValue.toString())
}
