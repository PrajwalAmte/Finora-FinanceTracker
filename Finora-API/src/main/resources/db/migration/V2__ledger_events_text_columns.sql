-- ---------------------------------------------------------------------------
-- Convert ledger_events before_state / after_state from JSONB → TEXT
--
-- Rationale: these columns may store AES-GCM encrypted blobs (not valid JSON)
-- when field-level encryption is enabled. TEXT accepts any string; JSONB does
-- not. PostgreSQL can implicitly cast TEXT ↔ JSONB so this is a lossless
-- change for existing plain-JSON rows.
-- ---------------------------------------------------------------------------
ALTER TABLE public.ledger_events
    ALTER COLUMN before_state TYPE TEXT,
    ALTER COLUMN after_state  TYPE TEXT;
