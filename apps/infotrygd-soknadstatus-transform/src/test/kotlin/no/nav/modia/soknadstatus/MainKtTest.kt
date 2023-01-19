package no.nav.modia.soknadstatus

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MainTest {
    @Language("xml")
    val xml = """
        <ns2:behandlingOpprettetOgAvsluttet xmlns:ns2="http://nav.no/melding/virksomhet/behandlingsstatus/hendelsehandterer/v1/hendelseshandtererBehandlingsstatus">
            <hendelsesId>5460</hendelsesId>
            <hendelsesprodusentRef>IT01</hendelsesprodusentRef>
            <hendelsesTidspunkt>2015-12-08T00:00:00.000</hendelsesTidspunkt>
            <behandlingsID>17100047L</behandlingsID>
            <behandlingstype>ae0019</behandlingstype>
            <sakstema>SYK</sakstema>
            <aktoerREF>
                <aktoerId>10000098546164</aktoerId>
            </aktoerREF>
            <ansvarligEnhetRF>1202</ansvarligEnhetRF>
            <applikasjonsSakREF>A1231311231-102-20</applikasjonsSakREF>
            <avslutningsstatus>innvilget</avslutningsstatus>
            <opprettelsesTidspunkt>2015-07-03T00:00:00.000</opprettelsesTidspunkt>
        </ns2:behandlingOpprettetOgAvsluttet>
    """.trimIndent()

    @Test
    fun `should be able to get type`() {
        val document = deserialize(null, xml)
        val root = ETL.rootNode(document)

        println(root)

        val oppdatering = transform(null, document)

        println(oppdatering)
    }
}
