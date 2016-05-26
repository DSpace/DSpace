--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-3024 Invent "permanent" groups
------------------------------------------------------

ALTER TABLE epersongroup
      ADD COLUMN permanent BOOLEAN DEFAULT false;
UPDATE epersongroup SET permanent = true
       WHERE uuid IN (
         SELECT dspace_object_id
	   FROM metadataschemaregistry s
	     JOIN metadatafieldregistry f USING (metadata_schema_id)
	     JOIN metadatavalue v USING (metadata_field_id)
	   WHERE s.short_id = 'dc'
	     AND f.element = 'title'
	     AND f.qualifier IS NULL
	     AND v.text_value IN ('Administrator', 'Anonymous')
       );
