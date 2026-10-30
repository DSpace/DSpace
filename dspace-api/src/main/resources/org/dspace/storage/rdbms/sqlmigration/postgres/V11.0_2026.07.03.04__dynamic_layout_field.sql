--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Field - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (field), V7.0_2020.08.03 (field),
--   V7.0_2020.08.27, V7.0_2021.05.24, V7.0_2021.11.05 (field),
--   V7.0_2021.11.26
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). Columns added by later 7.x migrations are copied
-- only when present; the mandatory "cell" column defaults to 0 when the legacy
-- table predates it. The legacy "style" column (dropped in 7.0_2021.11.26) is
-- ignored.
-------------------------------------------------------

-- Step 1: create the new sequence and table (safe on any database state)
CREATE SEQUENCE IF NOT EXISTS dynamic_layout_field_field_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_field
(
    field_id         INTEGER NOT NULL,
    metadata_field_id INTEGER,
    bundle           VARCHAR(255),
    rendering        VARCHAR(255),
    row              INTEGER NOT NULL,
    priority         INTEGER NOT NULL,
    type             VARCHAR(255),
    label            VARCHAR(255),
    box_id           INTEGER,
    metadata_value   VARCHAR(255),
    style_label      VARCHAR(255),
    style_value      VARCHAR(255),
    label_as_heading BOOLEAN,
    values_inline    BOOLEAN,
    row_style        VARCHAR(255),
    cell_style       VARCHAR(255),
    cell             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT dynamic_layout_field_pkey PRIMARY KEY (field_id),
    CONSTRAINT dynamic_layout_box2metadata_metadata_field_id_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id),
    CONSTRAINT dynamic_layout_field_box_fkey FOREIGN KEY (box_id)
        REFERENCES dynamic_layout_box (id)
);

-- Step 2: migrate data from the legacy cris_layout_field table when it exists.
DO $$
DECLARE
    select_box_id           TEXT := 'NULL';
    select_metadata_value   TEXT := 'NULL';
    select_style_label      TEXT := 'NULL';
    select_style_value      TEXT := 'NULL';
    select_label_as_heading TEXT := 'NULL';
    select_values_inline    TEXT := 'NULL';
    select_row_style        TEXT := 'NULL';
    select_cell_style       TEXT := 'NULL';
    select_cell             TEXT := '0';
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_field') THEN

        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'box_id') THEN
            select_box_id := 'box_id';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'metadata_value') THEN
            select_metadata_value := 'metadata_value';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'style_label') THEN
            select_style_label := 'style_label';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'style_value') THEN
            select_style_value := 'style_value';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'label_as_heading') THEN
            select_label_as_heading := 'label_as_heading';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'values_inline') THEN
            select_values_inline := 'values_inline';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'row_style') THEN
            select_row_style := 'row_style';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'cell_style') THEN
            select_cell_style := 'cell_style';
        END IF;
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public'
                   AND table_name = 'cris_layout_field' AND column_name = 'cell') THEN
            select_cell := 'cell';
        END IF;

        EXECUTE 'INSERT INTO dynamic_layout_field '
             || '(field_id, metadata_field_id, bundle, rendering, row, priority, type, label, '
             || 'box_id, metadata_value, style_label, style_value, label_as_heading, values_inline, '
             || 'row_style, cell_style, cell) '
             || 'SELECT field_id, metadata_field_id, bundle, rendering, row, priority, type, label, '
             || select_box_id || ', ' || select_metadata_value || ', ' || select_style_label || ', '
             || select_style_value || ', ' || select_label_as_heading || ', ' || select_values_inline || ', '
             || select_row_style || ', ' || select_cell_style || ', ' || select_cell || ' '
             || 'FROM cris_layout_field';

        DROP TABLE cris_layout_field CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_field_field_id_seq;

        PERFORM setval('dynamic_layout_field_field_id_seq',
                       COALESCE((SELECT MAX(field_id) FROM dynamic_layout_field), 1));
    END IF;
END $$;
