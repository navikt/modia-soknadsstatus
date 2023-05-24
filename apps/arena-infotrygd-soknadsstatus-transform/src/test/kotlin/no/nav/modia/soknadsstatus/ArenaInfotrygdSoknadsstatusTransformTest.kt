package no.nav.modia.soknadsstatus

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ArenaInfotrygdSoknadsstatusTransformTest {
    @Language("xml")
    val behandlingOpprettet = """
        <v2:behandlingOpprettet xmlns:v2="http://nav.no/melding/virksomhet/behandlingsstatus/hendelsehandterer/v1/hendelseshandtererBehandlingsstatus">
            <hendelsesId>HEN-1</hendelsesId>
            <hendelsesprodusentREF kodeverksRef="http://nav.no/kodeverk/Kodeverk/ApplikasjonIDer">HEN-1</hendelsesprodusentREF>
            <hendelsesTidspunkt>2004-02-14T19:44:14</hendelsesTidspunkt>
            <behandlingsID>BEH-1</behandlingsID>
            <behandlingstype kodeverksRef="http://nav.no/kodeverk/Kodeverk/Behandlingstyper">BEHT-1</behandlingstype>
            <sakstema kodeverksRef="http://nav.no/kodeverk/Kodeverk/Sakstemaer">TEMA-1</sakstema>
            <aktoerREF><aktoerId>AKTOR-1</aktoerId></aktoerREF>
            <primaerBehandlingREF><behandlingsREF>BREF</behandlingsREF><type>forrige</type></primaerBehandlingREF>
            <applikasjonBehandlingREF>test</applikasjonBehandlingREF>
            </v2:behandlingOpprettet>
    """.trimIndent()

    val behandlingAvsluttet = """
        <v2:behandlingAvsluttet xmlns:v2="http://nav.no/melding/virksomhet/behandlingsstatus/hendelsehandterer/v1/hendelseshandtererBehandlingsstatus">
            <hendelsesId>HEN-1</hendelsesId>
            <hendelsesprodusentREF kodeverksRef="http://nav.no/kodeverk/Kodeverk/ApplikasjonIDer">HEN-1</hendelsesprodusentREF>
            <hendelsesTidspunkt>2004-02-14T19:44:14</hendelsesTidspunkt>
            <behandlingsID>BEH-1</behandlingsID>
            <behandlingstype kodeverksRef="http://nav.no/kodeverk/Kodeverk/Behandlingstyper">BEHT-1</behandlingstype>
            <sakstema kodeverksRef="http://nav.no/kodeverk/Kodeverk/Sakstemaer">TEMA-1</sakstema>
            <aktoerREF><aktoerId>AKTOR-1</aktoerId></aktoerREF>
            <primaerBehandlingREF><behandlingsREF>BREF</behandlingsREF><type>forrige</type></primaerBehandlingREF>
            <applikasjonBehandlingREF>test</applikasjonBehandlingREF>
            <avslutningsstatus kodeverksRef="http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser">AVSLUTTET</avslutningsstatus>
            </v2:behandlingAvsluttet>
    """.trimIndent()

    val behandlingOpprettetOgAvsluttetXml = """
        <v2:behandlingOpprettetOgAvsluttet xmlns:v2="http://nav.no/melding/virksomhet/behandlingsstatus/hendelsehandterer/v1/hendelseshandtererBehandlingsstatus">
            <hendelsesId>HEN-1</hendelsesId>
            <hendelsesprodusentREF kodeverksRef="http://nav.no/kodeverk/Kodeverk/ApplikasjonIDer">HEN-1</hendelsesprodusentREF>
            <hendelsesTidspunkt>2004-02-14T19:44:14</hendelsesTidspunkt>
            <behandlingsID>BEH-1</behandlingsID>
            <behandlingstype kodeverksRef="http://nav.no/kodeverk/Kodeverk/Behandlingstyper">BEHT-1</behandlingstype>
            <sakstema kodeverksRef="http://nav.no/kodeverk/Kodeverk/Sakstemaer">TEMA-1</sakstema>
            <aktoerREF><aktoerId>AKTOR-1</aktoerId></aktoerREF>
            <primaerBehandlingREF><behandlingsREF>BREF</behandlingsREF><type>forrige</type></primaerBehandlingREF>
            <applikasjonBehandlingREF>test</applikasjonBehandlingREF>
            <avslutningsstatus kodeverksRef="http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser">AVSLUTTET</avslutningsstatus>
            <opprettelsesTidspunkt>2004-02-24T12:14:05</opprettelsesTidspunkt>
            </v2:behandlingOpprettetOgAvsluttet>
    """.trimIndent()

    @Test
    fun `should be able to get behandlingOpprettet sokand`() {
        val behandling = XMLConverter.fromXml(behandlingOpprettet)

        val oppdatering = transform(null, behandling)

        println(oppdatering)
    }

    @Test
    fun `should be able to get behandlingAvsluttet sokand`() {
        val behandling = XMLConverter.fromXml(behandlingAvsluttet)

        val oppdatering = transform(null, behandling)

        println(oppdatering)
    }

    @Test
    fun `should be able to get behandlingOpprettetOgAvsluttetXml sokand `() {
        val behandling = XMLConverter.fromXml(behandlingAvsluttet)

        val oppdatering = transform(null, behandling)

        println(oppdatering)
    }
}
