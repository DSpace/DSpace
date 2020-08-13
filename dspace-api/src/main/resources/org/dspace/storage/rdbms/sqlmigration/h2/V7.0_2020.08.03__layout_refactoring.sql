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

-- --
--
-- --
ALTER TABLE cris_layout_tab ADD CONSTRAINT cris_layout_tab_entity_shortname_unique UNIQUE(entity_id, shortname);

-- --
--
-- --
ALTER TABLE cris_layout_box ADD CONSTRAINT cris_layout_box_entity_shortname_unique UNIQUE(entity_id, shortname);

-- --
-- Add field position to join table cris_layout_box2field. This field is used to
-- define the position of the field in the box
-- --
-- ALTER TABLE cris_layout_box2field ADD COLUMN position INTEGER;

-- --
-- Remove the priority field from cris_layout_field. Now the position of the field
-- in the box is defined by the field cris_layout_box2field.position
-- --
-- ALTER TABLE cris_layout_field DROP COLUMN priority;

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

ALTER TABLE cris_layout_box2securityfield RENAME TO cris_layout_box2securitymetadata;

ALTER TABLE cris_layout_tab2securityfield RENAME TO cris_layout_tab2securitymetadata;

ALTER TABLE cris_layout_box2securitymetadata ALTER COLUMN authorized_field_id RENAME TO metadata_field_id;

ALTER TABLE cris_layout_tab2securitymetadata ALTER COLUMN authorized_field_id RENAME TO metadata_field_id;

ALTER TABLE cris_layout_box2securitymetadata ADD CONSTRAINT cris_layout_box2securitymetadata_pkey PRIMARY KEY (metadata_field_id, box_id);

ALTER TABLE cris_layout_tab2securitymetadata ADD CONSTRAINT cris_layout_tab2securitymetadata_pkey PRIMARY KEY (metadata_field_id, tab_id);

ALTER TABLE cris_layout_tab2box ADD CONSTRAINT cris_layout_tab2box_pkey PRIMARY KEY (cris_layout_tab_id, cris_layout_box_id);

ALTER TABLE cris_layout_field ADD COLUMN box_id INTEGER;

DROP TABLE cris_layout_box2field, cris_layout_fieldbitstream2metadata;

ALTER TABLE cris_layout_field ADD CONSTRAINT cris_layout_field_box_fkey FOREIGN KEY (box_id) REFERENCES cris_layout_box (id);

ALTER TABLE cris_layout_field ADD COLUMN metadata_value VARCHAR(255);