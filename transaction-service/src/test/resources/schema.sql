-- Включаем расширение для генерации UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Таблица категорий
CREATE TABLE categories (
                            id SERIAL PRIMARY KEY,                -- Уникальный идентификатор категории
                            name VARCHAR(50) NOT NULL UNIQUE,     -- Название категории (уникальное)
                            type VARCHAR(7) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')) -- Тип категории
);

-- Таблица транзакций
CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL,
                              amount DECIMAL(15, 2) NOT NULL,
                              type VARCHAR(7) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                              category VARCHAR(50) NOT NULL,
                              description TEXT,
                              date TIMESTAMP WITH TIME ZONE NOT NULL,
                              payment_method VARCHAR(50),
                              document_id UUID,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_category ON transactions(category);

-- Триггер для обновления updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_timestamp
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Вставка категорий с исправленным "Gifts"
INSERT INTO categories (name, type) VALUES
                                        -- Расходы
                                        ('Groceries', 'EXPENSE'),
                                        ('Restaurants', 'EXPENSE'),
                                        ('Transport', 'EXPENSE'),
                                        ('Entertainment', 'EXPENSE'),
                                        ('Utilities', 'EXPENSE'),
                                        ('Shopping', 'EXPENSE'),
                                        ('Healthcare', 'EXPENSE'),
                                        ('Education', 'EXPENSE'),
                                        ('Travel', 'EXPENSE'),
                                        ('Subscriptions', 'EXPENSE'),
                                        ('Rent', 'EXPENSE'),
                                        ('Taxes', 'EXPENSE'),
                                        ('Gifts Given', 'EXPENSE'),  -- Подарки, которые ты даришь
                                        ('Pets', 'EXPENSE'),
                                        ('Hobbies', 'EXPENSE'),
                                        ('Insurance', 'EXPENSE'),
                                        ('Repairs', 'EXPENSE'),
                                        ('Business Expenses', 'EXPENSE'),
                                        ('Childcare', 'EXPENSE'),
                                        ('Debt Repayment', 'EXPENSE'),
                                        -- Доходы
                                        ('Salary', 'INCOME'),
                                        ('Freelance', 'INCOME'),
                                        ('Investments', 'INCOME'),
                                        ('Bonuses', 'INCOME'),
                                        ('Gifts Received', 'INCOME'),  -- Подарки, которые тебе подарили
                                        ('Refunds', 'INCOME'),
                                        ('Passive Income', 'INCOME'),
                                        ('Sales', 'INCOME'),
                                        ('Grants', 'INCOME'),
                                        ('Cashback', 'INCOME');

