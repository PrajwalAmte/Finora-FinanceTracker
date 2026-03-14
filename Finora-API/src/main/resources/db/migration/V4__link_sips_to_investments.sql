-- V4: Link SIP payment schedules to their backing Investment record.
--
-- A Mutual Fund held via SIP is ONE asset — the Investment row is the
-- source of truth for units / NAV / current value.  The SIP row is now
-- purely a payment-schedule layer (monthly amount, next installment date).
--
-- Rules:
--   investment_id NOT NULL  → linked SIP; value/units come from investments table
--   investment_id NULL      → standalone SIP (manually created, no MF backing)

ALTER TABLE public.sips
    ADD COLUMN IF NOT EXISTS investment_id BIGINT
        REFERENCES public.investments(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sips_investment_id ON public.sips(investment_id);

-- Best-effort auto-link: match existing SIP ↔ Investment pairs by ISIN.
-- Where both rows share the same ISIN and belong to the same user, link them.
UPDATE public.sips s
SET    investment_id = i.id
FROM   public.investments i
WHERE  s.isin IS NOT NULL
  AND  i.isin IS NOT NULL
  AND  s.isin = i.isin
  AND  s.user_id = i.user_id
  AND  s.investment_id IS NULL;
