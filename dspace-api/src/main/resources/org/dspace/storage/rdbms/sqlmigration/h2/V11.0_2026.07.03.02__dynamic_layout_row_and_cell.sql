--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------
-- CRIS Layout Row and Cell - squashed idempotent migration
-- Consolidates: V7.0_2021.11.05 (row + cell)
-------------------------------------------------------

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
