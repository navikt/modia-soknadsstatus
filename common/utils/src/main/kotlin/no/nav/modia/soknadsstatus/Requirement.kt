package no.nav.modia.soknadsstatus

object Requirement {
    fun requireNoneOrAll(vararg requirements: Any?) {
        var haveARequirement = false
        for (requirement in requirements) {
            if (requirement != null) {
                haveARequirement = true
            } else if (haveARequirement) {
                throw IllegalArgumentException("Requirement was null. Either none or all of the requirements should be provided.")
            }
        }
    }
}
