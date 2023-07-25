package no.nav.modia.soknadsstatus

import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.melding.virksomhet.behandlingsstatus.sobproxy.v1.sobproxybehandlingsstatus.BehandlingAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.sobproxy.v1.sobproxybehandlingsstatus.BehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.sobproxy.v1.sobproxybehandlingsstatus.BehandlingOpprettetOgAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.sobproxy.v1.sobproxybehandlingsstatus.BehandlingStatus
import no.nav.modia.soknadsstatus.behandling.*
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.net.URL
import javax.xml.XMLConstants
import javax.xml.bind.DataBindingException
import javax.xml.bind.JAXB
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet as SoknadBehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet as SoknadBehandlingOpprettet

object XMLConverter {
    private const val SCHEMA_FIL_STATUS = "schema/behandlingsstatus.xsd"

    fun fromXml(message: String): Behandling {
        val behandlingStatus = validateAndConvertFromXML(message)

        val primaerBehandlingRef = if (behandlingStatus.primaerBehandlingREF != null) {
            toPrimaerBehandlingREF(
                behandlingStatus
            )
        } else {
            null
        }

        return when (behandlingStatus) {
            is BehandlingOpprettet -> SoknadBehandlingOpprettet(
                aktoerREF = behandlingStatus.aktoerREF.map { AktoerREF(it.brukerIdent) },
                ansvarligEnhetREF = behandlingStatus.ansvarligEnhetREF,
                applikasjonBehandlingREF = behandlingStatus.applikasjonBehandlingREF,
                applikasjonSakREF = behandlingStatus.applikasjonSakREF,
                behandlingsID = behandlingStatus.behandlingsID,
                behandlingstema = toBehandlingstema(behandlingStatus),
                behandlingstype = toBehandlingstype(behandlingStatus),
                hendelseType = "",
                hendelsesId = behandlingStatus.hendelsesId,
                hendelsesTidspunkt = toHendelsesTidspunkt(behandlingStatus),
                hendelsesprodusentREF = toHendelsesprodusentREF(behandlingStatus),
                primaerBehandlingREF = primaerBehandlingRef,
                sakstema = toSakstema(behandlingStatus),
                sekundaerBehandlingREF = toSekundaerBehandlingREF(behandlingStatus),
                styringsinformasjonListe = toStyringsinformasjonListe(behandlingStatus),
            )

            is BehandlingAvsluttet -> SoknadBehandlingAvsluttet(
                aktoerREF = behandlingStatus.aktoerREF.map { AktoerREF(it.brukerIdent) },
                ansvarligEnhetREF = behandlingStatus.ansvarligEnhetREF,
                applikasjonBehandlingREF = behandlingStatus.applikasjonBehandlingREF,
                applikasjonSakREF = behandlingStatus.applikasjonSakREF,
                behandlingsID = behandlingStatus.behandlingsID,
                behandlingstema = toBehandlingstema(behandlingStatus),
                behandlingstype = toBehandlingstype(behandlingStatus),
                hendelseType = "",
                hendelsesId = behandlingStatus.hendelsesId,
                hendelsesTidspunkt = toHendelsesTidspunkt(behandlingStatus),
                hendelsesprodusentREF = toHendelsesprodusentREF(behandlingStatus),
                primaerBehandlingREF = primaerBehandlingRef,
                sakstema = toSakstema(behandlingStatus),
                sekundaerBehandlingREF = toSekundaerBehandlingREF(behandlingStatus),
                styringsinformasjonListe = toStyringsinformasjonListe(behandlingStatus),
                avslutningsstatus = toAvslutningsstatus(behandlingStatus)
            )

            else -> {
                throw RuntimeException("Meldingen inneholder ikke XML som er gyldig i henhold til XSD-en")
            }
        }
    }

    fun validateAndConvertFromXML(message: String): BehandlingStatus {
        validateXML(message)
        return convertFromXML(message)
    }

    private fun convertFromXML(message: String): BehandlingStatus {
        try {
            StringReader(message).use { reader ->
                val rootElementName = extractRootElementName(message)
                return when (rootElementName) {
                    "behandlingOpprettet" -> JAXB.unmarshal(
                        reader,
                        BehandlingOpprettet::class.java
                    )

                    "behandlingAvsluttet" -> JAXB.unmarshal(
                        reader,
                        BehandlingAvsluttet::class.java
                    )

                    "behandlingOpprettetOgAvsluttet" -> JAXB.unmarshal(
                        reader,
                        BehandlingOpprettetOgAvsluttet::class.java
                    )

                    else -> {
                        throw RuntimeException("Meldingen inneholder en ukjent meldingstype: $rootElementName")
                    }
                }
            }
        } catch (dbEx: DataBindingException) {
            throw RuntimeException("Meldingen inneholder ikke XML som er gyldig i henhold til XSD-en", dbEx)
        }
    }

    private fun validateXML(inputXml: String) {
        try {
            StringReader(inputXml).use { inputXmlReader ->
                val validator =
                    createValidator(SCHEMA_FIL_STATUS)
                validator.validate(toSource(inputXmlReader))
            }
        } catch (saxe: SAXException) {
            throw RuntimeException("Melding validerte ikke mot $SCHEMA_FIL_STATUS", saxe)
        } catch (ioe: IOException) {
            throw RuntimeException("IO-feil ved validering av xml.", ioe)
        }
    }

