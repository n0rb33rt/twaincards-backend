-- Comprehensive Full Permissions for All User Roles

-- Function to revoke all existing permissions
CREATE OR REPLACE FUNCTION revoke_all_permissions() RETURNS VOID AS $$
DECLARE
    r RECORD;
BEGIN
    -- Revoke all existing permissions from all tables
    FOR r IN (
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = current_schema()
    ) LOOP
            EXECUTE 'REVOKE ALL ON ' || quote_ident(r.tablename) || ' FROM PUBLIC';
            EXECUTE 'REVOKE ALL ON ' || quote_ident(r.tablename) || ' FROM twaincards_user';
            EXECUTE 'REVOKE ALL ON ' || quote_ident(r.tablename) || ' FROM twaincards_premium';
            EXECUTE 'REVOKE ALL ON ' || quote_ident(r.tablename) || ' FROM twaincards_admin';
        END LOOP;

    -- Revoke all existing permissions from all sequences
    FOR r IN (
        SELECT sequencename
        FROM pg_sequences
        WHERE schemaname = current_schema()
    ) LOOP
            EXECUTE 'REVOKE ALL ON SEQUENCE ' || quote_ident(r.sequencename) || ' FROM PUBLIC';
            EXECUTE 'REVOKE ALL ON SEQUENCE ' || quote_ident(r.sequencename) || ' FROM twaincards_user';
            EXECUTE 'REVOKE ALL ON SEQUENCE ' || quote_ident(r.sequencename) || ' FROM twaincards_premium';
            EXECUTE 'REVOKE ALL ON SEQUENCE ' || quote_ident(r.sequencename) || ' FROM twaincards_admin';
        END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Execute the revoke function
SELECT revoke_all_permissions();

-- Disable all existing Row Level Security policies
DO $$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN (
            SELECT tablename
            FROM pg_tables
            WHERE schemaname = current_schema()
        ) LOOP
                BEGIN
                    EXECUTE 'ALTER TABLE ' || quote_ident(r.tablename) || ' DISABLE ROW LEVEL SECURITY';
                EXCEPTION WHEN OTHERS THEN
                    RAISE NOTICE 'Could not disable RLS on %', r.tablename;
                END;
            END LOOP;
    END $$;

-- Global Full Permissions Setup
\echo 'Granting FULL permissions to ALL user roles...'

-- Grant FULL permissions to ALL roles on ALL tables
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_premium;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_admin;

-- Grant FULL permissions on ALL sequences
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO twaincards_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO twaincards_premium;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO twaincards_admin;

-- Future table and sequence permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES ON TABLES TO twaincards_user, twaincards_premium, twaincards_admin;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES ON SEQUENCES TO twaincards_user, twaincards_premium, twaincards_admin;

-- Optional: Create a universal access policy for all tables
DO $$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN (
            SELECT tablename
            FROM pg_tables
            WHERE schemaname = current_schema()
        ) LOOP
                BEGIN
                    -- Skip certain system or configuration tables
                    IF r.tablename NOT IN ('pg_settings', 'pg_roles') THEN
                        EXECUTE 'ALTER TABLE ' || quote_ident(r.tablename) || ' ENABLE ROW LEVEL SECURITY';

                        -- Create a universal access policy
                        EXECUTE 'CREATE POLICY universal_access_' || r.tablename || ' ON ' || quote_ident(r.tablename) || '
                    FOR ALL
                    TO twaincards_user, twaincards_premium, twaincards_admin
                    USING (true)
                    WITH CHECK (true)';
                    END IF;
                EXCEPTION WHEN OTHERS THEN
                    RAISE NOTICE 'Could not set universal access on %', r.tablename;
                END;
            END LOOP;
    END $$;

\echo 'FULL Permissions setup complete for ALL user roles!'

-- IMPORTANT WARNING
\echo '!!! WARNING: This configuration provides FULL access to ALL users on ALL tables !!!'
\echo '!!! USE WITH EXTREME CAUTION IN PRODUCTION ENVIRONMENTS !!!'
\echo '!!! RECOMMENDED ONLY FOR DEVELOPMENT OR TESTING PURPOSES !!!'

-- Create a trigger function to track collection usage that works with the many-to-many relationship
CREATE OR REPLACE FUNCTION track_collection_usage_from_session_collections()
RETURNS TRIGGER AS $$
BEGIN
    -- Get the user_id from the study session
    DECLARE
        session_user_id BIGINT;
    BEGIN
        SELECT user_id INTO session_user_id 
        FROM study_sessions 
        WHERE id = NEW.session_id;
        
        -- Insert or update the collection usage record
        INSERT INTO collection_user_usage (collection_id, user_id, first_used_at, last_used_at, use_count)
        VALUES (NEW.collection_id, session_user_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
        ON CONFLICT (collection_id, user_id)
        DO UPDATE SET last_used_at = CURRENT_TIMESTAMP, use_count = collection_user_usage.use_count + 1;

        -- Update the collection's users_count if this is the first time this user used this collection
        WITH new_usage AS (
            SELECT collection_id, user_id,
                  (use_count = 1 AND first_used_at = last_used_at) AS is_first_time
            FROM collection_user_usage
            WHERE collection_id = NEW.collection_id AND user_id = session_user_id
        )
        UPDATE collections c
        SET users_count = users_count +
            CASE WHEN (SELECT is_first_time FROM new_usage) THEN 1 ELSE 0 END
        WHERE c.id = NEW.collection_id;
    END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop any existing trigger first
DROP TRIGGER IF EXISTS track_collection_usage_from_join_trigger ON session_collections;

-- Add trigger to session_collections table for tracking usage
CREATE TRIGGER track_collection_usage_from_join_trigger
AFTER INSERT ON session_collections
FOR EACH ROW
EXECUTE FUNCTION track_collection_usage_from_session_collections();