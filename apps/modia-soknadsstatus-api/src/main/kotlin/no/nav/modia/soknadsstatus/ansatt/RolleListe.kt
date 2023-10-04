package no.nav.modia.soknadsstatus.ansatt

class RolleListe(
    roller: Set<AnsattRolle>,
) : LinkedHashSet<AnsattRolle>(roller.map { it }) {
    constructor(roller: List<AnsattRolle>) : this(roller.toSet())
    constructor(vararg roller: AnsattRolle) : this(roller.toSet())

    fun hasIntersection(other: RolleListe): Boolean = this.intersect(other).isNotEmpty()
}
