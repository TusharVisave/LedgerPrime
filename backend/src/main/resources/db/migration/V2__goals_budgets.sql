-- V2: savings goals, receipt attachments, per-category budgets with rollover.

CREATE TABLE IF NOT EXISTS goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    target_paise BIGINT NOT NULL CHECK (target_paise > 0),
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS goal_allocations (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    note VARCHAR(200) NOT NULL DEFAULT '',
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS receipt_image_path VARCHAR(500);

ALTER TABLE budgets ADD COLUMN IF NOT EXISTS category VARCHAR(60) NOT NULL DEFAULT '';
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS rollover BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_budgets_user_month;
-- "month" is a reserved SQL keyword; the column is named budget_month (see V1).
ALTER TABLE budgets
    ADD CONSTRAINT uq_budgets_user_month_category UNIQUE (user_id, budget_month, category);

CREATE INDEX IF NOT EXISTS idx_goals_user ON goals (user_id);
CREATE INDEX IF NOT EXISTS idx_goal_allocations_goal ON goal_allocations (goal_id);
