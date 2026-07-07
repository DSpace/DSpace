--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Tab - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (tab), V7.0_2020.08.03 (tab),
--   V7.0_2021.11.05 (tab), V7.6_2023.10.23, V7.6_2023.10.28
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS cris_layout_tab_id_seq;

CREATE TABLE IF NOT EXISTS cris_layout_tab
(
    id            INTEGER NOT NULL,
    entity_id     INTEGER NOT NULL,
    priority      INTEGER NOT NULL,
    shortname     VARCHAR(255),
    header        VARCHAR(255),
    security      INTEGER,
    is_leading    BOOLEAN,
    custom_filter VARCHAR(255),
    CONSTRAINT cris_layout_tab_pkey PRIMARY KEY (id),
    CONSTRAINT cris_layout_tab_entity_id_fkey FOREIGN KEY (entity_id)
    REFERENCES entity_type (id),
    CONSTRAINT cris_layout_tab_entity_shortname_custom_filter_unique
    UNIQUE (entity_id, shortname, custom_filter)
);
