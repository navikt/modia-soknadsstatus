package no.nav.modia.soknadsstatus.jms

import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.QueueConnectionFactory
import jakarta.jms.Session
import no.nav.modia.soknadsstatus.AppMode
import org.slf4j.LoggerFactory

class JmsConsumer(
    private val config: Jms.Config,
    appMode: AppMode,
) {
    val logger = LoggerFactory.getLogger(JmsConsumer::class.java)
    private val connectionFactory: QueueConnectionFactory by lazy {
        Jms.createConnectionFactory(config, appMode)
    }

    fun subscribe(
        queueName: String,
        onMessage: (message: Message) -> Unit,
    ) {
        val connection = connectionFactory.createQueueConnection()
        val session = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)
        val queue = session.createQueue(queueName)
        val receiver = session.createReceiver(queue)

        receiver.messageListener =
            MessageListener { message ->
                try {
                    onMessage(message)
                } catch (e: Exception) {
                    logger.error("Failed when handling message", e)
                }
            }

        connection.start()
    }
}
