--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

-------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------
UPDATE metadatavalue SET dspace_object_id = (SELECT uuid
                                             FROM collection
                                             WHERE template_item_id = dspace_object_id)
WHERE dspace_object_id IN (SELECT template_item_id 
                           FROM Collection)
                       AND metadata_field_id
                       IN (SELECT metadata_field_id
                           FROM metadatafieldregistry mfr LEFT JOIN metadataschemaregistry msr
                                                          ON mfr.metadata_schema_id = msr.metadata_schema_id
                           WHERE msr.short_id = 'dspace' AND mfr.element = 'entity' AND mfr.qualifier = 'type');