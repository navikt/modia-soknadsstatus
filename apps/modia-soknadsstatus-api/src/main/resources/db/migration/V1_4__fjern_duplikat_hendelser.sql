-- step 1
CREATE TABLE hendelser_temp (LIKE hendelser);

-- step 2
INSERT INTO hendelser_temp(id, hendelses_id, modia_behandling_id, hendelse_produsent, hendelse_tidspunkt, hendelse_type, status, ansvarlig_enhet, behandling_id, behandlingstema, behandlingstype)
SELECT DISTINCT ON (modia_behandling_id, hendelses_id, status) id, hendelses_id, modia_behandling_id, hendelse_produsent, hendelse_tidspunkt, hendelse_type, status, ansvarlig_enhet, behandling_id, behandlingstema, behandlingstype
FROM hendelser;

-- step 3
ALTER TABLE hendelser RENAME TO hendelser_old;
ALTER TABLE hendelser_temp RENAME TO hendelser;

-- step 4
ALTER TABLE hendelser ADD UNIQUE (modia_behandling_id, hendelses_id, status);

