package no.nav.modia.soknadsstatus

import org.w3c.dom.Document
import org.w3c.dom.Node
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

object ETLUtils {
    private val xpath = XPathFactory.newInstance().newXPath()
    private val rootNode = xpath.compile("/*")

    fun Document.getAsTextContent(path: XPathExpression): String = (path.evaluate(this, XPathConstants.NODE) as Node).textContent
    fun Document.getNodeName(path: XPathExpression): String = (path.evaluate(this, XPathConstants.NODE) as Node).nodeName.removeNamespace()

    private fun String.removeNamespace(): String {
        val namespaceSeparator = this.indexOf(":")
        if (namespaceSeparator > -1) {
            return this.substring(namespaceSeparator + 1)
        }
        return this
    }
}
