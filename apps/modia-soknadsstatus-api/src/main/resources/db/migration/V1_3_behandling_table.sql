DROP TABLE IF EXISTS soknadsstatus;

CREATE TABLE identer
(
    ident VARCHAR(11) UNIQUE
);


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
    status                statusEnum  NOT NULL,
    ansvarlig_enhet       VARCHAR(20) NOT NULL,
    primaer_behandling_id VARCHAR(20)
);

CREATE TABLE behandling_eiere
(
    ident         VARCHAR(11) REFERENCES identer (ident) ON DELETE CASCADE,
    behandling_id VARCHAR(20) REFERENCES behandlinger (behandling_id) ON DELETE CASCADE,
    PRIMARY KEY (ident, behandling_id)
);

CREATE TABLE hendelser
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hendelses_id       VARCHAR(20) UNIQUE,
    behandling_id      UUID REFERENCES behandlinger (id) ON DELETE CASCADE,
    hendelse_produsent VARCHAR(20) NOT NULL,
    hendelse_tidspunkt TIMESTAMPTZ NOT NULL,
    hendelse_type      VARCHAR(20) NOT NULL,
    status             statusEnum  NOT NULL,
    ansvarlig_enhet    VARCHAR(20) NOT NULL,
    produsent_system   VARCHAR(20) NOT NULL
);

CREATE TABLE hendelse_eiere
(
    ident       VARCHAR(11) REFERENCES identer (ident) ON DELETE CASCADE,
    hendelse_id VARCHAR(20) REFERENCES hendelser (hendelses_id) ON DELETE CASCADE,
    PRIMARY KEY (ident, hendelse_id)
);


