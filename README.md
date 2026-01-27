# Modia Søknadstatus

## Prosjektets Formål
Dette prosjektet samler inn og tilbyr informasjon om status på brukeres søknader i NAV. Dataen aggregeres og brukes i Modia personoversikt.

---

## Arkitekturoversikt

Prosjektet består av **5 applikasjoner** som jobber sammen i en datapipeline:

```
Eksterne Systemer → MQ → Kafka → Transform Apps → Felles Topic → API → Modia Frontend
```

### Dataflytdiagram

```
      ARENA  ──┐
               ├─→ [MQ-kø] ──→ [mq-to-kafka] ──→ Kafka: arena-infotrygd-soknadsstatus
    INFOTRYGD ─┘                                 │
                                                  ↓
                                           [arena-infotrygd-transform]
                                                  │
                                                  ↓
                                    Kafka: personoversikt.modia-soknadsstatus-oppdatering
                                                  ↑
k9 og Foreldrepenger ──→ Kafka: personoversikt.modia-soknadsstatus-hendelse
                                                  │
                                                  ↓
                                      [soknadsstatus-hendelse-transform]
                                                  │
                                                  ↓
                                    Kafka: personoversikt.modia-soknadsstatus-oppdatering
                                                  │
                                                  ↓
                                         [modia-soknadsstatus-api]
                                                  │
                                                  ↓
                                           PostgreSQL Database
                                                  │
                                                  ↓
                                         REST API Endepunkter
                                                  │
                                                  ↓
                                      [modia-soknadsstatus-api]
                                                  │
                                                  ↓
                                           Modia Frontend
```

---

## Applikasjonene Forklart

### 1. **mq-to-kafka**
**Formål:** Bro mellom meldingskø og Kafka  

**Hva den gjør:**
- Kobler til IBM MQ (ActiveMQ i dev) og konsumerer meldinger fra en JMS-kø
- Mottar XML-meldinger fra **ARENA** og **INFOTRYGD** legacy-systemer
- Publiserer rå XML-meldinger til Kafka topic: `arena-infotrygd-soknadsstatus`

**Data mottatt:**
- **Format:** XML (TextMessage fra JMS)
- **Kildesystemer:** ARENA (AO01) og INFOTRYGD (IT00)
- **Skjema:** XSD definert i `behandlingsstatus.xsd`

---

### 2. **arena-infotrygd-soknadsstatus-transform**
**Formål:** XML til JSON transformer for Arena/Infotrygd-meldinger  

**Hva den gjør:**
- Konsumerer fra: `personoversikt.modia-soknadsstatus-arena-infotrygd-oppdatering`
- Deserialiserer XML-meldinger ved hjelp av JAXB (genererer Java-klasser fra XSD ved byggetid)
- Transformerer XML til standardisert `InnkommendeHendelse` JSON-format
- Filtrerer ut ugyldige INFOTRYGD-meldinger (feilregistreringer med blank status)
- Mapper ARENA- og INFOTRYGD-spesifikke statuser til felles domenestatuser
- Publiserer til: `personoversikt.modia-soknadsstatus-oppdatering`

**Datatransformasjoner:**
- **Input:** XML basert på `BehandlingStatus`-skjema
- **Output:** JSON `InnkommendeHendelse`-objekter
- Bruker mappingfiler (`avslutningsstatus-mapping.csv`) for statuskonverteringer

---

### 3. **soknadsstatus-hendelse-transform**
**Formål:** JSON til JSON transformer for moderne NAV-systemer  

**Hva den gjør:**
- Konsumerer fra: `personoversikt.modia-soknadsstatus-hendelse`
- Mottar JSON-meldinger fra nyere NAV-systemer (ikke ARENA/INFOTRYGD)
- Transformerer til standardisert `InnkommendeHendelse`-format
- Publiserer til: `personoversikt.modia-soknadsstatus-oppdatering`

**Dataformat:**
- **Input:** JSON `Hendelse`-objekter (camelCase-navngivning)
- **Output:** JSON `InnkommendeHendelse`-objekter
- Enklere statusmapping (avsluttet/ok/ja → FERDIG_BEHANDLET, avbrutt/nei/no → AVBRUTT)

---

### 4. **modia-soknadsstatus-api**
**Formål:** REST API og datapersistering  

**Hva den gjør:**
- Konsumerer fra: `personoversikt.modia-soknadsstatus-oppdatering` (felles topic)
- Lagrer `InnkommendeHendelse`-meldinger i PostgreSQL-database
- Tilbyr REST API-endepunkter for å spørre på søknadsstatus
- Implementerer tilgangskontroll (JWT-autentisering, Tilgangsmaskinen-integrasjon)
- Håndterer dead letter queue for feilede meldinger

