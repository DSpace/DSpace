--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Box - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (box), V7.0_2020.08.03 (box),
--   V7.0_2020.12.08 (box), V7.0_2021.11.05 (box), V7.0_2021.11.17
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). The max_columns, cell, position and container
-- columns were added by later 7.x migrations and are copied only when present.
-- The legacy clear and priority columns (dropped by 7.x migrations) are ignored.
-------------------------------------------------------

-- Step 1: create the new sequence and table (safe on any database state)
CREATE SEQUENCE IF NOT EXISTS dynamic_layout_box_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_box
(
    id          INTEGER NOT NULL,
    entity_id   INTEGER NOT NULL,
    type        VARCHAR(255),
    collapsed   BOOLEAN NOT NULL,
    shortname   VARCHAR(255),
    header      VARCHAR(255),
    minor       BOOLEAN NOT NULL,
    security    INTEGER,
    style       VARCHAR(255),
    max_columns INTEGER,
    cell        INTEGER,
    position    INTEGER,
    container   BOOLEAN,
    CONSTRAINT dynamic_layout_box_pkey PRIMARY KEY (id),
    CONSTRAINT dynamic_layout_box_entity_id_fkey FOREIGN KEY (entity_id)
        REFERENCES entity_type (id),
    CONSTRAINT dynamic_layout_cell_id_fk FOREIGN KEY (cell)
        REFERENCES dynamic_layout_cell
);

-- Step 2: migrate data from the legacy cris_layout_box table when it exists.
DO $$
DECLARE
    select_max_columns TEXT := 'NULL';
    select_cell        TEXT := 'NULL';
    select_position    TEXT := 'NULL';
    select_container   TEXT := 'NULL';
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_box') THEN

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_box'
                     AND column_name = 'max_columns') THEN
            select_max_columns := 'max_columns';
        END IF;

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_box'
                     AND column_name = 'cell') THEN
            select_cell := 'cell';
        END IF;

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_box'
                     AND column_name = 'position') THEN
            select_position := 'position';
        END IF;

        IF EXISTS (SELECT FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'cris_layout_box'
                     AND column_name = 'container') THEN
            select_container := 'container';
        END IF;

        EXECUTE 'INSERT INTO dynamic_layout_box '
             || '(id, entity_id, type, collapsed, shortname, header, minor, security, style, '
             || 'max_columns, cell, position, container) '
             || 'SELECT id, entity_id, type, collapsed, shortname, header, minor, security, style, '
             || select_max_columns || ', ' || select_cell || ', '
             || select_position || ', ' || select_container || ' '
             || 'FROM cris_layout_box';

        DROP TABLE cris_layout_box CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_box_id_seq;

        PERFORM setval('dynamic_layout_box_id_seq',
                       COALESCE((SELECT MAX(id) FROM dynamic_layout_box), 1));
    END IF;
END $$;
