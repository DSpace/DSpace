--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------------
---- CREATE table cris_layout_row
-------------------------------------------------------------------------------------

CREATE SEQUENCE cris_layout_row_id_seq;
CREATE TABLE  cris_layout_row
(
  id         INTEGER NOT NULL,
  style      CHARACTER VARYING(255),
  tab        INTEGER NOT NULL,
  position   INTEGER,

  CONSTRAINT cris_layout_row_pkey PRIMARY KEY (id),
  CONSTRAINT cris_layout_tab_fkey  FOREIGN KEY  (tab) REFERENCES cris_layout_tab (id)
);

-------------------------------------------------------------------------------------
---- CREATE table cris_layout_cell
-------------------------------------------------------------------------------------

CREATE SEQUENCE cris_layout_cell_id_seq;
CREATE TABLE  cris_layout_cell
(
  id         INTEGER NOT NULL,
  style      CHARACTER VARYING(255),
  row        INTEGER NOT NULL,
  position   INTEGER,

  CONSTRAINT cris_layout_cell_pkey PRIMARY KEY (id),
  CONSTRAINT cris_layout_row_fkey  FOREIGN KEY  (row) REFERENCES cris_layout_row (id)
);

-------------------------------------------------------------------------------------
---- CREATE table cris_layout_tab2box
-------------------------------------------------------------------------------------
DROP TABLE cris_layout_tab2box;

-------------------------------------------------------------------------------------
---- ALTER table cris_layout_box
-------------------------------------------------------------------------------------

ALTER TABLE cris_layout_box DROP COLUMN clear;
ALTER TABLE cris_layout_box ADD COLUMN cell INTEGER;
ALTER TABLE cris_layout_box ADD COLUMN position INTEGER;
ALTER TABLE cris_layout_box ADD COLUMN container BOOLEAN;
ALTER TABLE cris_layout_box ADD CONSTRAINT cris_layout_cell_id_fk FOREIGN KEY (cell) REFERENCES cris_layout_cell;

-------------------------------------------------------------------------------------
---- ALTER table cris_layout_tab
-------------------------------------------------------------------------------------

ALTER TABLE cris_layout_tab ADD COLUMN is_leading BOOLEAN;

-------------------------------------------------------------------------------------
---- ALTER table cris_layout_field
-------------------------------------------------------------------------------------

ALTER TABLE cris_layout_field ADD COLUMN label_as_heading BOOLEAN;
ALTER TABLE cris_layout_field ADD COLUMN values_inline BOOLEAN;

-------------------------------------------------------------------------------------
---- ALTER table cris_layout_field2nested
-------------------------------------------------------------------------------------

ALTER TABLE cris_layout_field2nested ADD COLUMN label_as_heading BOOLEAN;
ALTER TABLE cris_layout_field2nested ADD COLUMN values_inline BOOLEAN;