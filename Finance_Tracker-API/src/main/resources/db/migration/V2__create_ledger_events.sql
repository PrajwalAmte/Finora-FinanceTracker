CREATE TABLE ledger_events (
    id              UUID PRIMARY KEY,
    event_sequence  BIGSERIAL UNIQUE,
    event_uuid      UUID UNIQUE NOT NULL,
    entity_type     VARCHAR NOT NULL,
    entity_id       VARCHAR NOT NULL,
    action_type     VARCHAR NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    event_timestamp TIMESTAMPTZ NOT NULL,
    prev_hash       VARCHAR,
    hash            VARCHAR NOT NULL,
    user_id         VARCHAR NOT NULL,
    event_version   INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_ledger_user_time ON ledger_events(user_id, event_sequence);

CREATE INDEX idx_ledger_entity ON ledger_events(entity_type, entity_id);

CREATE OR REPLACE FUNCTION prevent_ledger_update()
RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'ledger_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_no_update
BEFORE UPDATE OR DELETE ON ledger_events
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_update();
