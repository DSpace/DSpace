--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Box - squashed idempotent migration
-- Consolidates: V7.0_2020.05.06 (box), V7.0_2020.08.03 (box),
--   V7.0_2020.12.08 (box), V7.0_2021.11.05 (box), V7.0_2021.11.17
-------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS cris_layout_box_id_seq;

CREATE TABLE IF NOT EXISTS cris_layout_box
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
    CONSTRAINT cris_layout_box_pkey PRIMARY KEY (id),
    CONSTRAINT cris_layout_box_entity_id_fkey FOREIGN KEY (entity_id)
        REFERENCES entity_type (id),
    CONSTRAINT cris_layout_cell_id_fk FOREIGN KEY (cell)
        REFERENCES cris_layout_cell
);