**REST API Endepunkter:**
```
GET  /api/soknadsstatus/behandling/{ident}
POST /api/soknadsstatus/behandling  (body: {})
GET  /api/soknadsstatus/hendelse/{ident}
POST /api/soknadsstatus/hendelse    (body: {})
```

**Query-parameter:**
- `?inkluderHendelser=true` - inkluderer hendelsehistorikk med behandling

**Sikkerhet:**
- JWT-autentisering via Azure AD
- Tilgangskontroll via Tilgangsmaskinen (NAVs tilgangskontrolltjeneste)
- Audit-logging for all tilgang

---

### 5. **data-generator-app**
**Formål:** Testdatagenerator  

**Hva den gjør:**
- Brukes til lokal testing og integrasjonstester
- Simulerer data fra kildesystemer
- Kan publisere til både JMS- og Kafka-topics

---

## Dataformater

### Eksterne Inputformater

#### 1. **ARENA/INFOTRYGD XML-format** (via MQ)
```xml
<behandlingOpprettet>
  <hendelsesId>...</hendelsesId>
  <behandlingsID>...</behandlingsID>
  <sakstema value="AAP"/>
  <behandlingstema value="ab01"/>
  <behandlingstype value="ae0014"/>
  <hendelsesprodusentREF value="AO01"/> <!-- ARENA eller IT00 -->
  <aktoerREF>
    <aktoerId>...</aktoerId>
  </aktoerREF>
  ...
</behandlingOpprettet>
```

**Meldingstyper:**
- `behandlingOpprettet` - Ny søknad opprettet
- `behandlingAvsluttet` - Søknad fullført
- `behandlingOpprettetOgAvsluttet` - Opprettet og umiddelbart fullført

**Produsentsystemer:**
- `AO01` - ARENA (ytelser/velferdsystem)
- `IT00` - INFOTRYGD (legacy pensjon/ytelsessystem)

#### 2. **Moderne Systemer JSON-format** (via Kafka)
```json
{
  "hendelsesId": "...",
  "behandlingsID": "...",
  "sakstema": {"value": "AAP"},
  "behandlingstema": {"value": "ab01"},
  "hendelseType": "behandlingOpprettet",
  "identREF": [{"ident": "00000000000"}],
  "aktoerREF": [{"aktoerId": "0000000000000"}],
  ...
}
```

### Internt Felles Format: `InnkommendeHendelse`

**Alle meldinger transformeres til dette formatet:**
```kotlin
@Serializable
data class InnkommendeHendelse(
    val aktoerer: List<String>,              // AktørID-liste
    val identer: List<String>,               // FNR/DNR-liste
    val ansvarligEnhet: String?,             // Ansvarlig NAV-kontor
    val behandlingsId: String,               // Behandlings-/søknads-ID
    val behandlingsTema: String,             // Behandlingstemakode
    val behandlingsType: String,             // Behandlingstypekode
    val hendelsesType: HendelseType,         // BEHANDLING_OPPRETTET, BEHANDLING_AVSLUTTET, etc.
    val hendelsesId: String,                 // Hendelses-ID
    val hendelsesTidspunkt: LocalDateTime,   // Hendelsestidspunkt
    val opprettelsesTidspunkt: LocalDateTime?, // Opprettelsestidspunkt
    val hendelsesProdusent: String,          // Produsentsystem (AO01, IT00, etc.)
    val applikasjonSak: String?,             // Applikasjonssakreferanse
    val applikasjonBehandling: String?,      // Applikasjonsbehandlingsreferanse
    val sakstema: String,                    // Sakstemakode
    val status: Status,                      // UNDER_BEHANDLING, FERDIG_BEHANDLET, AVBRUTT
    val primaerBehandling: PrimaerBehandling? // Primærbehandlingsreferanse
)
```

### Domenemodell: `SoknadsstatusDomain`

**Status-enum:**
```kotlin
enum class Status {
    UNDER_BEHANDLING,      // Pågående
    FERDIG_BEHANDLET,      // Fullført
    AVBRUTT               // Kansellert/Avbrutt
}
```

**Hendelsestyper:**
```kotlin
enum class HendelseType {
    BEHANDLING_OPPRETTET,
    BEHANDLING_AVSLUTTET,
    BEHANDLING_OPPRETTET_OG_AVSLUTTET
}
```

---

## Hvem Sender Data til Dette Systemet?

