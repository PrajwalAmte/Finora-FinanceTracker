-- ============================================================
-- V5: Schema hardening
-- ============================================================

-- ---------------------------------------------------------------------------
-- 1. PERFORMANCE: Replace two weak single-column indices on expenses with one
--    composite (user_id, date) index that covers every user-scoped date-range
--    query (findByUserIdAndDateBetween, sumExpensesByUserIdBetweenDates, etc.).
-- ---------------------------------------------------------------------------
DROP INDEX IF EXISTS public.idx_expenses_user_id;
DROP INDEX IF EXISTS public.idx_expenses_date;
CREATE INDEX idx_expenses_user_date ON public.expenses(user_id, date);

-- ---------------------------------------------------------------------------
-- 2. PERFORMANCE: Composite index on investments(user_id, type).
--    Useful for future per-type summaries and any filtered list queries.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_investments_user_type ON public.investments(user_id, type);

-- ---------------------------------------------------------------------------
-- 3. DATA INTEGRITY: Partial unique index on sips(user_id, isin).
--    Mirrors the equivalent constraint on investments — prevents the same
--    ISIN from being imported twice for the same user.
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_sips_user_isin
    ON public.sips(user_id, isin)
    WHERE isin IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. DATA INTEGRITY: CHECK constraints on enum-mapped TEXT columns.
--    The Java enums enforce valid values at the application layer, but without
--    DB-level constraints a raw INSERT / admin script could write garbage.
-- ---------------------------------------------------------------------------
ALTER TABLE public.investments
    ADD CONSTRAINT chk_investments_type
    CHECK (type IN ('STOCK', 'MUTUAL_FUND', 'ETF', 'BOND', 'OTHER'));

ALTER TABLE public.loans
    ADD CONSTRAINT chk_loans_interest_type
    CHECK (interest_type IN ('SIMPLE', 'COMPOUND'));

ALTER TABLE public.loans
    ADD CONSTRAINT chk_loans_compounding_frequency
    CHECK (compounding_frequency IN ('MONTHLY', 'QUARTERLY', 'YEARLY'));
