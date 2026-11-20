--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Security Metadata - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (securityfield),
--   V7.0_2020.08.03 (securitymetadata)
-------------------------------------------------------

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

DROP TABLE IF EXISTS dynamic_layout_box2securityfield;
DROP TABLE IF EXISTS dynamic_layout_tab2securityfield;
