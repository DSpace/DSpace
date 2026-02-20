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

-------------------------------------------------------------
-- This will create a temporary table and migrate all
-- existing dc.identifier metadata fields from projects
-- to oaire.awardNumber
-- 
-- this script is thought to be idempotent
-- meaning it can executed several times and the expected
-- result it will be always the same.
-- Despite the warning if you have a DS <10.x version
-- you can execute it manualy on the DB to fix your instance
-- Issue: https://github.com/DSpace/DSpace/issues/11869
-------------------------------------------------------------

BEGIN;

CREATE TEMPORARY TABLE "temp_metadatavalue" (
    "metadata_value_id" Integer NOT NULL,
    "metadata_field_id" Integer,
    "text_value" Text,
    "dspace_object_id" UUid );

INSERT INTO "temp_metadatavalue" (metadata_value_id,metadata_field_id,text_value,dspace_object_id)
SELECT 
  metadata_value_id,
  metadata_field_id,
  text_value,
  dspace_object_id
FROM metadatavalue
WHERE dspace_object_id IN
(select dspace_object_id
from metadatavalue 
WHERE metadata_field_id IN
( select metadata_field_id
    from metadatafieldregistry
    where metadata_schema_id=(select metadata_schema_id from metadataschemaregistry where short_id='dspace')
      and element = 'entity'
      and qualifier = 'type'
)
 AND text_value = 'Project')

 AND metadata_field_id = (select metadata_field_id
    from metadatafieldregistry
    where metadata_schema_id=(select metadata_schema_id from metadataschemaregistry where short_id='dc')
      and element = 'identifier'
    and qualifier IS NULL
);

UPDATE metadatavalue SET
metadata_field_id = (select metadata_field_id
    from metadatafieldregistry
    where metadata_schema_id=(select metadata_schema_id from metadataschemaregistry where short_id='oaire')
      and element = 'awardNumber'
    and qualifier IS NULL
)
WHERE metadata_value_id IN (SELECT metadata_value_id FROM temp_metadatavalue);

COMMIT;
