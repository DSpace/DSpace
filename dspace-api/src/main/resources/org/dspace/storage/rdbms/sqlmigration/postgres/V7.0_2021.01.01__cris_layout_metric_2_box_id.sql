--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
CREATE SEQUENCE cris_layout_metric2box_id_seq;
DELETE FROM cris_layout_metric2box;
ALTER TABLE cris_layout_metric2box DROP CONSTRAINT cris_layout_metric2box_pkey;
ALTER TABLE cris_layout_metric2box ADD COLUMN id integer NOT NULL;
ALTER TABLE cris_layout_metric2box ADD CONSTRAINT cris_layout_metric2box_pkey PRIMARY KEY (id);
