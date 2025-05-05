-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Update user table if needed (assuming it exists)
ALTER TABLE users DROP COLUMN IF EXISTS role;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles(id);

CREATE INDEX idx_collection_is_public ON collections(is_public);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_collection_name_trigram ON collections USING GIN (name gin_trgm_ops);
CREATE INDEX idx_collection_description_trigram ON collections USING GIN (description gin_trgm_ops);

CREATE INDEX idx_card_collection_id ON cards(collection_id);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

--    DESCRIBE USE TO CHECK ON cards(collection_id);
CREATE INDEX idx_card_front_text_trigram ON cards USING GIN (lower(front_text) gin_trgm_ops);
CREATE INDEX idx_card_back_text_trigram ON cards USING GIN (lower(back_text) gin_trgm_ops);

-- Create custom parameter for row-level security
CREATE EXTENSION IF NOT EXISTS "plpgsql";

DO $$
BEGIN
    -- Check if the parameter already exists
    IF NOT EXISTS (
        SELECT 1 FROM pg_settings WHERE name = 'app.user_id'
    ) THEN
        -- Add the custom parameter for storing current user ID
        EXECUTE 'ALTER DATABASE ' || current_database() || ' SET app.user_id = -1';
    END IF;
END
$$;

-- Set up row-level security for cards table
ALTER TABLE cards ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY cards_user_access ON cards
  USING (
    -- Users can see cards from their own collections
    collection_id IN (SELECT id FROM collections WHERE user_id = current_setting('app.user_id', TRUE)::bigint)
    OR 
    -- Users can see cards from public collections
    collection_id IN (SELECT id FROM collections WHERE is_public = TRUE)
  );

-- Create admin policy that has access to all cards
CREATE POLICY cards_admin_access ON cards
  USING (
    -- Check if current database user is the admin user
    current_user = 'twaincards_admin'
  );

-- Grant permissions to different database roles
GRANT SELECT, INSERT, UPDATE, DELETE ON cards TO twaincards_admin;
GRANT SELECT ON cards TO twaincards_user, twaincards_premium;
GRANT INSERT, UPDATE, DELETE ON cards TO twaincards_premium;

-- Set up RLS for collections table too
ALTER TABLE collections ENABLE ROW LEVEL SECURITY;

CREATE POLICY collections_user_access ON collections
  USING (
    -- Users can see their own collections
    user_id = current_setting('app.user_id', TRUE)::bigint
    OR 
    -- Users can see public collections
    is_public = TRUE
  );

CREATE POLICY collections_admin_access ON collections
  USING (
    current_user = 'twaincards_admin'
  );

GRANT SELECT, INSERT, UPDATE, DELETE ON collections TO twaincards_admin;
GRANT SELECT ON collections TO twaincards_user, twaincards_premium;
GRANT INSERT, UPDATE, DELETE ON collections TO twaincards_premium;

-- Allow the app.user_id parameter to be used in policies
ALTER DATABASE CURRENT SET session_preload_libraries = 'auto_explain';

