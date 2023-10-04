package no.nav.modia.soknadsstatus

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.io.path.Path
import kotlin.io.path.readText

@Serializable
data class Source(
    val name: String,
    val type: Type,
    val resourceId: String,
    @Transient
    val exampleFile: String = "",
) {
    @Serializable
    enum class Type {
        JMS,
        KAFKA,
    }

    val content = Path(exampleFile).readText()
}
