create table public.cards (
                              id bigint primary key not null,
                              back_text character varying(255) not null,
                              created_at timestamp(6) without time zone,
                              example_usage character varying(255),
                              front_text character varying(255) not null,
                              updated_at timestamp(6) without time zone,
                              collection_id bigint not null,
                              foreign key (collection_id) references public.collections (id)
                                  match simple on update no action on delete no action
);

create table public.collection_user_usage (
                                              first_used_at timestamp(6) without time zone,
                                              last_used_at timestamp(6) without time zone,
                                              use_count integer,
                                              collection_id bigint not null,
                                              user_id bigint not null,
                                              primary key (collection_id, user_id),
                                              foreign key (collection_id) references public.collections (id)
                                                  match simple on update no action on delete no action,
                                              foreign key (user_id) references public.users (id)
                                                  match simple on update no action on delete no action
);

create table public.collections (
                                    id bigint primary key not null,
                                    created_at timestamp(6) without time zone,
                                    description character varying(255),
                                    is_public boolean,
                                    name character varying(100) not null,
                                    updated_at timestamp(6) without time zone,
                                    source_language_id bigint not null,
                                    target_language_id bigint not null,
                                    user_id bigint not null,
                                    users_count integer,
                                    foreign key (target_language_id) references public.languages (id)
                                        match simple on update no action on delete no action,
                                    foreign key (source_language_id) references public.languages (id)
                                        match simple on update no action on delete no action,
                                    foreign key (user_id) references public.users (id)
                                        match simple on update no action on delete no action
);
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- repository query
CREATE INDEX idx_collection_name_search ON public.collections
    USING gin (lower(name) gin_trgm_ops, lower(description) gin_trgm_ops);

-- findByTextInCard method
CREATE INDEX idx_card_text_search ON public.cards
    USING gin (lower(front_text) gin_trgm_ops, lower(back_text) gin_trgm_ops);

--  findCardsForReview query
CREATE INDEX idx_learning_progress_user_review ON public.learning_progress
    (user_id, next_review_date);


create index idx_source_text on common_words using btree (source_text);
create index idx_source_language_target_language on common_words using btree (source_language_code, target_language_code);
create unique index uq_source_text_source_lang_target_lang on common_words using btree (source_text, source_language_code, target_language_code);

create table public.languages (
                                  id bigint primary key not null,
                                  code character varying(10) not null,
                                  name character varying(50) not null,
                                  native_name character varying(50)
);
create unique index ukmyc139vxcejowe4q8qm3ca5jn on languages using btree (code);

create table public.learning_history (
                                         id bigint primary key not null,
                                         action_type character varying(20) not null,
                                         is_correct boolean,
                                         performed_at timestamp(6) without time zone,
                                         card_id bigint not null,
                                         study_session_id bigint,
                                         user_id bigint not null,
                                         foreign key (card_id) references public.cards (id)
                                             match simple on update no action on delete no action,
                                         foreign key (study_session_id) references public.study_sessions (id)
                                             match simple on update no action on delete no action,
                                         foreign key (user_id) references public.users (id)
                                             match simple on update no action on delete no action
);

create table public.learning_progress (
                                          id bigint primary key not null,
                                          correct_answers integer,
                                          incorrect_answers integer,
                                          last_reviewed_at timestamp(6) without time zone,
                                          learning_status character varying(20),
                                          next_review_date timestamp(6) without time zone,
                                          repetition_count integer,
                                          card_id bigint not null,
                                          user_id bigint not null,
                                          foreign key (card_id) references public.cards (id)
                                              match simple on update no action on delete no action,
                                          foreign key (user_id) references public.users (id)
                                              match simple on update no action on delete no action
);
create unique index uk4i1i1qcwy35ayngbk1mhk20o8 on learning_progress using btree (user_id, card_id);

create table public.roles (
                              id bigint primary key not null,
                              name character varying(20) not null
);
create unique index ukofx66keruapi6vyqpv6f2or37 on roles using btree (name);

create table public.session_collections (
                                            session_id bigint not null,
                                            collection_id bigint not null,
                                            primary key (session_id, collection_id),
                                            foreign key (session_id) references public.study_sessions (id)
                                                match simple on update no action on delete no action,
                                            foreign key (collection_id) references public.collections (id)
                                                match simple on update no action on delete no action
);

create table public.study_sessions (
                                       id bigint primary key not null,
                                       cards_reviewed integer,
                                       correct_answers integer,
                                       device_type character varying(50),
                                       is_completed boolean,
                                       platform character varying(50),
                                       user_id bigint not null,
                                       session_duration_seconds integer,
                                       end_time timestamp(6) without time zone,
                                       start_time timestamp(6) without time zone,
                                       foreign key (user_id) references public.users (id)
                                           match simple on update no action on delete no action
);

create table public.tokens (
                               id bigint primary key not null,
                               created_at timestamp(6) without time zone not null,
                               expires_at timestamp(6) without time zone not null,
                               token character varying(40) not null,
                               token_type character varying(255) not null,
                               used boolean not null,
                               used_at timestamp(6) without time zone,
                               user_id bigint not null,
                               foreign key (user_id) references public.users (id)
                                   match simple on update no action on delete no action
);
create unique index ukna3v9f8s7ucnj16tylrs822qj on tokens using btree (token);

create table public.user_statistics (
                                        id bigint primary key not null,
                                        last_study_date date,
                                        learned_cards integer,
                                        learning_streak_days integer,
                                        total_cards integer,
                                        updated_at timestamp(6) without time zone,
                                        user_id bigint not null,
                                        cards_in_progress integer,
                                        cards_to_learn integer,
                                        foreign key (user_id) references public.users (id)
                                            match simple on update no action on delete no action
);
create unique index uk14hay547dsmu33phkweirx73y on user_statistics using btree (user_id);

create table public.users (
                              id bigint primary key not null,
                              email character varying(100) not null,
                              first_name character varying(50),
                              is_active boolean,
                              last_login_date timestamp(6) without time zone,
                              last_name character varying(50),
                              password_hash character varying(255) not null,
                              registration_date timestamp(6) without time zone,
                              role character varying(20),
                              username character varying(50) not null,
                              role_id bigint,
                              foreign key (role_id) references public.roles (id)
                                  match simple on update no action on delete no action
);
create unique index uk6dotkott2kjsp8vw4d0m25fb7 on users using btree (email);
create unique index ukr43af9ap4edm43mmtq01oddj6 on users using btree (username);

