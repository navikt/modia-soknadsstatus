package no.nav.modia.soknadstatus

import no.nav.modia.soknadstatus.ETLUtils.getAsTextContent
import no.nav.modia.soknadstatus.ETLUtils.getNodeName
import org.w3c.dom.Document
import javax.xml.xpath.XPathFactory

object ETL {
    private val xpath = XPathFactory.newInstance().newXPath()
    private val rootNode = xpath.compile("/*")
    private val sakstema = xpath.compile("/*/sakstema")
    private val behandlingsId = xpath.compile("/*/behandlingsID")
    private val behandlingstype = xpath.compile("/*/behandlingstype")
    private val aktoerId = xpath.compile("/*/aktoerREF/aktoerId")
    private val tidspunkt = xpath.compile("/*/hendelsesTidspunkt")
    private val status = xpath.compile("/*/avslutningsstatus")

    fun rootNode(document: Document): String = document.getNodeName(rootNode)
    fun sakstema(document: Document): String = document.getAsTextContent(sakstema)
    fun behandlingsId(document: Document): String = document.getAsTextContent(behandlingsId)
    fun behandlingstype(document: Document): String = document.getAsTextContent(behandlingstype)
    fun aktoerId(document: Document): String = document.getAsTextContent(aktoerId)
    fun tidspunkt(document: Document): String = document.getAsTextContent(tidspunkt)
    fun status(document: Document): String = document.getAsTextContent(status)
}
