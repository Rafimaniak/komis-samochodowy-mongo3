-- INSERT INTO samochod (marka, model, rok_produkcji, cena, kolor, przebieg, status) VALUES
--                                                                                       ('BMW', 'Seria 3', 2020, 120000.00, 'Czarny', 50000, 'DOSTEPNY'),
--                                                                                       ('Audi', 'A4', 2019, 95000.00, 'Biały', 60000, 'DOSTEPNY'),
--                                                                                       ('Mercedes', 'Klasa C', 2021, 150000.00, 'Srebrny', 30000, 'SPRZEDANY'),
--                                                                                       ('Volkswagen', 'Golf', 2018, 65000.00, 'Czerwony', 80000, 'DOSTEPNY');
-- Tabela użytkowników
CREATE TABLE public.users (
                              id bigint NOT NULL,
                              username character varying(50) NOT NULL,
                              email character varying(100) NOT NULL,
                              password character varying(255) NOT NULL,
                              role character varying(20) DEFAULT 'USER',
                              enabled boolean DEFAULT true,
                              created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
                              klient_id bigint,
                              CONSTRAINT users_pkey PRIMARY KEY (id),
                              CONSTRAINT users_username_key UNIQUE (username),
                              CONSTRAINT users_email_key UNIQUE (email),
                              CONSTRAINT fk_users_klient FOREIGN KEY (klient_id) REFERENCES public.klient(id)
);

-- Sekwencja
CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

-- Dodaj domyślnego admina (hasło: admin)
-- Dodaj domyślnego admina (hasło: admin)
INSERT INTO public.users (username, email, password, role, enabled) VALUES (
                                                                               'admin',
                                                                               'admin@komis.pl',
                                                                               '$2a$10$8.UnVuG9HHgffUDAlk8qfOuW2.4A3L7d3K3j6M3dMvR4n1pJQ1qW.',
                                                                               'ADMIN',
                                                                               true
                                                                           ) ON CONFLICT (username) DO NOTHING;

-- Dodaj domyślnego usera (hasło: user)
INSERT INTO public.users (username, email, password, role, enabled) VALUES (
                                                                               'user',
                                                                               'user@komis.pl',
                                                                               '$2a$10$8.UnVuG9HHgffUDAlk8qfOuW2.4A3L7d3K3j6M3dMvR4n1pJQ1qW.',
                                                                               'USER',
                                                                               true
                                                                           ) ON CONFLICT (username) DO NOTHING;