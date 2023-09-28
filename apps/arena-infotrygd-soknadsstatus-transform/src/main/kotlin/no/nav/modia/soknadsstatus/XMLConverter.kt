package no.nav.modia.soknadsstatus

import jakarta.xml.bind.DataBindingException
import jakarta.xml.bind.JAXB
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.behandling.*
import no.nav.modia.soknadsstatus.behandling.Hendelse
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.net.URL
import javax.xml.XMLConstants
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet as SoknadBehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet as SoknadBehandlingOpprettet

private const val ARENA = "AO01"
private const val INFOTRYGD = "IT01"

object XMLConverter {
    private const val SCHEMA_FIL_STATUS = "schema/behandlingsstatus.xsd"

    fun fromXml(message: String): Hendelse {
        val behandlingStatus = validateAndConvertFromXML(message)

        val primaerBehandlingRef = toPrimaerBehandlingREF(behandlingStatus)

        return when (behandlingStatus) {
            is BehandlingOpprettet -> SoknadBehandlingOpprettet(
                aktoerREF = behandlingStatus.getAktoerREF().map { AktoerREF(it.brukerIdent) },
                ansvarligEnhetREF = behandlingStatus.ansvarligEnhetREF,
                applikasjonBehandlingREF = behandlingStatus.applikasjonBehandlingREF,
                applikasjonSakREF = behandlingStatus.applikasjonSakREF,
                behandlingsID = behandlingStatus.behandlingsID,
                behandlingstema = toBehandlingstema(behandlingStatus),
                behandlingstype = toBehandlingstype(behandlingStatus),
                hendelseType = "BEHANDLING_OPPRETTET",
                hendelsesId = behandlingStatus.hendelsesId,
                hendelsesTidspunkt = toZonedDateTime(behandlingStatus.hendelsesTidspunkt),
                hendelsesprodusentREF = toHendelsesprodusentREF(behandlingStatus),
                primaerBehandlingREF = primaerBehandlingRef,
                sakstema = toSakstema(behandlingStatus),
                sekundaerBehandlingREF = toSekundaerBehandlingREF(behandlingStatus),
                styringsinformasjonListe = toStyringsinformasjonListe(behandlingStatus),
            )

            is BehandlingOpprettetOgAvsluttet -> SoknadBehandlingAvsluttet(
                aktoerREF = behandlingStatus.getAktoerREF().map { AktoerREF(it.brukerIdent) },
                ansvarligEnhetREF = behandlingStatus.ansvarligEnhetREF,
                applikasjonBehandlingREF = behandlingStatus.applikasjonBehandlingREF,
                applikasjonSakREF = behandlingStatus.applikasjonSakREF,
                behandlingsID = behandlingStatus.behandlingsID,
                behandlingstema = toBehandlingstema(behandlingStatus),
                behandlingstype = toBehandlingstype(behandlingStatus),
                hendelseType = "BEHANDLING_OPPRETTET_OG_AVSLUTTET",
                hendelsesId = behandlingStatus.hendelsesId,
                hendelsesTidspunkt = toZonedDateTime(behandlingStatus.hendelsesTidspunkt),
                hendelsesprodusentREF = toHendelsesprodusentREF(behandlingStatus),
                primaerBehandlingREF = primaerBehandlingRef,
                sakstema = toSakstema(behandlingStatus),
                sekundaerBehandlingREF = toSekundaerBehandlingREF(behandlingStatus),
                styringsinformasjonListe = toStyringsinformasjonListe(behandlingStatus),
                avslutningsstatus = toAvslutningsstatus(behandlingStatus),
                opprettelsesTidspunkt = toZonedDateTime(behandlingStatus.opprettelsesTidspunkt),

            )

            is BehandlingAvsluttet -> SoknadBehandlingAvsluttet(
                aktoerREF = behandlingStatus.getAktoerREF().map { AktoerREF(it.brukerIdent) },
                ansvarligEnhetREF = behandlingStatus.ansvarligEnhetREF,
                applikasjonBehandlingREF = behandlingStatus.applikasjonBehandlingREF,
                applikasjonSakREF = behandlingStatus.applikasjonSakREF,
                behandlingsID = behandlingStatus.behandlingsID,
                behandlingstema = toBehandlingstema(behandlingStatus),
                behandlingstype = toBehandlingstype(behandlingStatus),
                hendelseType = "BEHANDLING_AVSLUTTET",
                hendelsesId = behandlingStatus.hendelsesId,
                hendelsesTidspunkt = toZonedDateTime(behandlingStatus.hendelsesTidspunkt),
                hendelsesprodusentREF = toHendelsesprodusentREF(behandlingStatus),
                primaerBehandlingREF = primaerBehandlingRef,
                sakstema = toSakstema(behandlingStatus),
                sekundaerBehandlingREF = toSekundaerBehandlingREF(behandlingStatus),
                styringsinformasjonListe = toStyringsinformasjonListe(behandlingStatus),
                avslutningsstatus = toAvslutningsstatus(behandlingStatus),
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
                        BehandlingOpprettet::class.java,
                    )

                    "behandlingAvsluttet" -> JAXB.unmarshal(
                        reader,
                        BehandlingAvsluttet::class.java,
                    )

                    "behandlingOpprettetOgAvsluttet" -> JAXB.unmarshal(
                        reader,
                        BehandlingOpprettetOgAvsluttet::class.java,
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

    private fun toBehandlingstema(behandlingStatus: BehandlingStatus): Behandlingstema {
        val value = getArenaOrInfotrygdValue(behandlingStatus = behandlingStatus, getArenaValue = {
            ArenaTemaTypeMapper.getMappedBehandlingTema(
                behandlingStatus.sakstema.value,
                behandlingStatus.behandlingstype.value,
            )
        }, getInfoTrygdValue = {
            InfotrygdTemaTypeMapper.getMappedBehandlingTema(
                behandlingStatus.sakstema.value,
            )
        })

        return Behandlingstema(
            kodeRef = behandlingStatus.sakstema.kodeRef,
            kodeverksRef = behandlingStatus.sakstema.kodeverksRef,
            value = value,
        )
    }

    private fun toBehandlingstype(behandlingStatus: BehandlingStatus): Behandlingstype {
        val value = getArenaOrInfotrygdValue(
            behandlingStatus = behandlingStatus,
            getArenaValue = {
                ArenaTemaTypeMapper.getMappedBehandlingsType(
                    behandlingStatus.sakstema.value,
                    behandlingStatus.behandlingstype.value,
                )
            },
            getInfoTrygdValue = {
                InfotrygdTemaTypeMapper.getMappedBehandlingsType(behandlingStatus.behandlingstype.value)
            },
        )

        return Behandlingstype(
            kodeRef = behandlingStatus.behandlingstype.kodeRef,
            kodeverksRef = behandlingStatus.behandlingstype.kodeverksRef,
            value = value,
        )
    }

    private fun toZonedDateTime(xmlGregorianCalendar: XMLGregorianCalendar) =
        xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime()
            .toLocalDateTime().toKotlinLocalDateTime()

    private fun toHendelsesprodusentREF(behandlingStatus: BehandlingStatus) = HendelsesprodusentREF(
        kodeRef = behandlingStatus.hendelsesprodusentREF.kodeRef,
        kodeverksRef = behandlingStatus.hendelsesprodusentREF.kodeverksRef,
        value = behandlingStatus.hendelsesprodusentREF.value,
    )

    private fun toPrimaerBehandlingREF(behandlingStatus: BehandlingStatus) =
        behandlingStatus.primaerBehandlingREF?.let {
            PrimaerBehandlingREF(
                behandlingsREF = it.behandlingsREF,
                type = Type(
                    kodeRef = it.type?.kodeRef,
                    kodeverksRef = it.type?.kodeverksRef,
                    value = it.type.value,
                ),
            )
        }

    private fun toSakstema(behandlingStatus: BehandlingStatus): Sakstema {
        val value = getArenaOrInfotrygdValue(behandlingStatus = behandlingStatus, getArenaValue = {
            ArenaTemaTypeMapper.getMappedArkivTema(
                behandlingStatus.sakstema.value,
                behandlingStatus.behandlingstype.value,
            )
        }, getInfoTrygdValue = {
            InfotrygdTemaTypeMapper.getMappedBehandlingsType(behandlingStatus.sakstema.value)
        })
        return Sakstema(
            kodeRef = behandlingStatus.sakstema.kodeRef,
            kodeverksRef = behandlingStatus.sakstema.kodeverksRef,
            value = value,
        )
    }

    private fun toSekundaerBehandlingREF(behandlingStatus: BehandlingStatus) =
        behandlingStatus.getSekundaerBehandlingREF().map {
            SekundaerBehandlingREF(
                behandlingsREF = it.behandlingsREF,
                type = Type(kodeRef = it.type.kodeRef, kodeverksRef = it.type.kodeverksRef, value = it.type.value),
            )
        }

    private fun toStyringsinformasjonListe(behandlingStatus: BehandlingStatus) =
        behandlingStatus.getStyringsinformasjonListe().map {
            StyringsinformasjonListe(
                key = it.key,
                type = it.type,
                value = it.value,
            )
        }

    private fun toAvslutningsstatus(behandlingStatus: BehandlingAvsluttet) = Avslutningsstatus(
        kodeRef = behandlingStatus.avslutningsstatus.kodeRef,
        kodeverksRef = behandlingStatus.avslutningsstatus.kodeverksRef,
        value = behandlingStatus.avslutningsstatus.value,
    )

    private fun getArenaOrInfotrygdValue(
        behandlingStatus: BehandlingStatus,
        getArenaValue: () -> String,
        getInfoTrygdValue: () -> String,
    ): String {
        return if (behandlingStatus.hendelsesprodusentREF.isInfoTrygd()) {
            getInfoTrygdValue()
        } else if (behandlingStatus.hendelsesprodusentREF.isArena()) {
            getArenaValue()
        } else {
            throw IllegalArgumentException("Mottok ukjent produsentsystem: ${behandlingStatus.hendelsesprodusentREF.value}")
        }
    }
}

object BehandlingDeserializer {
    fun deserialize(data: String): Hendelse {
        return XMLConverter.fromXml(data)
    }
}

private fun Applikasjoner.isInfoTrygd() = this.value == INFOTRYGD
private fun Applikasjoner.isArena() = this.value == ARENA
