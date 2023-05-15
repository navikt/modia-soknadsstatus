package no.nav.modia.soknadsstatus

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import no.nav.personoversikt.common.logging.Logging
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
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

object XMLConverter {
    private const val SCHEMA_FIL_STATUS = "hendelseshandtererBehandlingsstatus.xsd"

    fun fromXml(message: String): Hendelse {
        return validateAndConvertFromXML(message)
    }

    fun validateAndConvertFromXML(message: String): Hendelse {
        validateXML(message)
        return convertFromXML(message)
    }

    private fun convertFromXML(message: String): Hendelse {
        try {
            StringReader(message).use { reader ->
                val rootElementName = extractRootElementName(message)
                return when (rootElementName) {
                    "behandlingsstegstatus" -> JAXB.unmarshal(
                        reader,
                        Behandlingsstegstatus::class.java
                    )

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
}

object BehandlingXmlSerdes {
    class BehandlingXmlSerializer : Serializer<Hendelse> {
        override fun serialize(topic: String?, data: Hendelse?): ByteArray {
            // We don't serialize to xml
            Logging.secureLog.error("Prøvde å serialisere til xml. Dette er ikke støttet.")
            return ByteArray(0)
        }
    }

    class BehandlingXmlDeserializer : Deserializer<Hendelse> {
        override fun deserialize(topic: String?, data: ByteArray?): Hendelse? {
            if (data == null) {
                return null
            }

            val encodedString = data.toString(Charsets.UTF_8)
            return XMLConverter.fromXml(encodedString)
        }
    }

    class XMLSerde : Serde<Hendelse> {
        override fun serializer() = BehandlingXmlSerializer()
        override fun deserializer() = BehandlingXmlDeserializer()
    }
}
