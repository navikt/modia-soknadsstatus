package no.nav.modia.soknadsstatus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InfotrygdAvslutningsstatusMapperTest {
    @Test
    fun `test at infotrygd mapping status filen blir lest`() {
        val status = InfotrygdAvslutningsstatusMapper.getAvslutningsstatus("T")
        assertEquals(status, SoknadsstatusDomain.Status.FERDIG_BEHANDLET)
    }
}
