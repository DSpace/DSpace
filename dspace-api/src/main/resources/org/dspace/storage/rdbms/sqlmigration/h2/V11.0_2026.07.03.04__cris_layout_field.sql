--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Field - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (field), V7.0_2020.08.03 (field),
--   V7.0_2020.08.27, V7.0_2021.05.24, V7.0_2021.11.05 (field),
--   V7.0_2021.11.26
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS cris_layout_field_field_id_seq;

CREATE TABLE IF NOT EXISTS cris_layout_field
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
    cell             INTEGER NOT NULL,
    CONSTRAINT cris_layout_field_pkey PRIMARY KEY (field_id),
    CONSTRAINT cris_layout_box2metadata_metadata_field_id_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id)
);

ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS box_id INTEGER;
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS metadata_value VARCHAR(255);
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS style_label VARCHAR(255);
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS style_value VARCHAR(255);
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS label_as_heading BOOLEAN;
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS values_inline BOOLEAN;
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS row_style VARCHAR(255);
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS cell_style VARCHAR(255);
ALTER TABLE cris_layout_field ADD COLUMN IF NOT EXISTS cell INTEGER NOT NULL;

ALTER TABLE cris_layout_field DROP COLUMN IF EXISTS style;

ALTER TABLE cris_layout_field ALTER COLUMN metadata_field_id SET NULL;

ALTER TABLE cris_layout_field DROP CONSTRAINT IF EXISTS cris_layout_field_box_fkey;
ALTER TABLE cris_layout_field ADD CONSTRAINT cris_layout_field_box_fkey
    FOREIGN KEY (box_id) REFERENCES cris_layout_box (id);
