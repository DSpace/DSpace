--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-3086 OAI Harvesting performance
---------------------------------------------------------------
-- This script will create indexes on the key fields of the
-- metadataschemaregistry and metadatafieldregistry tables to
-- increase the performance of the queries. It will also clean
-- up some redunant indexes on the metadatavalue table.
---------------------------------------------------------------

CREATE UNIQUE INDEX metadataschemaregistry_unique_idx_short_id on metadataschemaregistry(short_id);

CREATE INDEX metadatafieldregistry_idx_element_qualifier on metadatafieldregistry(element, qualifier);

CREATE INDEX resourcepolicy_idx_rptype on resourcepolicy(rptype);

-- Clean up

-- Duplicates of INDEX metadatavalue_field_object (a composite index can also serve as a 'single field' index)
DROP INDEX metadatavalue_field;
DROP INDEX metadatavalue_field_fk_idx;