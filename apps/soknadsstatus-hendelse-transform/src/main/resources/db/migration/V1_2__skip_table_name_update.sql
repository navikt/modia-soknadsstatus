DROP TABLE IF EXISTS fp_k9_dlq_event_skip;

CREATE TABLE hendelse_dlq_event_skip
(
    key        VARCHAR(60) PRIMARY KEY  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    skipped_at TIMESTAMP WITH TIME ZONE
);