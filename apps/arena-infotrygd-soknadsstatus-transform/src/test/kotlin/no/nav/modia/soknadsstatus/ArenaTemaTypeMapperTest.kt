package no.nav.modia.soknadsstatus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArenaTemaTypeMapperTest {
    @Test
    fun `test at arnena mapping fil blir lest`() {
        val arkivTema = ArenaTemaTypeMapper.getMappedArkivTema("AA;AA;AA115", "G")
        val behandlignsTema = ArenaTemaTypeMapper.getMappedBehandlingTema("AA;AA;AA115", "G")
        val behandlingsType = ArenaTemaTypeMapper.getMappedBehandlingsType("AA;AA;AA115", "G")
        assertEquals("AAP", arkivTema)
        assertEquals("ab0075", behandlignsTema)
        assertEquals("ae0047", behandlingsType)
    }
}
