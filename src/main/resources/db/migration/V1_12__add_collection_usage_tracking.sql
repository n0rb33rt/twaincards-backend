
ALTER TABLE collections ADD COLUMN users_count INT DEFAULT 0 NOT NULL;

CREATE TABLE collection_user_usage (
    collection_id INT NOT NULL,
    user_id INT NOT NULL,
    first_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    use_count INT DEFAULT 1 NOT NULL,
    PRIMARY KEY (collection_id, user_id),
    CONSTRAINT fk_collection_usage_collection_id FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_usage_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_collection_user_usage_collection_id ON collection_user_usage(collection_id);
CREATE INDEX idx_collection_user_usage_user_id ON collection_user_usage(user_id);

CREATE OR REPLACE FUNCTION track_collection_usage()
RETURNS TRIGGER AS $$
BEGIN

    IF TG_OP = 'INSERT' THEN

        INSERT INTO collection_user_usage (collection_id, user_id, first_used_at, last_used_at, use_count)
        VALUES (NEW.collection_id, NEW.user_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
        ON CONFLICT (collection_id, user_id)
        DO UPDATE SET last_used_at = CURRENT_TIMESTAMP, use_count = collection_user_usage.use_count + 1;

        WITH new_usage AS (
            SELECT collection_id, user_id,
                   (use_count = 1 AND first_used_at = last_used_at) AS is_first_time
            FROM collection_user_usage
            WHERE collection_id = NEW.collection_id AND user_id = NEW.user_id
        )
        UPDATE collections c
        SET users_count = users_count +
            CASE WHEN (SELECT is_first_time FROM new_usage) THEN 1 ELSE 0 END
        WHERE c.id = NEW.collection_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_collection_usage_trigger
AFTER INSERT ON study_sessions
FOR EACH ROW
EXECUTE FUNCTION track_collection_usage();

