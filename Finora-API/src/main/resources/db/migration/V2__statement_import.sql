-- =============================================================================
-- V2: Statement import columns
-- Adds ISIN and import_source tracking to investments and sips.
-- import_source NULL = manual entry (never overwritten by re-import).
-- import_source non-NULL = one of: CAS, CAMS, ZERODHA_EXCEL.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- investments
-- ---------------------------------------------------------------------------
ALTER TABLE public.investments ADD COLUMN isin TEXT;
ALTER TABLE public.investments ADD COLUMN import_source TEXT;

-- Partial unique index: at most one row per (user, ISIN) for imported rows.
-- Manual rows (isin IS NULL) are excluded — users may hold the same stock
-- in multiple manual entries (e.g. different lots tracked separately).
-- This index is the DB-level guard against concurrent-import races.
CREATE UNIQUE INDEX idx_investments_user_isin
    ON public.investments (user_id, isin)
    WHERE isin IS NOT NULL;

CREATE INDEX idx_investments_isin          ON public.investments (isin);
CREATE INDEX idx_investments_import_source ON public.investments (import_source);

-- ---------------------------------------------------------------------------
-- sips
-- ---------------------------------------------------------------------------
ALTER TABLE public.sips ADD COLUMN isin TEXT;
ALTER TABLE public.sips ADD COLUMN import_source TEXT;

CREATE INDEX idx_sips_isin          ON public.sips (isin);
CREATE INDEX idx_sips_import_source ON public.sips (import_source);
