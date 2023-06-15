package no.nav.modia.soknadsstatus.jms

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadsstatus.AppMode
import org.slf4j.LoggerFactory
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.QueueConnectionFactory
import javax.jms.Session

class JmsConsumer(private val config: Jms.Config, appMode: AppMode) {
    val logger = LoggerFactory.getLogger(JmsConsumer::class.java)
    private val connectionFactory: QueueConnectionFactory by lazy {
        Jms.createConnectionFactory(config, appMode)
    }

    suspend fun subscribe(queueName: String): Flow<Message> {
        val connection = connectionFactory.createQueueConnection()
        val session = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)
        val queue = session.createQueue(queueName)
        val receiver = session.createReceiver(queue)
        val channel = Channel<Message>()


        receiver.messageListener = MessageListener { message ->
            runBlocking {
                try {
                    channel.send(message)
                } catch (e: Exception) {
                     logger.error("Failed when parsing message", e)
                }
            }
        }

        connection.start()

        val flow = channel
            .consumeAsFlow()
            .cancellable()
            .onCompletion {
                receiver.close()
                session.close()
                connection.close()
            }

        return flow
    }
}
