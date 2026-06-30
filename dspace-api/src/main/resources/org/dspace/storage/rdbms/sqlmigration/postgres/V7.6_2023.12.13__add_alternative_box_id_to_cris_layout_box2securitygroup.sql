--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Alter TABLE cris_layout_box2securitygroup ADD alternative_box_id
-----------------------------------------------------------------------------------

ALTER TABLE cris_layout_box2securitygroup ADD COLUMN alternative_box_id INTEGER;
ALTER TABLE cris_layout_box2securitygroup ADD CONSTRAINT cris_layout_box2securitygroup_box_id2 FOREIGN KEY (alternative_box_id) REFERENCES cris_layout_box (id) ON DELETE SET NULL;