--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-3024 extremely slow searching when logged in as admin
---------------------------------------------------------------
-- This script will put the group name on the epersongroup
-- record itself for performance reasons. It will also make
-- sure that a group name is unique (so that for example no two
-- Administrator groups can be created).
---------------------------------------------------------------

ALTER TABLE epersongroup
DROP COLUMN IF EXISTS name;

ALTER TABLE epersongroup
ADD (name VARCHAR(250));

CREATE UNIQUE INDEX epersongroup_unique_idx_name on epersongroup(name);

UPDATE epersongroup
SET name =
(SELECT text_value
 FROM metadatavalue v
   JOIN metadatafieldregistry field on v.metadata_field_id = field.metadata_field_id
   JOIN metadataschemaregistry s ON field.metadata_schema_id = s.metadata_schema_id
 WHERE s.short_id = 'dc' AND element = 'title' AND qualifier IS NULL
 AND v.dspace_object_id = epersongroup.uuid LIMIT 1);