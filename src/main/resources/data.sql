-- First add configuration for custom paramaters
SET LOCAL client_min_messages = warning;
DO $$
BEGIN
    EXECUTE 'ALTER DATABASE ' || current_database() || ' SET app.user_id = 0';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Could not set app.user_id parameter, might be set already';
END $$;

CREATE OR REPLACE FUNCTION update_user_stats_on_learning_history()
    RETURNS TRIGGER AS $$
DECLARE
    last_study DATE;
BEGIN

    SELECT last_study_date INTO last_study FROM user_statistics WHERE user_id = NEW.user_id;

    INSERT INTO user_statistics (user_id, last_study_date, learned_cards, learning_streak_days, total_cards)
    VALUES (NEW.user_id, CURRENT_DATE, 0,
            CASE
                WHEN last_study = CURRENT_DATE - INTERVAL '1 day' THEN 1
                WHEN last_study = CURRENT_DATE THEN 0
                ELSE 0
                END,
            0)
    ON CONFLICT (user_id)
        DO UPDATE SET
                      last_study_date = CURRENT_DATE,
                      learning_streak_days =
                          CASE
                              WHEN user_statistics.last_study_date = CURRENT_DATE THEN user_statistics.learning_streak_days
                              WHEN user_statistics.last_study_date = CURRENT_DATE - INTERVAL '1 day' THEN user_statistics.learning_streak_days + 1
                              ELSE 1
                              END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_learning_history_insert
    AFTER INSERT ON learning_history
    FOR EACH ROW
    EXECUTE FUNCTION update_user_stats_on_learning_history();



CREATE OR REPLACE FUNCTION update_card_stats_on_insert()
    RETURNS TRIGGER AS $$
DECLARE
    collection_owner_id BIGINT;
BEGIN

    SELECT user_id INTO collection_owner_id
    FROM collections
    WHERE id = NEW.collection_id;


    UPDATE user_statistics
    SET
        total_cards = total_cards + 1,
        cards_to_learn = cards_to_learn + 1
    WHERE user_id = collection_owner_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS after_card_insert ON cards;
DROP TRIGGER IF EXISTS after_card_insert_update_to_learn ON cards;

CREATE TRIGGER after_card_insert
    AFTER INSERT ON cards
    FOR EACH ROW
EXECUTE FUNCTION update_card_stats_on_insert();

-- Insert roles
INSERT INTO roles (name, description) 
VALUES ('USER', 'Regular user with limited access - 30 cards per day') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) 
VALUES ('PREMIUM', 'Premium user with unlimited cards per day') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) 
VALUES ('ADMIN', 'Administrator with full system access') 
ON CONFLICT (name) DO NOTHING;
