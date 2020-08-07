--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- --
-- Add field position to join table cris_layout_box2field. This field is used to
-- define the position of the field in the box
-- --
ALTER TABLE cris_layout_box2field ADD COLUMN position INTEGER;

-- --
-- Remove the priority field from cris_layout_field. Now the position of the field
-- in the box is defined by the field cris_layout_box2field.position
-- --
ALTER TABLE cris_layout_field DROP COLUMN priority;

-- --
-- Add field position to join table cris_layout_tab2field. This field is used to
-- define the position of the box in the tab
-- --
ALTER TABLE cris_layout_tab2box ADD COLUMN position INTEGER;

-- --
-- Remove the priority field from cris_layout_box. Now the position of the box
-- in the tab is defined by the field cris_layout_tab2box.position
-- --
ALTER TABLE cris_layout_box DROP COLUMN priority;