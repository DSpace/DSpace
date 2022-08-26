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
-- Move all 'relationship.type' metadata fields to 'dspace.entity.type'. Remove 'relationship' schema.
-------------------------------------------------------------------------------------------------------
-- Special case: we need to the 'dspace' schema to already exist. If users don't already have it we must create it
-- manually via SQL, as by default it won't be created until database updates are finished.
INSERT INTO metadataschemaregistry (metadata_schema_id, namespace, short_id)
  SELECT metadataschemaregistry_seq.nextval, 'http://dspace.org/dspace' as namespace, 'dspace' as short_id FROM dual
    WHERE NOT EXISTS
      (SELECT metadata_schema_id,namespace,short_id FROM metadataschemaregistry
       WHERE namespace = 'http://dspace.org/dspace' AND short_id = 'dspace');


-- Add 'dspace.entity.type' field to registry (if missing)
INSERT INTO metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier)
  SELECT metadatafieldregistry_seq.nextval,
    (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dspace'), 'entity', 'type' FROM dual
        WHERE NOT EXISTS
          (SELECT metadata_field_id,element,qualifier FROM metadatafieldregistry
           WHERE metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dspace')
           AND element = 'entitye' AND qualifier='type');

-- Moves all 'relationship.type' field values to a new 'dspace.entity.type' field
UPDATE metadatavalue
  SET metadata_field_id =
     (SELECT metadata_field_id FROM metadatafieldregistry
      WHERE metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dspace')
      AND element = 'entity' AND qualifier='type')
  WHERE metadata_field_id =
     (SELECT metadata_field_id FROM metadatafieldregistry
      WHERE metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='relationship')
      AND element = 'type' AND qualifier is NULL);


-- Delete 'relationship.type' field from registry
DELETE FROM metadatafieldregistry
  WHERE metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id = 'relationship')
  AND element = 'type' AND qualifier is NULL;

-- Delete 'relationship' schema (which is now empty)
DELETE FROM metadataschemaregistry WHERE short_id = 'relationship' AND namespace = 'http://dspace.org/relationship';
