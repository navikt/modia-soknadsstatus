package no.nav.modia.soknadsstatus.jms

import com.ibm.mq.constants.CMQC.MQENC_NATIVE
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.common.CommonConstants
import no.nav.modia.soknadsstatus.AppMode
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.jms.pool.PooledConnectionFactory
import java.io.File
import java.io.FileNotFoundException
import javax.jms.QueueConnectionFactory
import javax.net.ssl.SSLSocketFactory

object Jms {
    data class Config(
        val host: String,
        val port: Int,
        val queueManager: String,
        val username: String,
        val password: String,
        val channel: String,
        val jmsKeyStorePath: String? = "",
        val jmsKeystorePassword: String? = "",
    )

    class SSLConfig(private val appMode: AppMode, private val jmsKeyStorePath: String?, private val jmsPassword: String?) {
        fun injectSSLConfigIfProd() {
            if (appMode.locally) {
                return
            }
            ensureKeyStoreFileExists()
            injectKeyValuePair("javax.net.ssl.keyStoreType", "jks")
            injectKeyValuePair("javax.net.ssl.keyStore", requireNotNull(jmsKeyStorePath))
            injectKeyValuePair("javax.net.ssl.keyStorePassword", requireNotNull(jmsPassword))
        }

        private fun ensureKeyStoreFileExists() {
            val file = File(requireNotNull(jmsKeyStorePath))
            if (!file.exists() || file.isDirectory) {
                throw FileNotFoundException("The keystore file is required when running in production")
            }
        }

        private fun injectKeyValuePair(key: String, value: String) = System.setProperty(key, value)
    }

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
            channel = config.channel
            ccsid = 1208
            setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
            setIntProperty(JmsConstants.JMS_IBM_ENCODING, MQENC_NATIVE)
            setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, 1208)
            sslSocketFactory = SSLSocketFactory.getDefault()
            sslCipherSuite = "*TLS13ORHIGHER"
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
