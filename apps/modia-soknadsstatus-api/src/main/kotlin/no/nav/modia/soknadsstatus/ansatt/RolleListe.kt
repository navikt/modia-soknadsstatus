package no.nav.modia.soknadsstatus.ansatt

class RolleListe(
    roller: Set<String>,
) : LinkedHashSet<String>(roller.map { it.lowercase() }) {
    constructor(roller: List<String>) : this(roller.toSet())
    constructor(vararg roller: String) : this(roller.toSet())

    fun hasIntersection(other: RolleListe): Boolean = this.intersect(other).isNotEmpty()
}
