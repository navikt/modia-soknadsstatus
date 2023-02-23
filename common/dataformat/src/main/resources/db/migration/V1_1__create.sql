CREATE TYPE statusEnum AS ENUM ('UNDER_BEHANDLING', 'FERDIG_BEHANDLET', 'AVBRUTT');

CREATE TABLE soknadsstatus
(
    ident          varchar(11) not null,
    behandlingsRef varchar(40) not null,
    systemRef      varchar(40) not null,
    tema           varchar(8)  not null,
    status         statusEnum  not null,
    tidspunkt      timestamp   not null,
    PRIMARY KEY (ident, behandlingsRef)
);

CREATE INDEX soknadsstatus_tidspunkt_idx ON soknadsstatus (tidspunkt);