### 1. **ARENA **
- **System:** NAVs ytelser og velferdsystem
- **Transport:** IBM MQ → JMS-kø
- **Format:** XML (validert mot XSD-skjema)
- **Datatyper:** Søknadsstatuser for dagpenger, arbeidsavklaringspenger, etc.
- **Temaer:** Ulike sakstema/behandlingstema-kombinasjoner

Her er [Arena-dokumentasjon](https://confluence.adeo.no/spaces/ARENA/pages/122716400/Arena+-+Tjenestekonsument+MQ+-+Modia+Behandlingsstatus_v1) for tjenester som sender data på køen.
Dokumentasjonen inneholder også en liste med alle teamer det kommer meldinger på.

### 2. **INFOTRYGD **
- **System:** NAVs legacy pensjon- og ytelsessystem
- **Transport:** IBM MQ → JMS-kø
- **Format:** XML (validert mot XSD-skjema)
- **Datatyper:** Pensjonssøknader, sykepenger, etc.
- **Spesialtilfeller:** Sender feilregistreringer som filtreres ut

### 3. **Moderne NAV-systemer**
- Foreløpig Foreldrepenger og K9
- **Transport:** Kafka topic `personoversikt.modia-soknadsstatus-hendelse`
- **Format:** JSON
- **Datatyper:** Søknadsstatuser fra nyere NAV-systemer
- **Systemer:** Ikke eksplisitt navngitt i koden, men alle systemer som publiserer til hendelse-topicet
- 
[Dokumentasjon](https://confluence.adeo.no/spaces/INFOTRYGD/pages/126681246/MOD_01+-+Komponenter+og+programmer) fra Infotrygd.

---



## Fellesmoduler

### `/common/dataformat`
- Domenemodeller (`SoknadsstatusDomain`, `InnkommendeHendelse`)
- XML-dataklasser (`Hendelse`, `BehandlingOpprettet`, `BehandlingAvsluttet`)
- Transformere og statusmappere

### `/common/kafka`
- Kafka producer/consumer-verktøy
- Dead letter queue-håndtering
- Kafka Streams-konfigurasjon

### `/common/ktor`
- Basis Ktor-oppsett
- Felles plugins og konfigurasjoner

### `/common/jms`
- JMS consumer-implementasjon
- SSL-konfigurasjon for MQ

---

## Feilhåndtering

Hver transformer-app har:
- **Dead Letter Queue (DLQ)** - Feilede meldinger går til separate Kafka-topics
- **Skip-tabeller** - PostgreSQL-tabeller for å spore hvilke DLQ-meldinger som skal hoppes over
- **Slack-varsler** - Alerts sendes ved kritiske feil
- **Retry-logikk** - Konfigurerbare restart-forsinkelser

**DLQ-topics:**
- `personoversikt.modia-soknadsstatus-oppdatering-dlq` (arena-infotrygd)
- `personoversikt.modia-soknadsstatus-hendelse-dlq` (soknadsstatus-hendelse)
- API har også DLQ-consumer som prøver feilede meldinger på nytt

---

## Lokal Utvikling

**Start alt:**
```bash
make start
```

**Tilgjengelige tjenester:**
- ActiveMQ Admin: http://localhost:8161/admin (admin/admin)
- Redpanda Console: http://localhost:8080/topics
- PostgreSQL: jdbc:postgresql://localhost:5432/modia-soknadsstatus (admin/admin)
- Arena-Infotrygd DB: localhost:5433
- Hendelse Transform DB: localhost:5434

**Apper kjører på:**
- mq-to-kafka: 9000
- arena-infotrygd-transform: 9010
- soknadsstatus-hendelse-transform: 9014
- modia-soknadsstatus-api: 9015


## Utvikling

Kommandoer for lokal utvikling;

| Kommando     | Beskrivelse                                                                       |
|--------------|-----------------------------------------------------------------------------------|
| `make`       | Starter utviklingsmiljø og alle applikasjonene                                    |
| `make start` | Starter utviklingsmiljø og alle applikasjonene                                    |
| `make stop`  | Stopper utviklingsmiljø, og evt applikasjoner                                     |
| `make dev`   | Starter MQ, postgres og kafka. Applikasjonene kan deretter startes fra intelliJ |
| `make build` | Bygger og tester alle applikasjonene                                              |
| `make test`  | Kjører tester                                                                     |

Når utviklingsmiljø er startet opp vil ActiveMQ, PostgreSQL og kafka (redpanda) være startet i bakgrunn.

ActiveMQ adminpanel kan ses her; http://localhost:8161/admin/index.jsp
Brukernavn og passord er **admin**.

Redpanda console kan ses her; http://localhost:8080/topics

postgres er tilgjengelig på jdbc://localhost:5432 Brukernavn og passord er **admin**.