
UPDATE hendelser h SET behandlingstema = b.behandlingstema, behandlingstype = b.behandlingstype from behandlinger b where b.id = h.modia_behandling_id;

