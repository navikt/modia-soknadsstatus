
ALTER TABLE behandlinger ALTER COLUMN behandling_id TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN produsent_system TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN sakstema TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN behandlingstema TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN behandlingstype TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN ansvarlig_enhet TYPE VARCHAR(128);
ALTER TABLE behandlinger ALTER COLUMN primaer_behandling_id TYPE VARCHAR(128);

ALTER TABLE hendelser ALTER COLUMN hendelses_id TYPE VARCHAR(128);
ALTER TABLE hendelser ALTER COLUMN hendelse_produsent TYPE VARCHAR(128);
ALTER TABLE hendelser ALTER COLUMN ansvarlig_enhet TYPE VARCHAR(128);

ALTER TABLE hendelser RENAME COLUMN behandling_id TO modia_behandling_id;
ALTER TABLE hendelser ADD COLUMN behandling_id VARCHAR(128);

UPDATE hendelser h SET behandling_id = b.behandling_id from behandlinger b where b.id = h.modia_behandling_id;