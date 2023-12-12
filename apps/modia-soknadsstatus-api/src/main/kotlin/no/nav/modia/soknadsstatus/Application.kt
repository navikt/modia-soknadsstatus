package no.nav.modia.soknadsstatus

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.modia.soknadsstatus.hendelseconsumer.HendelseConsumer
import no.nav.modia.soknadsstatus.hendelseconsumer.HendelseConsumerPlugin
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.slf4j.event.Level

fun Application.soknadsstatusModule(
    env: Env = Env(),
    configuration: Configuration = Configuration.factory(env),
    services: Services = Services.factory(env, configuration),
) {
    val security =
        Security(
            listOfNotNull(
                configuration.authProviderConfig,
            ),
        )

    install(CORS) {
        anyHost() // TODO kanskje kun whiteliste personoversikt domenene i første omgang?
        allowMethod(HttpMethod.Get)
    }

    install(BaseNaisApp)

    install(Authentication) {
        if (env.kafkaApp.appMode == AppMode.NAIS) {
            security.setupJWT(this)
        } else {
            security.setupMock(this, "Z999999", "f24261ea-60ed-4894-a2d6-29e55214df08")
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/modia-soknadsstatus/api") }
        mdc("userId") { security.getSubject(it).joinToString(";") }
    }

    install(HendelseConsumerPlugin()) {
        hendelseConsumer =
            HendelseConsumer(
                sendToDeadLetterQueueExceptionHandler =
                    SendToDeadLetterQueueExceptionHandler(
                        requireNotNull(env.kafkaApp.deadLetterQueueTopic),
                        services.dlqProducer,
                    ),
                topic = requireNotNull(env.kafkaApp.sourceTopic),
                kafkaConsumer =
                    KafkaUtils.createConsumer(
                        env.kafkaApp,
                        consumerGroup = "${env.kafkaApp.appName}-hendelse-consumer",
                        autoCommit = true,
                        pollRecords = 10,
                    ),
                pollDurationMs = env.hendelseConsumerEnv.pollDurationMs,
                exceptionRestartDelayMs = env.hendelseConsumerEnv.exceptionRestartDelayMs,
            ) { _, _, value ->
                runCatching {
                    val decodedValue =
                        Encoding.decode(InnkommendeHendelse.serializer(), value)
                    services.hendelseService.onNewHendelse(decodedValue)
                }
            }
    }

    install(DeadLetterQueueConsumerPlugin()) {
        deadLetterQueueConsumer =
            DeadLetterQueueConsumer(
                topic = requireNotNull(env.kafkaApp.deadLetterQueueTopic),
                kafkaConsumer =
                    KafkaUtils.createConsumer(
                        env.kafkaApp,
                        consumerGroup = "${env.kafkaApp.appName}-dlq-consumer",
                    ),
                pollDurationMs = env.kafkaApp.deadLetterQueueConsumerPollIntervalMs,
                exceptionRestartDelayMs = env.kafkaApp.deadLetterQueueExceptionRestartDelayMs,
                deadLetterMessageSkipService = services.dlSkipService,
            ) { _, key, value ->
                runCatching {
                    try {
                        val decodedValue =
                            Encoding.decode(InnkommendeHendelse.serializer(), value)
                        services.hendelseService.onNewHendelse(decodedValue)
                    } catch (e: Exception) {
                        TjenestekallLogg.error(
                            "Klarte ikke å håndtere DL",
                            fields = mapOf("key" to key, "value" to value),
                            throwable = e,
                        )
                        throw e
                    }
                }
            }
    }
    installRouting(security, services)
}
