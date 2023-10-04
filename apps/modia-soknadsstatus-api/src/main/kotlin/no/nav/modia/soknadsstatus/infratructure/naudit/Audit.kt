package no.nav.modia.soknadsstatus.infratructure.naudit

import io.ktor.server.auth.*
import net.logstash.logback.marker.Markers
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier.DENY_REASON
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier.FAIL_REASON
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.Logging

val cefLogger =
    ArchSightCEFLogger(
        CEFLoggerConfig(
            applicationName = "modia",
            logName = "soknadsstatus",
            filter = { (action: Audit.Action) ->
                action != Audit.Action.READ
            },
        ),
    )

class Audit {
    open class AuditResource(
        val resource: String,
    )

    enum class Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
    }

    interface AuditDescriptor<T> {
        fun log(resource: T)

        fun denied(reason: String)

        fun failed(exception: Throwable)

        fun Throwable.getFailureReason(): String = this.message ?: this.toString()
    }

    internal class WithDataDescriptor<T>(
        private val action: Action,
        private val resourceType: AuditResource,
        private val authContext: AuthenticationContext,
        private val extractIdentifiers: (T?) -> List<Pair<AuditIdentifier, String?>>,
    ) : AuditDescriptor<T> {
        override fun log(resource: T) {
            val identifiers = extractIdentifiers(resource).toTypedArray()
            logInternal(action, resourceType, identifiers, authContext)
        }

        override fun denied(reason: String) {
            val identifiers = extractIdentifiers(null).toTypedArray().plus(DENY_REASON to reason)
            logInternal(action, resourceType, identifiers, authContext)
        }

        override fun failed(exception: Throwable) {
            val identifiers = extractIdentifiers(null).toTypedArray().plus(FAIL_REASON to exception.getFailureReason())
            logInternal(action, resourceType, identifiers, authContext)
        }
    }

    internal class NoopDescriptor<T> : AuditDescriptor<T> {
        override fun log(resource: T) {}

        override fun denied(reason: String) {}

        override fun failed(exception: Throwable) {}
    }

    internal class NothingDescriptor(
        private val action: Action,
        private val resourceType: AuditResource,
        private val authContext: AuthenticationContext,
        private val identifiers: Array<Pair<AuditIdentifier, String?>>,
    ) : AuditDescriptor<Any> {
        override fun log(resource: Any) {
            logInternal(action, resourceType, identifiers, authContext)
        }

        override fun denied(reason: String) {
            logInternal(action, resourceType, identifiers.plus(DENY_REASON to reason), authContext)
        }

        override fun failed(exception: Throwable) {
            logInternal(action, resourceType, identifiers.plus(FAIL_REASON to exception.getFailureReason()), authContext)
        }
    }

    companion object {
        val skipAuditLog: AuditDescriptor<Any> = NoopDescriptor()

        @JvmStatic
        fun <T> skipAuditLog(): AuditDescriptor<T> = NoopDescriptor()

        @JvmStatic
        fun describe(
            authContext: AuthenticationContext,
            action: Action,
            resourceType: AuditResource,
            vararg identifiers: Pair<AuditIdentifier, String?>,
        ): AuditDescriptor<Any> = NothingDescriptor(action, resourceType, authContext, identifiers as Array<Pair<AuditIdentifier, String?>>)

        @JvmStatic
        fun <T> describe(
            authContext: AuthenticationContext,
            action: Action,
            resourceType: AuditResource,
            extractIdentifiers: (T?) -> List<Pair<AuditIdentifier, String?>>,
        ): AuditDescriptor<T> = WithDataDescriptor(action, resourceType, authContext, extractIdentifiers)

        private val auditMarker = Markers.appendEntries(mapOf(Logging.LOGTYPE_KEY to "audit"))

        private fun logInternal(
            action: Action,
            resourceType: AuditResource,
            identifiers: Array<Pair<AuditIdentifier, String?>>,
            authContext: AuthenticationContext,
        ) {
            val subject = authContext.principal<Security.SubjectPrincipal>()?.subject
            val logline =
                listOfNotNull(
                    "action='$action'",
                    subject?.let { "subject='$it'" },
                    "resource='${resourceType.resource}'",
                    *identifiers
                        .map { "${it.first}='${it.second ?: "-"}'" }
                        .toTypedArray(),
                ).joinToString(" ")

            Logging.secureLog.info(auditMarker, logline)
            cefLogger.log(CEFEvent(action, resourceType, subject ?: "-", identifiers))
        }
    }
}
