--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- Dynamic Layout Security Metadata - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (securityfield),
--   V7.0_2020.08.03 (securitymetadata)
--
-- Handles three states idempotently (legacy cris_* present / dynamic_* already
-- present / fresh install). Two legacy names may exist depending on how far the
-- original 7.x migrations progressed:
--   * cris_layout_box2securitymetadata / cris_layout_tab2securitymetadata
--     (post 7.0_2020.08.03, column metadata_field_id)
--   * cris_layout_box2securityfield / cris_layout_tab2securityfield
--     (pre 7.0_2020.08.03, column authorized_field_id)
-- Whichever exists is copied into the new tables, mapping authorized_field_id to
-- metadata_field_id, then all legacy variants are dropped.
-------------------------------------------------------

-- Step 1: create the new tables (safe on any database state)
CREATE TABLE IF NOT EXISTS dynamic_layout_box2securitymetadata
(
    box_id            INTEGER NOT NULL,
    metadata_field_id INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_box2securitymetadata_pkey PRIMARY KEY (metadata_field_id, box_id),
    CONSTRAINT dynamic_layout_box2securitymetadata_box_id_fkey
        FOREIGN KEY (box_id) REFERENCES dynamic_layout_box (id),
    CONSTRAINT dynamic_layout_box2securitymetadata_field_id_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id)
);

CREATE TABLE IF NOT EXISTS dynamic_layout_tab2securitymetadata
(
    tab_id            INTEGER NOT NULL,
    metadata_field_id INTEGER NOT NULL,
    CONSTRAINT dynamic_layout_tab2securitymetadata_pkey PRIMARY KEY (metadata_field_id, tab_id),
    CONSTRAINT dynamic_layout_tab2securitymetadata_tab_id_fkey
        FOREIGN KEY (tab_id) REFERENCES dynamic_layout_tab (id),
    CONSTRAINT dynamic_layout_tab2securitymetadata_field_id_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id)
);

-- Step 2: migrate data from whichever legacy variant exists.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_box2securitymetadata') THEN
        INSERT INTO dynamic_layout_box2securitymetadata (box_id, metadata_field_id)
        SELECT box_id, metadata_field_id FROM cris_layout_box2securitymetadata;
    ELSIF EXISTS (SELECT FROM pg_catalog.pg_tables
                  WHERE schemaname = 'public' AND tablename = 'cris_layout_box2securityfield') THEN
        INSERT INTO dynamic_layout_box2securitymetadata (box_id, metadata_field_id)
        SELECT box_id, authorized_field_id FROM cris_layout_box2securityfield;
    END IF;

    IF EXISTS (SELECT FROM pg_catalog.pg_tables
               WHERE schemaname = 'public' AND tablename = 'cris_layout_tab2securitymetadata') THEN
        INSERT INTO dynamic_layout_tab2securitymetadata (tab_id, metadata_field_id)
        SELECT tab_id, metadata_field_id FROM cris_layout_tab2securitymetadata;
    ELSIF EXISTS (SELECT FROM pg_catalog.pg_tables
                  WHERE schemaname = 'public' AND tablename = 'cris_layout_tab2securityfield') THEN
        INSERT INTO dynamic_layout_tab2securitymetadata (tab_id, metadata_field_id)
        SELECT tab_id, authorized_field_id FROM cris_layout_tab2securityfield;
    END IF;
END $$;

-- Step 3: drop every legacy variant (also corrects a previous erroneous
-- dynamic_ prefix on the *securityfield drops). Safe no-op when absent.
DROP TABLE IF EXISTS cris_layout_box2securityfield;
DROP TABLE IF EXISTS cris_layout_box2securitymetadata;
DROP TABLE IF EXISTS cris_layout_tab2securityfield;
DROP TABLE IF EXISTS cris_layout_tab2securitymetadata;
