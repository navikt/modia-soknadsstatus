CREATE TYPE statusEnum AS ENUM ('UNDER_BEHANDLING', 'FERDIG_BEHANDLET', 'AVBRUTT');
CREATE TYPE hendelseTypeEnum AS ENUM ('BEHANDLING_OPPRETTET', 'BEHANDLING_OPPRETTET_OG_AVSLUTTET', 'BEHANDLING_AVSLUTTET');

CREATE TABLE behandlinger
(
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behandling_id           VARCHAR(128) UNIQUE,
    produsent_system        VARCHAR(128) NOT NULL,
    start_tidspunkt         TIMESTAMPTZ  NOT NULL,
    slutt_tidspunkt         TIMESTAMPTZ,
    sist_oppdatert          TIMESTAMPTZ  NOT NULL,
    sakstema                VARCHAR(128),
    behandlingstema         VARCHAR(128),
    behandlingstype         VARCHAR(128),
    status                  statusEnum   NOT NULL,
    ansvarlig_enhet         VARCHAR(128) NOT NULL,
    primaer_behandling_id   VARCHAR(128),
    primaer_behandling_type VARCHAR(128),
    applikasjon_sak         VARCHAR(128),
    applikasjon_behandling  VARCHAR(128)
);

CREATE TABLE hendelser
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hendelses_id        VARCHAR(128),
    behandling_id       VARCHAR(128),
    behandlingstema     VARCHAR(128),
    behandlingstype     VARCHAR(128),
    modia_behandling_id UUID REFERENCES behandlinger (id) ON DELETE CASCADE,
    hendelse_produsent  VARCHAR(128)     NOT NULL,
    hendelse_tidspunkt  TIMESTAMPTZ      NOT NULL,
    hendelse_type       hendelseTypeEnum NOT NULL,
    status              statusEnum       NOT NULL,
    ansvarlig_enhet     VARCHAR(128)     NOT NULL
);

CREATE TABLE behandling_eiere
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ident         VARCHAR(11),
    behandling_id UUID REFERENCES behandlinger (id) ON DELETE CASCADE,
    UNIQUE (ident, behandling_id)
);

CREATE TABLE hendelse_eiere
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ident       VARCHAR(11),
    hendelse_id UUID REFERENCES hendelser (id) ON DELETE CASCADE,
    UNIQUE (ident, hendelse_id)
);
CREATE TABLE soknadsstatus_api_dlq_event_skip
(
    key        VARCHAR(60) PRIMARY KEY  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    skipped_at TIMESTAMP WITH TIME ZONE
);

DO $$
BEGIN
        IF EXISTS
            ( SELECT 1 from pg_roles where rolname='cloudsqliamuser')
        THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamuser;
END IF ;
END
$$ ;