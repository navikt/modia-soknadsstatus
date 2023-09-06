DROP TABLE IF EXISTS soknadsstatus;

CREATE TYPE hendelseTypeEnum AS ENUM ('BEHANDLING_OPPRETTET', 'BEHANDLING_OPPRETTET_OG_AVSLUTTET', 'BEHANDLING_AVSLUTTET');

CREATE TABLE behandlinger
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behandling_id         VARCHAR(20) UNIQUE,
    produsent_system      VARCHAR(20) NOT NULL,
    start_tidspunkt       TIMESTAMPTZ NOT NULL,
    slutt_tidspunkt       TIMESTAMPTZ,
    sist_oppdatert        TIMESTAMPTZ NOT NULL,
    sakstema              VARCHAR(10),
    behandlingstema       VARCHAR(10),
    behandlingstype       VARCHAR(10),
    status                statusEnum  NOT NULL,
    ansvarlig_enhet       VARCHAR(20) NOT NULL,
    primaer_behandling_id VARCHAR(20)
);

CREATE TABLE hendelser
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hendelses_id       VARCHAR(20) UNIQUE,
    behandling_id      UUID REFERENCES behandlinger (id) ON DELETE CASCADE,
    hendelse_produsent VARCHAR(20) NOT NULL,
    hendelse_tidspunkt TIMESTAMPTZ NOT NULL,
    hendelse_type      hendelseTypeEnum  NOT NULL,
    status             statusEnum  NOT NULL,
    ansvarlig_enhet    VARCHAR(20) NOT NULL
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


