--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Row and Cell - squashed idempotent migration
-- Consolidates: V7.0_2021.11.05 (row + cell)
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). Rows are migrated before cells so the cell.row
-- foreign key to dynamic_layout_row is always satisfiable, and ids are kept
-- unchanged so downstream references stay valid.
-------------------------------------------------------

-- Step 1: create the new sequences and tables (safe on any database state)
CREATE SEQUENCE IF NOT EXISTS dynamic_layout_row_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_row
(
    id       INTEGER NOT NULL,
    style    VARCHAR(255),
    tab      INTEGER NOT NULL,
    position INTEGER,
    CONSTRAINT dynamic_layout_row_pkey PRIMARY KEY (id),
    CONSTRAINT dynamic_layout_tab_fkey FOREIGN KEY (tab) REFERENCES dynamic_layout_tab (id)
);

CREATE SEQUENCE IF NOT EXISTS dynamic_layout_cell_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_cell
(
    id       INTEGER NOT NULL,
    style    VARCHAR(255),
    row      INTEGER NOT NULL,
    position INTEGER,
    CONSTRAINT dynamic_layout_cell_pkey PRIMARY KEY (id),
    CONSTRAINT dynamic_layout_row_fkey FOREIGN KEY (row) REFERENCES dynamic_layout_row (id)
);

-- Step 2: migrate data from the legacy cris_layout_row / cris_layout_cell tables.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_row') THEN
        INSERT INTO dynamic_layout_row (id, style, tab, position)
        SELECT id, style, tab, position FROM cris_layout_row;

        DROP TABLE cris_layout_row CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_row_id_seq;

        PERFORM setval('dynamic_layout_row_id_seq',
                       COALESCE((SELECT MAX(id) FROM dynamic_layout_row), 1));
    END IF;

    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_cell') THEN
        INSERT INTO dynamic_layout_cell (id, style, row, position)
        SELECT id, style, row, position FROM cris_layout_cell;

        DROP TABLE cris_layout_cell CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_cell_id_seq;

        PERFORM setval('dynamic_layout_cell_id_seq',
                       COALESCE((SELECT MAX(id) FROM dynamic_layout_cell), 1));
    END IF;
END $$;
