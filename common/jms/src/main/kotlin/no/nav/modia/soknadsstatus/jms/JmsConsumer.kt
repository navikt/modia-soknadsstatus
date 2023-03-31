package no.nav.modia.soknadsstatus.jms

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.QueueConnectionFactory
import javax.jms.Session

class JmsConsumer(private val config: Jms.Config) {
    private val connectionFactory: QueueConnectionFactory by lazy {
        Jms.createConnectionFactory(config)
    }

    suspend fun subscribe(queueName: String): Flow<Message> {
        val connection = connectionFactory.createQueueConnection()
        val session = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)
        val receiver = session.createReceiver { queueName }
        val channel = Channel<Message>()

        receiver.messageListener = MessageListener { message ->
            runBlocking {
                channel.send(message)
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
