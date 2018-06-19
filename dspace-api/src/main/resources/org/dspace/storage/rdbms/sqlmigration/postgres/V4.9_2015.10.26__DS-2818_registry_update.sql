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

--  Special case of migration, we need to the EPerson schema in order to get our metadata for all queries to work
-- but we cannot a DB connection until our database is up to date, so we need to create our registries manually in sql

INSERT INTO metadataschemaregistry (namespace, short_id) SELECT 'http://dspace.org/eperson', 'eperson'
  WHERE NOT EXISTS (SELECT metadata_schema_id,namespace,short_id FROM metadataschemaregistry WHERE namespace = 'http://dspace.org/eperson' AND short_id = 'eperson');


-- Insert eperson.firstname
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'), 'firstname'
    WHERE NOT EXISTS (SELECT metadata_field_id,element FROM metadatafieldregistry WHERE element = 'firstname' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'));

-- Insert eperson.lastname
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'), 'lastname'
    WHERE NOT EXISTS (SELECT metadata_field_id,element FROM metadatafieldregistry WHERE element = 'lastname' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'));

-- Insert eperson.phone
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'), 'phone'
    WHERE NOT EXISTS (SELECT metadata_field_id,element FROM metadatafieldregistry WHERE element = 'phone' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'));

-- Insert eperson.language
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'), 'language'
    WHERE NOT EXISTS (SELECT metadata_field_id,element FROM metadatafieldregistry WHERE element = 'language' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='eperson'));

-- Insert into dc.provenance
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dc'), 'provenance'
    WHERE NOT EXISTS (SELECT metadata_field_id,element FROM metadatafieldregistry WHERE element = 'provenance' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dc'));

-- Insert into dc.rights.license
INSERT INTO metadatafieldregistry (metadata_schema_id, element, qualifier)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dc'), 'rights', 'license'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,qualifier FROM metadatafieldregistry WHERE element = 'rights' AND qualifier='license' AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='dc'));