# Modia Søknadstatus

Applikasjoner for å samle inn informasjon om status på brukers søknader.


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