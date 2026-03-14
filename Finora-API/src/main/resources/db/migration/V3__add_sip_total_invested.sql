-- Stores the actual total amount invested in the SIP so far.
-- This replaces the computed (monthlyAmount × completedInstallments) approach,
-- which was inaccurate when installments were missed or the start date was unknown.
ALTER TABLE sips ADD COLUMN IF NOT EXISTS total_invested NUMERIC(19,2) DEFAULT 0;
