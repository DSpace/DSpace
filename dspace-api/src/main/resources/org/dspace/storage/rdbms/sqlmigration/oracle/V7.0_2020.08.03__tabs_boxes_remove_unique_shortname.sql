--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- --
-- Remove unique constraint on shortname of table cris_layout_tab.
-- Now the shortname don't is unique because different entity types can have tabs with same shortname
-- --
ALTER TABLE cris_layout_tab DROP CONSTRAINT cris_layout_tab_shortname_key;

-- --
-- Remove unique constraint on shortname of table cris_layout_box.
-- Now the shortname don't is unique because different entity types can have boxes with same shortname
-- --
ALTER TABLE cris_layout_box DROP CONSTRAINT cris_layout_box_shortname_key;