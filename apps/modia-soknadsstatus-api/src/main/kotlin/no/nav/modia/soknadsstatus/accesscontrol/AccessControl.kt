package no.nav.modia.soknadsstatus.accesscontrol

import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.modia.soknadsstatus.infratructure.naudit.Audit
import no.nav.personoversikt.common.kabac.AttributeValue
import no.nav.personoversikt.common.kabac.CombiningAlgorithm
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.logging.TjenestekallLogg
import no.nav.utils.getCallId

interface AccessControl {
    fun check(policy: PolicyWithAttributes): AccessControlInstance
}

interface AccessControlInstance : AccessControl {
    fun <S> get(
        audit: Audit.AuditDescriptor<in S>,
        block: () -> S,
    ): S

    fun getDecision(): Decision
}
private typealias NoAccessHandler = (String) -> java.lang.RuntimeException

data class PolicyWithAttributes(
    val policy: Kabac.Policy,
    val attributes: List<AttributeValue<*>>,
)

private class Instance(
    private val enforcementPoint: Kabac.PolicyEnforcementPoint,
    private val noAccessHandler: NoAccessHandler,
) : AccessControlInstance {
    private val policies = mutableListOf<PolicyWithAttributes>()
    private var combiningAlgorithm = CombiningAlgorithm.denyOverride
    private var bias = enforcementPoint.bias

    override fun check(policy: PolicyWithAttributes): AccessControlInstance {
        this.policies.add(policy)
        return this
    }

    override fun <S> get(
        audit: Audit.AuditDescriptor<in S>,
        block: () -> S,
    ): S =
        when (val decision = getDecision()) {
            is Decision.Permit ->
                runCatching(block)
                    .onSuccess(audit::log)
                    .onFailure(audit::failed)
                    .getOrThrow()
            is Decision.Deny -> {
                audit.denied(decision.message)
                throw noAccessHandler(decision.message)
            }
            is Decision.NotApplicable -> {
                throw noAccessHandler(decision.message ?: "No applicable policy found")
            }
        }

    override fun getDecision(): Decision {
        val attributes =
            policies
                .flatMap { it.attributes }
                .distinctBy { it.key }
        val ctx = enforcementPoint.createEvaluationContext(attributes)
        val policy = combiningAlgorithm.combine(policies.map { it.policy })

        val (decision, report) =
            enforcementPoint.evaluatePolicyWithContextWithReport(
                bias = bias,
                ctx = ctx,
                policy = policy,
            )
        TjenestekallLogg.info(TjenestekallLogg.format("policy-report: ${getCallId()}", report), fields = mapOf())

        return decision
    }
}

open class AccessControlKabac(
    private val enforcementPoint: Kabac.PolicyEnforcementPoint,
    private val noAccessHandler: NoAccessHandler,
) : AccessControl {
    override fun check(policy: PolicyWithAttributes): AccessControlInstance = Instance(enforcementPoint, noAccessHandler).check(policy)
}
