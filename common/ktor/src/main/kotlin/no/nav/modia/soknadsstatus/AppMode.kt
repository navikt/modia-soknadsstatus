package no.nav.modia.soknadsstatus

enum class AppMode(val locally: Boolean) {
    LOCALLY_WITHIN_DOCKER(locally = true),
    LOCALLY_WITHIN_IDEA(locally = true),
    NAIS(locally = false),
    ;

    companion object {
        operator fun invoke(appMode: String?): AppMode {
            return when (appMode) {
                null -> NAIS
                else -> AppMode.valueOf(appMode)
            }
        }
    }
}