    private fun createValidator(xsdPath: String): Validator {
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val schema = schemaFactory.newSchema(toUrl(xsdPath))
        return schema.newValidator()
    }

    private fun toUrl(schemaFile: String): URL? {
        return javaClass.classLoader.getResource(schemaFile)
    }

    private fun toSource(inputXml: StringReader): Source {
        return StreamSource(inputXml)
    }

    private fun extractRootElementName(receivedMessageText: String): String {
        val closingTagStart = receivedMessageText.lastIndexOf("</")
        if (closingTagStart < 0) {
            throw RuntimeException("Meldingen inneholder ikke velformet XML.")
        }
        val closingTagEnd = receivedMessageText.indexOf('>', closingTagStart)
        if (closingTagEnd < 0) {
            throw RuntimeException("Meldingen inneholder ikke velformet XML.")
        }
        return receivedMessageText.substring(closingTagStart + 2, closingTagEnd).trim { it <= ' ' }
            .replace(".+:{1}".toRegex(), "")
    }

    private fun toBehandlingstema(behandlingStatus: BehandlingStatus) = Behandlingstema(
        kodeRef = behandlingStatus.sakstema.kodeRef,
        kodeverksRef = behandlingStatus.sakstema.kodeverksRef,
        value = behandlingStatus.sakstema.value
    )

    private fun toBehandlingstype(behandlingStatus: BehandlingStatus) = Behandlingstype(
        kodeRef = behandlingStatus.behandlingstype.kodeRef,
        kodeverksRef = behandlingStatus.behandlingstype.kodeverksRef,
        value = behandlingStatus.behandlingstype.value
    )

    private fun toHendelsesTidspunkt(behandlingStatus: BehandlingStatus) =
        behandlingStatus.hendelsesTidspunkt.toGregorianCalendar().toZonedDateTime()
            .toLocalDateTime().toKotlinLocalDateTime()

    private fun toHendelsesprodusentREF(behandlingStatus: BehandlingStatus) = HendelsesprodusentREF(
        kodeRef = behandlingStatus.hendelsesprodusentREF.kodeRef,
        kodeverksRef = behandlingStatus.hendelsesprodusentREF.kodeverksRef,
        value = behandlingStatus.hendelsesprodusentREF.value
    )

    private fun toPrimaerBehandlingREF(behandlingStatus: BehandlingStatus) = PrimaerBehandlingREF(
        behandlingsREF = behandlingStatus.primaerBehandlingREF?.behandlingsREF,
        type = Type(
            kodeRef = behandlingStatus.primaerBehandlingREF.type?.kodeRef,
            kodeverksRef = behandlingStatus.primaerBehandlingREF.type?.kodeverksRef,
            value = behandlingStatus.primaerBehandlingREF.type.value
        )
    )

    private fun toSakstema(behandlingStatus: BehandlingStatus) = Sakstema(
        kodeRef = behandlingStatus.sakstema.kodeRef,
        kodeverksRef = behandlingStatus.sakstema.kodeverksRef,
        value = behandlingStatus.sakstema.value
    )

    private fun toSekundaerBehandlingREF(behandlingStatus: BehandlingStatus) =
        behandlingStatus.sekundaerBehandlingREF.map {
            SekundaerBehandlingREF(
                behandlingsREF = it.behandlingsREF,
                type = Type(kodeRef = it.type.kodeRef, kodeverksRef = it.type.kodeverksRef, value = it.type.value)
            )
        }

    private fun toStyringsinformasjonListe(behandlingStatus: BehandlingStatus) =
        behandlingStatus.styringsinformasjonListe.map {
            StyringsinformasjonListe(
                key = it.key,
                type = it.type,
                value = it.value
            )
        }

    private fun toAvslutningsstatus(behandlingStatus: BehandlingAvsluttet) = Avslutningsstatus(
        kodeRef = behandlingStatus.avslutningsstatus.kodeRef,
        kodeverksRef = behandlingStatus.avslutningsstatus.kodeverksRef,
        value = behandlingStatus.avslutningsstatus.value
    )
}

object BehandlingXmlSerdes {
    val log = LoggerFactory.getLogger(BehandlingXmlSerializer::class.java)

    class BehandlingXmlSerializer : Serializer<Behandling> {
        override fun serialize(topic: String?, data: Behandling?): ByteArray {
            // We don't serialize to xml
            log.error("Prøvde å serialisere til xml. Dette er ikke støttet.")
            return ByteArray(0)
        }
    }

    class BehandlingXmlDeserializer : Deserializer<Behandling> {
        override fun deserialize(topic: String?, data: ByteArray?): Behandling? {
            if (data == null) {
                return null
            }

            val encodedString = data.toString(Charsets.UTF_8)
            return XMLConverter.fromXml(encodedString)
        }
    }

    class XMLSerde : Serde<Behandling> {
        override fun serializer() = BehandlingXmlSerializer()
        override fun deserializer() = BehandlingXmlDeserializer()
    }
}
