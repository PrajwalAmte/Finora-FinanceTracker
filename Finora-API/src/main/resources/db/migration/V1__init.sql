-- =============================================================================
-- Finora - Full Schema (consolidated)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------
CREATE TABLE public.users (
    id            BIGSERIAL       PRIMARY KEY,
    username      VARCHAR(50)     NOT NULL UNIQUE,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    password_hash VARCHAR(500)    NOT NULL,
    role          VARCHAR(20)     NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ
);

CREATE INDEX idx_users_username ON public.users(username);
CREATE INDEX idx_users_email    ON public.users(email);

-- ---------------------------------------------------------------------------
-- Expenses
-- ---------------------------------------------------------------------------
CREATE TABLE public.expenses (
    id             BIGSERIAL       PRIMARY KEY,
    description    TEXT,
    amount         NUMERIC(19, 2),
    date           DATE,
    category       TEXT,
    payment_method TEXT,
    user_id        BIGINT          REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_expenses_date    ON public.expenses(date);
CREATE INDEX idx_expenses_user_id ON public.expenses(user_id);

-- ---------------------------------------------------------------------------
-- Investments
-- ---------------------------------------------------------------------------
CREATE TABLE public.investments (
    id             BIGSERIAL       PRIMARY KEY,
    name           TEXT,
    symbol         TEXT,
    type           TEXT,
    quantity       NUMERIC(19, 6),
    purchase_price NUMERIC(19, 6),
    current_price  NUMERIC(19, 6),
    purchase_date  DATE,
    last_updated   DATE,
    user_id        BIGINT          REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_investments_symbol  ON public.investments(symbol);
CREATE INDEX idx_investments_type    ON public.investments(type);
CREATE INDEX idx_investments_user_id ON public.investments(user_id);

-- ---------------------------------------------------------------------------
-- Loans
-- ---------------------------------------------------------------------------
CREATE TABLE public.loans (
    id                    BIGSERIAL       PRIMARY KEY,
    name                  TEXT,
    principal_amount      NUMERIC(19, 2),
    interest_rate         NUMERIC(9,  6),
    interest_type         TEXT,
    compounding_frequency TEXT,
    start_date            DATE,
    tenure_months         INTEGER,
    emi_amount            NUMERIC(19, 2),
    current_balance       NUMERIC(19, 2),
    last_updated          DATE,
    user_id               BIGINT          REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_loans_start_date ON public.loans(start_date);
CREATE INDEX idx_loans_user_id    ON public.loans(user_id);

-- ---------------------------------------------------------------------------
-- SIPs
-- ---------------------------------------------------------------------------
CREATE TABLE public.sips (
    id                   BIGSERIAL       PRIMARY KEY,
    name                 TEXT,
    scheme_code          TEXT,
    monthly_amount       NUMERIC(19, 2),
    start_date           DATE,
    duration_months      INTEGER,
    current_nav          NUMERIC(19, 6),
    total_units          NUMERIC(24, 8),
    last_updated         DATE,
    last_investment_date DATE,
    user_id              BIGINT          REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sips_scheme_code ON public.sips(scheme_code);
CREATE INDEX idx_sips_user_id     ON public.sips(user_id);

-- ---------------------------------------------------------------------------
-- Ledger events (append-only audit log)
-- ---------------------------------------------------------------------------
CREATE TABLE public.ledger_events (
    id              UUID        PRIMARY KEY,
    event_sequence  BIGSERIAL   UNIQUE,
    event_uuid      UUID        UNIQUE NOT NULL,
    entity_type     VARCHAR     NOT NULL,
    entity_id       VARCHAR     NOT NULL,
    action_type     VARCHAR     NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    event_timestamp TIMESTAMPTZ NOT NULL,
    prev_hash       VARCHAR,
    hash            VARCHAR     NOT NULL,
    user_id         VARCHAR     NOT NULL,
    event_version   INTEGER     NOT NULL DEFAULT 1
);

CREATE INDEX idx_ledger_user_time ON public.ledger_events(user_id, event_sequence);
CREATE INDEX idx_ledger_entity    ON public.ledger_events(entity_type, entity_id);

-- Prevent any UPDATE or DELETE on ledger_events (append-only guarantee)
CREATE OR REPLACE FUNCTION prevent_ledger_update()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_no_update
BEFORE UPDATE OR DELETE ON public.ledger_events
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_update();


