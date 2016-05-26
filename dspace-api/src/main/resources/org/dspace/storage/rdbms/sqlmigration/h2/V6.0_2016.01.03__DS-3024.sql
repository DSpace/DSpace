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
      ADD (permanent BOOLEAN DEFAULT false);
UPDATE epersongroup SET permanent = true
       WHERE uuid IN (
         SELECT dspace_object_id
	   FROM metadataschemaregistry AS s
	     JOIN metadatafieldregistry AS f
	       ON (s.metadata_schema_id = f.metadata_schema_id)
	     JOIN metadatavalue AS v
	       ON (f.metadata_field_id = v.metadata_field_id)
	   WHERE s.short_id = 'dc'
	     AND f.element = 'title'
	     AND f.qualifier IS NULL
	     AND v.text_value IN ('Administrator', 'Anonymous')
       );
