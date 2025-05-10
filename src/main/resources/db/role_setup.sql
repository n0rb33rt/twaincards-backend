-- Create database users for each role
CREATE USER twaincards_user WITH PASSWORD 'user_password';
CREATE USER twaincards_premium WITH PASSWORD 'premium_password';
CREATE USER twaincards_admin WITH PASSWORD 'admin_password';

-- Grant basic schema usage permissions to all users
GRANT USAGE ON SCHEMA public TO twaincards_user, twaincards_premium, twaincards_admin;

-- Grant limited permissions to basic user
GRANT SELECT ON public.languages, public.common_words, public.roles TO twaincards_user;
GRANT SELECT, INSERT, UPDATE ON public.learning_history, public.learning_progress TO twaincards_user;
GRANT SELECT, INSERT, UPDATE ON public.study_sessions TO twaincards_user;
GRANT SELECT, INSERT, UPDATE ON public.users TO twaincards_user;

-- Grant row-level security on collections and cards
GRANT SELECT, INSERT, UPDATE, DELETE ON public.collections, public.cards TO twaincards_user;

-- Set up row-level security
ALTER TABLE public.collections ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS collection_access ON public.collections;
CREATE POLICY collection_access ON public.collections 
    FOR ALL
    TO twaincards_user 
    USING (user_id = current_setting('app.user_id', true)::bigint OR is_public = true);

ALTER TABLE public.cards ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS card_access ON public.cards;
CREATE POLICY card_access ON public.cards 
    FOR ALL
    TO twaincards_user 
    USING (collection_id IN (
        SELECT id FROM public.collections 
        WHERE user_id = current_setting('app.user_id', true)::bigint OR is_public = true));

-- Grant premium user permissions (inherit user permissions plus more)
GRANT twaincards_user TO twaincards_premium;
-- Premium has additional data analytics access
GRANT SELECT ON ALL TABLES IN SCHEMA public TO twaincards_premium;

-- Grant admin user permissions (full access)
GRANT twaincards_premium TO twaincards_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO twaincards_admin;

-- Grant function execute permissions
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO twaincards_user, twaincards_premium, twaincards_admin; 