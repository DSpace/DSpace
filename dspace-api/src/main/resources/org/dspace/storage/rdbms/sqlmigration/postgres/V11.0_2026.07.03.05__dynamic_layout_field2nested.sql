--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Field 2 Nested - squashed idempotent migration
-- Consolidates: V7.0_2021.03.25, V7.0_2021.11.05 (nested),
--   V7.0_2021.11.26 (nested)
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). All target columns exist since table creation, so a
-- plain column-projected copy is used. The transient label_as_heading /
-- values_inline columns (added then dropped by 7.x) are intentionally not
-- copied.
-------------------------------------------------------

-- Step 1: create the new sequence and table (safe on any database state)
CREATE SEQUENCE IF NOT EXISTS dynamic_layout_field_nested_id_seq;

CREATE TABLE IF NOT EXISTS dynamic_layout_field2nested
(
    nested_field_id  INTEGER NOT NULL,
    rendering        VARCHAR(255),
    priority         INTEGER NOT NULL,
    label            VARCHAR(255),
    style            VARCHAR(255),
    style_label      VARCHAR(255),
    style_value      VARCHAR(255),
    metadata_field_id INTEGER NOT NULL,
    field_id         INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_field2nested_pkey PRIMARY KEY (nested_field_id),
    CONSTRAINT dynamic_layout_field2nested_metadatafieldregistry_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id),
    CONSTRAINT dynamic_layout_field2nested_dynamic_layout_field_fkey
        FOREIGN KEY (field_id) REFERENCES dynamic_layout_field (field_id)
);

-- Step 2: migrate data from the legacy cris_layout_field2nested table when present.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_field2nested') THEN
        INSERT INTO dynamic_layout_field2nested
            (nested_field_id, rendering, priority, label, style, style_label,
             style_value, metadata_field_id, field_id)
        SELECT nested_field_id, rendering, priority, label, style, style_label,
               style_value, metadata_field_id, field_id
        FROM cris_layout_field2nested;

        DROP TABLE cris_layout_field2nested CASCADE;
        DROP SEQUENCE IF EXISTS cris_layout_field_nested_id_seq;

        PERFORM setval('dynamic_layout_field_nested_id_seq',
                       COALESCE((SELECT MAX(nested_field_id) FROM dynamic_layout_field2nested), 1));
    END IF;
END $$;
