-- Create the database roles
CREATE ROLE twaincards_user;
CREATE ROLE twaincards_premium;
CREATE ROLE twaincards_admin;

-- Set up role inheritance (premium inherits from user, admin inherits from premium)
GRANT twaincards_user TO twaincards_premium;
GRANT twaincards_premium TO twaincards_admin;

-- Grant basic permissions to twaincards_user
-- Basic read access to general tables
GRANT SELECT ON public.languages TO twaincards_user;
GRANT SELECT ON public.common_words TO twaincards_user;

-- Allow users to manage their own data
GRANT SELECT, INSERT, UPDATE, DELETE ON public.users TO twaincards_user;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
CREATE POLICY user_self_access ON public.users
    USING (id = current_setting('app.user_id')::bigint);

-- Allow access to collections (own or public)
GRANT SELECT, INSERT, UPDATE, DELETE ON public.collections TO twaincards_user;
ALTER TABLE public.collections ENABLE ROW LEVEL SECURITY;
CREATE POLICY collection_access ON public.collections
    USING (user_id = current_setting('app.user_id')::bigint OR is_public = true);
CREATE POLICY collection_modify ON public.collections
    FOR INSERT OR UPDATE OR DELETE
    USING (user_id = current_setting('app.user_id')::bigint);

-- Grant access to cards in accessible collections
GRANT SELECT, INSERT, UPDATE, DELETE ON public.cards TO twaincards_user;
ALTER TABLE public.cards ENABLE ROW LEVEL SECURITY;
CREATE POLICY card_access ON public.cards
    USING (collection_id IN (
    SELECT id FROM public.collections
    WHERE user_id = current_setting('app.user_id')::bigint OR is_public = true
));
CREATE POLICY card_modify ON public.cards
    FOR INSERT OR UPDATE OR DELETE
    USING (collection_id IN (
        SELECT id FROM public.collections
        WHERE user_id = current_setting('app.user_id')::bigint
    ));

-- For learning history, study sessions, etc.
GRANT SELECT, INSERT, UPDATE ON public.learning_history TO twaincards_user;
ALTER TABLE public.learning_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY learning_history_access ON public.learning_history
    USING (user_id = current_setting('app.user_id')::bigint);

GRANT SELECT, INSERT, UPDATE ON public.study_sessions TO twaincards_user;
ALTER TABLE public.study_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY study_session_access ON public.study_sessions
    USING (user_id = current_setting('app.user_id')::bigint);

-- Additional privileges for premium users (not many additional DB privileges needed
-- since the limit enforcement is in application code)
-- Could add special tables or functionality exclusive to premium users

-- Admin privileges - full access to manage all data
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO twaincards_admin;

-- Add comments explaining roles
COMMENT ON ROLE twaincards_user IS 'Regular users - limited to 30 cards per day';
COMMENT ON ROLE twaincards_premium IS 'Premium users - unlimited card studying capabilities';
COMMENT ON ROLE twaincards_admin IS 'Admin users - full management capabilities';