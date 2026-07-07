--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Field 2 Nested - squashed idempotent migration
-- Consolidates: V7.0_2021.03.25, V7.0_2021.11.05 (nested),
--   V7.0_2021.11.26 (nested)
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS cris_layout_field_nested_id_seq;

CREATE TABLE IF NOT EXISTS cris_layout_field2nested
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
    CONSTRAINT cris_layout_field2nested_pkey PRIMARY KEY (nested_field_id),
    CONSTRAINT cris_layout_field2nested_metadatafieldregistry_fkey
        FOREIGN KEY (metadata_field_id) REFERENCES metadatafieldregistry (metadata_field_id),
    CONSTRAINT cris_layout_field2nested_cris_layout_field_fkey
        FOREIGN KEY (field_id) REFERENCES cris_layout_field (field_id)
);

ALTER TABLE cris_layout_field2nested DROP COLUMN IF EXISTS label_as_heading;
ALTER TABLE cris_layout_field2nested DROP COLUMN IF EXISTS values_inline;
