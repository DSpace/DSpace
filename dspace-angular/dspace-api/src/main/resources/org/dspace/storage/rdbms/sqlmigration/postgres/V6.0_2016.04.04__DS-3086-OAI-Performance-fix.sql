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
-- increase the performance of the queries. It will also add
-- "ON DELETE CASCADE" to improve the performance of Item deletion.
---------------------------------------------------------------

CREATE UNIQUE INDEX metadataschemaregistry_unique_idx_short_id on metadataschemaregistry(short_id);

CREATE INDEX metadatafieldregistry_idx_element_qualifier on metadatafieldregistry(element, qualifier);

CREATE INDEX resourcepolicy_idx_rptype on resourcepolicy(rptype);

ALTER TABLE resourcepolicy
DROP CONSTRAINT resourcepolicy_dspace_object_fkey,
ADD CONSTRAINT resourcepolicy_dspace_object_fkey
FOREIGN KEY (dspace_object)
REFERENCES dspaceobject(uuid)
ON DELETE CASCADE;

ALTER TABLE metadatavalue
DROP CONSTRAINT metadatavalue_dspace_object_id_fkey,
ADD CONSTRAINT metadatavalue_dspace_object_id_fkey
FOREIGN KEY (dspace_object_id)
REFERENCES dspaceobject(uuid)
ON DELETE CASCADE;

-- Clean up

-- Duplicate of INDEX metadatavalue_field_object (a composite index can also serve as a 'single field' index)
-- and metadatavalue_field_fk_idx. This index also does not exist for H2 or Oracle.
DROP INDEX metadatavalue_field;