--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Tab - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (tab), V7.0_2020.08.03 (tab),
--   V7.0_2021.11.05 (tab), V7.6_2023.10.23, V7.6_2023.10.28
--
-- Handles three states idempotently:
--   1. Legacy cris_layout_tab present  -> create dynamic_layout_tab, copy data,
--      drop the legacy table/sequence, sync the new sequence.
--   2. dynamic_layout_tab already present -> no-op (CREATE IF NOT EXISTS).
--   3. Fresh install (neither present)  -> just create dynamic_layout_tab.
-------------------------------------------------------

-- Step 1: create the new sequence and table (safe on any database state)
CREATE SEQUENCE IF NOT EXISTS dynamic_layout_tab_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_tab
(
    id            INTEGER NOT NULL,
    entity_id     INTEGER NOT NULL,
    priority      INTEGER NOT NULL,
    shortname     VARCHAR(255),
    header        VARCHAR(255),
    security      INTEGER,
    is_leading    BOOLEAN,
    custom_filter VARCHAR(255),
    CONSTRAINT dynamic_layout_tab_pkey PRIMARY KEY (id),
    CONSTRAINT dynamic_layout_tab_entity_id_fkey FOREIGN KEY (entity_id)
        REFERENCES entity_type (id),
    CONSTRAINT dynamic_layout_tab_entity_shortname_custom_filter_unique
        UNIQUE (entity_id, shortname, custom_filter)
);

-- Step 2: migrate data from the legacy cris_layout_tab table when it exists.
-- Ids are preserved because downstream layout tables reference them. The
-- is_leading and custom_filter columns were introduced by later 7.x migrations
-- so they are copied only when present, otherwise NULL is stored.
DO $$
DECLARE
    select_is_leading    TEXT := 'NULL';
    select_custom_filter TEXT := 'NULL';
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_tab') THEN

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_tab'
                     AND column_name = 'is_leading') THEN
            select_is_leading := 'is_leading';
        END IF;

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_tab'
                     AND column_name = 'custom_filter') THEN
            select_custom_filter := 'custom_filter';
        END IF;

        EXECUTE 'INSERT INTO dynamic_layout_tab '
             || '(id, entity_id, priority, shortname, header, security, is_leading, custom_filter) '
             || 'SELECT id, entity_id, priority, shortname, header, security, '
             || select_is_leading || ', ' || select_custom_filter || ' '
             || 'FROM cris_layout_tab';

        DROP TABLE cris_layout_tab CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_tab_id_seq;

        PERFORM setval('dynamic_layout_tab_id_seq',
                       COALESCE((SELECT MAX(id) FROM dynamic_layout_tab), 1));
    END IF;
END $$;
