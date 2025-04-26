-- Створення таблиць для платформи вивчення мов

-- Таблиця з мовами
CREATE TABLE IF NOT EXISTS languages (
                                         id SERIAL PRIMARY KEY,
                                         code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    native_name VARCHAR(50),
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Створення індексу для швидкого пошуку мови за кодом
CREATE INDEX IF NOT EXISTS idx_languages_code ON languages(code);

-- Таблиця користувачів
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    native_language_id INT REFERENCES languages(id),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'PREMIUM', 'ADMIN'))
    );

-- Індекс для швидкого пошуку користувача за email та username
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Таблиця з колекціями карток
CREATE TABLE IF NOT EXISTS collections (
                                           id SERIAL PRIMARY KEY,
                                           user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    source_language_id INT NOT NULL REFERENCES languages(id),
    target_language_id INT NOT NULL REFERENCES languages(id),
    is_public BOOLEAN DEFAULT FALSE,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Індекс для швидкого пошуку колекцій користувача та публічних колекцій
CREATE INDEX IF NOT EXISTS idx_collections_user_id ON collections(user_id);
CREATE INDEX IF NOT EXISTS idx_collections_public ON collections(is_public);

-- Таблиця з картками для вивчення
CREATE TABLE IF NOT EXISTS cards (
                                     id SERIAL PRIMARY KEY,
                                     collection_id INT NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    front_text VARCHAR(255) NOT NULL,
    back_text VARCHAR(255) NOT NULL,
    phonetic_text VARCHAR(255),
    example_usage TEXT,
    image_url VARCHAR(255),
    audio_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Індекс для швидкого пошуку карток за колекцією
CREATE INDEX IF NOT EXISTS idx_cards_collection_id ON cards(collection_id);

-- Таблиця з прогресом вивчення карток
CREATE TABLE IF NOT EXISTS learning_progress (
                                                 id SERIAL PRIMARY KEY,
                                                 user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id INT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    repetition_count INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    incorrect_answers INT DEFAULT 0,
    ease_factor DECIMAL(4,2) DEFAULT 2.5,
    next_review_date TIMESTAMP,
    learning_status VARCHAR(20) DEFAULT 'NEW' CHECK (learning_status IN ('NEW', 'LEARNING', 'REVIEW', 'KNOWN')),
    last_reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, card_id)
    );

-- Індекси для швидкого пошуку прогресу користувача та карток для повторення
CREATE INDEX IF NOT EXISTS idx_learning_progress_user_id ON learning_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_progress_card_id ON learning_progress(card_id);
CREATE INDEX IF NOT EXISTS idx_learning_progress_next_review ON learning_progress(user_id, next_review_date);

-- Таблиця з історією вивчення (для аналітики)
CREATE TABLE IF NOT EXISTS learning_history (
                                                id SERIAL PRIMARY KEY,
                                                user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id INT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    action_type VARCHAR(20) NOT NULL CHECK (action_type IN ('CREATE', 'VIEW', 'REVIEW', 'EDIT', 'DELETE')),
    is_correct BOOLEAN,
    response_time_ms INT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Індекс для швидкого пошуку історії вивчення за користувачем та часом
CREATE INDEX IF NOT EXISTS idx_learning_history_user_id ON learning_history(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_history_performed_at ON learning_history(performed_at);

-- Таблиця для зберігання статистики користувача
CREATE TABLE IF NOT EXISTS user_statistics (
                                               id SERIAL PRIMARY KEY,
                                               user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    total_cards INT DEFAULT 0,
    learned_cards INT DEFAULT 0,
    total_study_time_minutes INT DEFAULT 0,
    learning_streak_days INT DEFAULT 0,
    last_study_date DATE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Таблиця для збереження тегів
CREATE TABLE IF NOT EXISTS tags (
                                    id SERIAL PRIMARY KEY,
                                    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Зв'язок між картками та тегами (багато-до-багатьох)
CREATE TABLE IF NOT EXISTS card_tags (
                                         card_id INT REFERENCES cards(id) ON DELETE CASCADE,
    tag_id INT REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, tag_id)
    );

-- Зберігання журналу дій користувачів (аудит)
CREATE TABLE IF NOT EXISTS user_activity_log (
                                                 id SERIAL PRIMARY KEY,
                                                 user_id INT REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INT,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE OR REPLACE FUNCTION update_timestamp_column() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
-- Застосування тригера до відповідних таблиць
CREATE TRIGGER update_languages_timestamp BEFORE UPDATE ON languages
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER update_collections_timestamp BEFORE UPDATE ON collections
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER update_cards_timestamp BEFORE UPDATE ON cards
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();

-- Тригер для оновлення статистики користувача при зміні прогресу вивчення
CREATE OR REPLACE FUNCTION update_user_statistics()
RETURNS TRIGGER AS $$
BEGIN
    -- Оновлюємо статистику користувача
    IF (TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND OLD.learning_status != NEW.learning_status)) THEN
        -- Якщо статус змінено на KNOWN, збільшуємо кількість вивчених карток
        IF NEW.learning_status = 'KNOWN' AND (TG_OP = 'INSERT' OR OLD.learning_status != 'KNOWN') THEN
UPDATE user_statistics
SET learned_cards = learned_cards + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.user_id;
-- Якщо статус змінено з KNOWN на інший, зменшуємо кількість вивчених карток
ELSIF OLD.learning_status = 'KNOWN' AND NEW.learning_status != 'KNOWN' THEN
UPDATE user_statistics
SET learned_cards = GREATEST(0, learned_cards - 1),
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = NEW.user_id;
END IF;
END IF;

    -- Оновлюємо дату останнього вивчення та streak
    IF NEW.last_reviewed_at::date > COALESCE((SELECT last_study_date FROM user_statistics WHERE user_id = NEW.user_id), '1900-01-01'::date) THEN
UPDATE user_statistics
SET last_study_date = NEW.last_reviewed_at::date,
            learning_streak_days = CASE
                -- Якщо вчора був останній день навчання, збільшуємо streak
                WHEN last_study_date = (NEW.last_reviewed_at::date - INTERVAL '1 day')::date THEN learning_streak_days + 1
                -- Якщо сьогодні вже навчалися, не змінюємо streak
                WHEN last_study_date = NEW.last_reviewed_at::date THEN learning_streak_days
                -- Інакше починаємо новий streak
                ELSE 1
END,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = NEW.user_id;
END IF;

RETURN NEW;
END;
$$ language 'plpgsql';

-- Застосування тригера до таблиці learning_progress
CREATE TRIGGER update_stats_from_learning_progress
    AFTER INSERT OR UPDATE ON learning_progress
                        FOR EACH ROW EXECUTE FUNCTION update_user_statistics();

-- Процедура для оновлення статистики користувача
CREATE OR REPLACE PROCEDURE update_all_user_statistics()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Оновлення загальної кількості карток
UPDATE user_statistics us
SET total_cards = (
    SELECT COUNT(DISTINCT c.id)
    FROM cards c
             JOIN collections col ON c.collection_id = col.id
    WHERE col.user_id = us.user_id
),
    learned_cards = (
        SELECT COUNT(*)
        FROM learning_progress lp
        WHERE lp.user_id = us.user_id AND lp.learning_status = 'KNOWN'
    ),
    updated_at = CURRENT_TIMESTAMP;

-- Оновлюємо streak для користувачів, які не навчалися сьогодні
UPDATE user_statistics
SET learning_streak_days = 0
WHERE last_study_date < (CURRENT_DATE - INTERVAL '1 day')::date
      AND learning_streak_days > 0;
END;
$$;

-- Тригер для створення запису в user_statistics при реєстрації нового користувача
CREATE OR REPLACE FUNCTION create_user_statistics()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO user_statistics (user_id, total_cards, learned_cards, total_study_time_minutes, learning_streak_days)
VALUES (NEW.id, 0, 0, 0, 0);
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER after_user_insert
    AFTER INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION create_user_statistics();

-- Створення ролей доступу
CREATE ROLE app_readonly;
CREATE ROLE app_user;
CREATE ROLE app_admin;

-- Налаштування прав доступу
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;
GRANT SELECT, INSERT, UPDATE ON users, collections, cards, learning_progress, learning_history, user_statistics, tags, card_tags TO app_user;
GRANT DELETE ON collections, cards, card_tags TO app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO app_admin;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user, app_admin;