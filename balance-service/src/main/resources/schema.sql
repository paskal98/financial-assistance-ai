CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE user_balances (
                               user_id UUID PRIMARY KEY,
                               balance DECIMAL(15, 2) NOT NULL DEFAULT 0.0,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE savings_goals (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               name VARCHAR(50) NOT NULL,
                               target_amount DECIMAL(15, 2) NOT NULL,
                               current_amount DECIMAL(15, 2) DEFAULT 0.0,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES user_balances(user_id)
);

CREATE INDEX idx_user_balances_user_id ON user_balances (user_id);
CREATE INDEX idx_savings_goals_user_id ON savings_goals (user_id);

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_balance_timestamp
    BEFORE UPDATE ON user_balances
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_savings_timestamp
    BEFORE UPDATE ON savings_goals
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();