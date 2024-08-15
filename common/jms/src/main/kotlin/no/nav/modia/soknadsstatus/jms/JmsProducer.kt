package no.nav.modia.soknadsstatus.jms

import jakarta.jms.DeliveryMode
import jakarta.jms.QueueConnection
import jakarta.jms.QueueConnectionFactory
import jakarta.jms.QueueSender
import jakarta.jms.QueueSession
import jakarta.jms.Session
import no.nav.modia.soknadsstatus.AppMode

class JmsProducer(
    private val config: Jms.Config,
    appMode: AppMode,
) : AutoCloseable {
    private val connectionFactory: QueueConnectionFactory by lazy {
        Jms.createConnectionFactory(config, appMode)
    }
    private val connection: QueueConnection by lazy { connectionFactory.createQueueConnection() }
    private val session: QueueSession by lazy { connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE) }
    private val senderMap = mutableMapOf<String, QueueSender>()

    fun send(
        queueName: String,
        payload: String,
    ) {
        val sender =
            senderMap.computeIfAbsent(queueName) {
                session
                    .createSender { queueName }
                    .also {
                        it.deliveryMode = DeliveryMode.NON_PERSISTENT
                    }
            }
        sender.send(session.createTextMessage(payload))
    }

    override fun close() {
        senderMap.values.forEach { sender -> sender.close() }
        session.close()
        connection.close()
    }
}
