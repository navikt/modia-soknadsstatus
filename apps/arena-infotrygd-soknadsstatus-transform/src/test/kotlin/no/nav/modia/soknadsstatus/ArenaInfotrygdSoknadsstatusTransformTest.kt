package no.nav.modia.soknadsstatus

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArenaInfotrygdSoknadsstatusTransformTest {
    @Language("xml")
    val behandlingOpprettet = """
        <?xml version="1.0" encoding="ISO-8859-1" standalone="no"?><informasjon:behandlingOpprettet xmlns:informasjon="http://nav.no/melding/virksomhet/behandlingsstatus/sobproxy/v1/sobproxyBehandlingsstatus" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><hendelsesId>50090284</hendelsesId><hendelsesprodusentREF>AO01</hendelsesprodusentREF><hendelsesTidspunkt>2023-06-16T11:40:24</hendelsesTidspunkt><behandlingsID>1500oVFWH</behandlingsID><behandlingstype>O</behandlingstype><sakstema>DAGP;DAGP;FETI</sakstema><aktoerREF xsi:type="informasjon:Person"><brukerIdent>19099531196</brukerIdent></aktoerREF><ansvarligEnhetREF>9958</ansvarligEnhetREF><applikasjonSakREF>202233397</applikasjonSakREF></informasjon:behandlingOpprettet>
    """.trimIndent()

    val behandlingAvsluttet = """
        <?xml version="1.0" encoding="ISO-8859-1" standalone="no"?><informasjon:behandlingAvsluttet xmlns:informasjon="http://nav.no/melding/virksomhet/behandlingsstatus/sobproxy/v1/sobproxyBehandlingsstatus" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><hendelsesId>50090289</hendelsesId><hendelsesprodusentREF>AO01</hendelsesprodusentREF><hendelsesTidspunkt>2023-06-16T11:40:24</hendelsesTidspunkt><behandlingsID>1500oVFWi</behandlingsID><behandlingstype>O</behandlingstype><sakstema>DAGP;DAGP;FETI</sakstema><aktoerREF xsi:type="informasjon:Person"><brukerIdent>26127338824</brukerIdent></aktoerREF><ansvarligEnhetREF>9958</ansvarligEnhetREF><applikasjonSakREF>2021240702</applikasjonSakREF><avslutningsstatus>OK</avslutningsstatus></informasjon:behandlingAvsluttet>
    """.trimIndent()

    val behandlingOpprettetOgAvsluttetXml = """
        <?xml version="1.0" encoding="ISO-8859-1" standalone="no"?><informasjon:behandlingOpprettetOgAvsluttet xmlns:informasjon="http://nav.no/melding/virksomhet/behandlingsstatus/sobproxy/v1/sobproxyBehandlingsstatus" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><hendelsesId>50090289</hendelsesId><hendelsesprodusentREF>AO01</hendelsesprodusentREF><hendelsesTidspunkt>2023-06-16T11:40:24</hendelsesTidspunkt><behandlingsID>1500oVFWi</behandlingsID><behandlingstype>O</behandlingstype><sakstema>DAGP;DAGP;FETI</sakstema><aktoerREF xsi:type="informasjon:Person"><brukerIdent>26127338824</brukerIdent></aktoerREF><ansvarligEnhetREF>9958</ansvarligEnhetREF><applikasjonSakREF>2021240702</applikasjonSakREF><avslutningsstatus>OK</avslutningsstatus><opprettelsesTidspunkt>2023-06-16T11:40:24</opprettelsesTidspunkt></informasjon:behandlingOpprettetOgAvsluttet>
    """.trimIndent()

    @Test
    fun `should be able to get behandlingOpprettet sokand`() {
        val behandling = XMLConverter.fromXml(behandlingOpprettet)

        val oppdatering = transform(null, behandling)

        assertEquals("19099531196", oppdatering.identer?.first()?.ident)
        assertEquals(oppdatering.behandlingsId, "1500oVFWH")
    }

    @Test
    fun `should be able to get behandlingAvsluttet sokand`() {
        val behandling = XMLConverter.fromXml(behandlingAvsluttet)

        val oppdatering = transform(null, behandling)

        assertEquals("26127338824", oppdatering.identer?.first()?.ident)
        assertEquals("1500oVFWi", oppdatering.behandlingsId)
    }

    @Test
    fun `should be able to get behandlingOpprettetOgAvsluttetXml sokand `() {
        val behandling = XMLConverter.fromXml(behandlingOpprettetOgAvsluttetXml)

        val oppdatering = transform(null, behandling)

        assertEquals("26127338824", oppdatering.identer?.first()?.ident)
        assertEquals("1500oVFWi", oppdatering.behandlingsId)
    }
}
