CREATE TABLE IF NOT EXISTS public.expenses (
  id              BIGSERIAL PRIMARY KEY,
  description     TEXT,
  amount          NUMERIC(19, 2),
  date            DATE,
  category        TEXT,
  payment_method  TEXT
);

CREATE INDEX IF NOT EXISTS idx_expenses_date ON public.expenses (date);


CREATE TABLE IF NOT EXISTS public.investments (
  id               BIGSERIAL PRIMARY KEY,
  name             TEXT,
  symbol           TEXT,
  type             TEXT,
  quantity         NUMERIC(19, 6),
  purchase_price   NUMERIC(19, 6),
  current_price    NUMERIC(19, 6),
  purchase_date    DATE,
  last_updated     DATE
);

CREATE INDEX IF NOT EXISTS idx_investments_symbol ON public.investments (symbol);
CREATE INDEX IF NOT EXISTS idx_investments_type ON public.investments (type);


CREATE TABLE IF NOT EXISTS public.loans (
  id                      BIGSERIAL PRIMARY KEY,
  name                    TEXT,
  principal_amount        NUMERIC(19, 2),
  interest_rate           NUMERIC(9, 6),
  interest_type           TEXT,
  compounding_frequency   TEXT,
  start_date              DATE,
  tenure_months           INTEGER,
  emi_amount              NUMERIC(19, 2),
  current_balance         NUMERIC(19, 2),
  last_updated            DATE
);

CREATE INDEX IF NOT EXISTS idx_loans_start_date ON public.loans (start_date);


CREATE TABLE IF NOT EXISTS public.sips (
  id                     BIGSERIAL PRIMARY KEY,
  name                   TEXT,
  scheme_code            TEXT,
  monthly_amount         NUMERIC(19, 2),
  start_date             DATE,
  duration_months        INTEGER,
  current_nav            NUMERIC(19, 6),
  total_units            NUMERIC(24, 8),
  last_updated           DATE,
  last_investment_date   DATE
);

CREATE INDEX IF NOT EXISTS idx_sips_scheme_code ON public.sips (scheme_code);


