-- ============================================================
-- V6: Field-level encryption infrastructure
-- ============================================================
-- This migration prepares the database for AES-256-GCM column encryption.
-- Actual encryption of existing plaintext data is handled by the JPA
-- @Convert annotations on entity fields — new writes are automatically
-- encrypted, and a one-time backfill script should be run to encrypt
-- existing rows.
--
-- NOTE: Before deploying, ensure FIELD_ENCRYPTION_KEY env var is set.
-- If not set, encryption is a no-op (passthrough).
-- ============================================================

-- ---------------------------------------------------------------------------
-- 1. Add vault columns to users table
-- ---------------------------------------------------------------------------
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS vault_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS vault_salt VARCHAR(64);

-- ---------------------------------------------------------------------------
-- 2. Widen email column to accommodate encrypted ciphertext
--    Original: 255 chars -> New: 500 chars (Base64 ciphertext is ~1.33x longer)
-- ---------------------------------------------------------------------------
ALTER TABLE public.users
    ALTER COLUMN email TYPE VARCHAR(500);

-- ---------------------------------------------------------------------------
-- 3. Widen text columns that will hold encrypted data
--    TEXT type in Postgres is unlimited, but we document the intent.
--    These columns already use TEXT, so no ALTER needed:
--    - expenses.description
--    - investments.name
--    - loans.name
--    - sips.name
--    - ledger_events.before_state (JSONB)
--    - ledger_events.after_state (JSONB)
-- ---------------------------------------------------------------------------
-- No action needed — TEXT has no length limit.

-- ---------------------------------------------------------------------------
-- 4. Add index on vault_enabled for potential admin queries
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_users_vault_enabled ON public.users(vault_enabled);
