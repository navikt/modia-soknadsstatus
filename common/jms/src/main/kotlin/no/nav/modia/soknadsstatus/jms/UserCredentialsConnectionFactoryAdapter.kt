package no.nav.modia.soknadsstatus.jms

import javax.jms.*

class UserCredentialsConnectionFactoryAdapter(
    val connectionFactory: ConnectionFactory,
    val username: String,
    val password: String,
) : ConnectionFactory,
    QueueConnectionFactory,
    TopicConnectionFactory {
    override fun createConnection(): Connection = doCreateConnection()

    override fun createConnection(
        userName: String?,
        password: String?,
    ): Connection = doCreateConnection()

    override fun createContext(): JMSContext = connectionFactory.createContext()

    override fun createContext(
        userName: String?,
        password: String?,
    ): JMSContext = connectionFactory.createContext(username, password)

    override fun createContext(
        userName: String?,
        password: String?,
        sessionMode: Int,
    ): JMSContext =
        connectionFactory.createContext(
            userName,
            password,
            sessionMode,
        )

    override fun createContext(sessionMode: Int): JMSContext = connectionFactory.createContext(sessionMode)

    override fun createTopicConnection(): TopicConnection = doCreateTopicConnection()

    override fun createTopicConnection(
        userName: String?,
        password: String?,
    ): TopicConnection = doCreateTopicConnection()

    override fun createQueueConnection(): QueueConnection = doCreateQueueConnection()

    override fun createQueueConnection(
        userName: String?,
        password: String?,
    ): QueueConnection = doCreateQueueConnection()

    private fun doCreateConnection(): Connection = connectionFactory.createConnection(username, password)

    private fun doCreateQueueConnection(): QueueConnection =
        when (connectionFactory) {
            is QueueConnectionFactory -> connectionFactory.createQueueConnection(username, password)
            else -> error("target ConnectionFactory is not a QueueConnectionFactory")
        }

    private fun doCreateTopicConnection(): TopicConnection =
        when (connectionFactory) {
            is TopicConnectionFactory -> connectionFactory.createTopicConnection(username, password)
            else -> error("target ConnectionFactory is not a TopicConnectionFactory")
        }
}
