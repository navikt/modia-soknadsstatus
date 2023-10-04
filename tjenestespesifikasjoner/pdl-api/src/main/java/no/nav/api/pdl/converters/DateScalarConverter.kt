package no.nav.api.pdl.converters

import com.expediagroup.graphql.client.converter.ScalarConverter
import kotlinx.datetime.LocalDate

class DateScalarConverter : ScalarConverter<LocalDate> {
    override fun toJson(value: LocalDate): String = value.toString()

    override fun toScalar(rawValue: Any): LocalDate = LocalDate.parse(rawValue.toString())
}
