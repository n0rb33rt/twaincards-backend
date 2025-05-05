
CREATE USER twaincards_user WITH PASSWORD 'user_password';
CREATE USER twaincards_premium WITH PASSWORD 'premium_password';
CREATE USER twaincards_admin WITH PASSWORD 'admin_password';


GRANT USAGE ON SCHEMA public TO twaincards_user, twaincards_premium, twaincards_admin;


GRANT SELECT ON public.languages, public.common_words TO twaincards_user;
GRANT SELECT, INSERT, UPDATE ON public.learning_history, public.learning_progress TO twaincards_user;
GRANT SELECT, INSERT, UPDATE ON public.study_sessions TO twaincards_user;
GRANT SELECT ON public.roles TO twaincards_user;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.collections, public.cards TO twaincards_user;


ALTER TABLE public.collections ENABLE ROW LEVEL SECURITY;
CREATE POLICY collection_access ON public.collections
    FOR ALL
    TO twaincards_user
    USING (user_id = current_setting('app.user_id')::bigint OR is_public = true);

ALTER TABLE public.cards ENABLE ROW LEVEL SECURITY;
CREATE POLICY card_access ON public.cards
    FOR ALL
    TO twaincards_user
    USING (collection_id IN (
        SELECT id FROM public.collections
        WHERE user_id = current_setting('app.user_id')::bigint OR is_public = true));


GRANT twaincards_user TO twaincards_premium;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO twaincards_premium;


GRANT twaincards_premium TO twaincards_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO twaincards_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO twaincards_admin;