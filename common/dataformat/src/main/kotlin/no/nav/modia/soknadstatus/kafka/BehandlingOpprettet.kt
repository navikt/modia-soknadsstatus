package no.nav.modia.soknadstatus.kafka

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class BehandlingOpprettet(
    val aktoerREF: List<AktoerREF>,
    val ansvarligEnhetREF: String,
    val applikasjonBehandlingREF: String,
    val applikasjonSakREF: String,
    val behandlingsID: String,
    val behandlingstema: Behandlingstema,
    val behandlingstype: Behandlingstype,
    val hendelseType: String,
    val hendelsesId: String,
    val hendelsesTidspunkt: LocalDateTime,
    val hendelsesprodusentREF: HendelsesprodusentREF,
    val primaerBehandlingREF: PrimaerBehandlingREF?,
    val sakstema: Sakstema,
    val sekundaerBehandlingREF: List<SekundaerBehandlingREF>,
    val styringsinformasjonListe: List<StyringsinformasjonListe>
)

fun generateMessage(): String {
    return """
        {
            "aktoerREF": ["aktør"],
            "ansvarligEnhetREF": "enhet",
            "applikasjonBehandlingREF": "applikasjon", 
            "applikasjonSakREF": "applikasjonsak",
            "behandlingsID": "behandlingsID",
            "behandlingstema": {
                "kodeRef": "kodeRef",
                "kodeverksRef": "kodeverksRef",
                "value": "value"
            },
            "behandlingstype": {
                "kodeRef": "kodeRef",
                "kodeverksRef": "kodeverksRef",
                "value": "value"
            },
            "hendelseType": "hendelseType",
            "hendelsesId": "hendelseId",
            "hendelsesTidspunkt": "${java.time.LocalDateTime.now()}",
            "hendelsesprodusentREF": {
                "kodeRef": "kodeRef",
                "kodeverksRef": "kodeverksRef",
                "value": "value"
            },
            "primaerBehandlingREF": {
                "behandlingsREF": "behandlingsREF",
                "type": {
                    "kodeRef": "kodeRef",
                    "kodeverksRef": "kodeverksRef",
                    "value": "value"
                },
            },
            "sakstema": {
                "kodeRef": "kodeRef",
                "kodeverksRef": "kodeverksRef",
                "value": "value"
            },
            "sekundaerBehandlingREF": [ 
                {
                    "behandlingsREF": "behandlingsREF",
                    "type": {
                        "kodeRef": "kodeRef",
                        "kodeverksRef": "kodeverksRef",
                        "value": "value"
                    }
                }
            ],
            "styringsinformasjonListe": [
                {
                    "key": "key",
                    "type": "type",
                    "value": "value"
                }
            ]
        }
    """.trimIndent()
}

fun main() {
    val message = generateMessage()
    println(message)
    val behandling = Json.decodeFromString(BehandlingOpprettet.serializer(), message)
    println(behandling)
}