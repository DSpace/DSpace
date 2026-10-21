--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Security Groups - squashed idempotent migration
-- Consolidates: V7.2_2022.05.06, V7.2_2022.05.08,
--   V7.6_2023.12.12, V7.6_2023.12.13
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). The alternative_box_id / alternative_tab_id columns
-- were added by later 7.x migrations and are copied only when present.
-------------------------------------------------------

-- Step 1: create the new tables (safe on any database state)
CREATE TABLE IF NOT EXISTS dynamic_layout_box2securitygroup
(
    box_id             INTEGER NOT NULL,
    group_id           UUID NOT NULL,
    alternative_box_id INTEGER,
    CONSTRAINT dynamic_layout_box2securitygroup_pkey PRIMARY KEY (box_id, group_id),
    CONSTRAINT dynamic_layout_box2securitygroup_box_id
        FOREIGN KEY (box_id) REFERENCES dynamic_layout_box (id),
    CONSTRAINT dynamic_layout_box2securitygroup_group_id
        FOREIGN KEY (group_id) REFERENCES epersongroup (uuid),
    CONSTRAINT dynamic_layout_box2securitygroup_box_id2
        FOREIGN KEY (alternative_box_id) REFERENCES dynamic_layout_box (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS dynamic_layout_tab2securitygroup
(
    tab_id             INTEGER NOT NULL,
    group_id           UUID NOT NULL,
    alternative_tab_id INTEGER,
    CONSTRAINT dynamic_layout_tab2securitygroup_pkey PRIMARY KEY (tab_id, group_id),
    CONSTRAINT dynamic_layout_tab2securitygroup_tab_id
        FOREIGN KEY (tab_id) REFERENCES dynamic_layout_tab (id),
    CONSTRAINT dynamic_layout_tab2securitygroup_group_id
        FOREIGN KEY (group_id) REFERENCES epersongroup (uuid),
    CONSTRAINT dynamic_layout_tab2securitygroup_tab_id2
        FOREIGN KEY (alternative_tab_id) REFERENCES dynamic_layout_tab (id) ON DELETE SET NULL
);

-- Step 2: migrate data from the legacy cris_layout_*2securitygroup tables.
DO $$
DECLARE
    select_alternative_box_id TEXT := 'NULL';
    select_alternative_tab_id TEXT := 'NULL';
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_box2securitygroup') THEN
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_box2securitygroup'
                   AND column_name = 'alternative_box_id') THEN
            select_alternative_box_id := 'alternative_box_id';
        END IF;

        EXECUTE 'INSERT INTO dynamic_layout_box2securitygroup (box_id, group_id, alternative_box_id) '
             || 'SELECT box_id, group_id, ' || select_alternative_box_id || ' '
             || 'FROM cris_layout_box2securitygroup';

        DROP TABLE cris_layout_box2securitygroup CASCADE;
    END IF;

    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_tab2securitygroup') THEN
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_tab2securitygroup'
                   AND column_name = 'alternative_tab_id') THEN
            select_alternative_tab_id := 'alternative_tab_id';
        END IF;

        EXECUTE 'INSERT INTO dynamic_layout_tab2securitygroup (tab_id, group_id, alternative_tab_id) '
             || 'SELECT tab_id, group_id, ' || select_alternative_tab_id || ' '
             || 'FROM cris_layout_tab2securitygroup';

        DROP TABLE cris_layout_tab2securitygroup CASCADE;
    END IF;
END $$;
