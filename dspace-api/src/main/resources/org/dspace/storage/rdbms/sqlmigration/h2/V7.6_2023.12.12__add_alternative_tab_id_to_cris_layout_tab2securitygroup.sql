--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Alter TABLE cris_layout_tab2securitygroup ADD alternative_tab_id
-----------------------------------------------------------------------------------

ALTER TABLE cris_layout_tab2securitygroup ADD COLUMN alternative_tab_id INTEGER;
ALTER TABLE cris_layout_tab2securitygroup ADD CONSTRAINT cris_layout_tab2securitygroup_tab_id2 FOREIGN KEY (alternative_tab_id) REFERENCES cris_layout_tab (id) ON DELETE SET NULL;