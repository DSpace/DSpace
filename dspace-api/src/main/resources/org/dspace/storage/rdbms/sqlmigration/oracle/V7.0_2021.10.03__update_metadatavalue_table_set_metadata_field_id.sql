--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------------
---- UPDATE table metadatavalue
-------------------------------------------------------------------------------------

UPDATE metadatavalue SET metadata_field_id = (
                         SELECT metadata_field_id 
                         FROM metadatafieldregistry mfr LEFT JOIN metadataschemaregistry msr
                                                        ON mfr.metadata_schema_id = msr.metadata_schema_id
                         WHERE msr.short_id = 'dspace' AND mfr.element = 'entity' AND mfr.qualifier = 'type')
WHERE metadata_field_id is null and text_value = 'Publication';