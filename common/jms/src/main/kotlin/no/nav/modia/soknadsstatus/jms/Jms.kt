package no.nav.modia.soknadsstatus.jms

import com.ibm.mq.constants.CMQC.MQENC_NATIVE
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.common.CommonConstants
import no.nav.modia.soknadsstatus.AppMode
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.jms.pool.PooledConnectionFactory
import javax.jms.QueueConnectionFactory

object Jms {
    data class Config(
        val host: String,
        val port: Int,
        val queueManager: String,
        val username: String,
        val password: String,
    )

    private var connectionFactory: QueueConnectionFactory? = null

    fun createConnectionFactory(config: Config, appMode: AppMode): QueueConnectionFactory {
        if (connectionFactory == null) {
            connectionFactory = PooledConnectionFactory().apply {
                maxConnections = 10
                maximumActiveSessionPerConnection = 10
                connectionFactory = UserCredentialsConnectionFactoryAdapter(
                    username = config.username,
                    password = config.password,
                    connectionFactory = if (appMode == AppMode.NAIS) createIBMConnectionFactory(config) else createActiveMQConnectionFactory(
                        config
                    )
                )
            }
        }

        return checkNotNull(connectionFactory)
    }

    private fun createIBMConnectionFactory(config: Config): QueueConnectionFactory {
        return MQQueueConnectionFactory().apply {
            hostName = config.host
            port = config.port
            queueManager = config.queueManager
            transportType = CommonConstants.WMQ_CM_CLIENT
            ccsid = 1208
            setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
            setIntProperty(JmsConstants.JMS_IBM_ENCODING, MQENC_NATIVE)
            setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, 1208)
        }
    }

    private fun createActiveMQConnectionFactory(config: Config): QueueConnectionFactory {
        return ActiveMQConnectionFactory(
            config.username,
            config.password,
            "nio://${config.host}:${config.port}"
        )
    }
}
