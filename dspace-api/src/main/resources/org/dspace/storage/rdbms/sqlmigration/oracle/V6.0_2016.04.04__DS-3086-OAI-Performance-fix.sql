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

CREATE UNIQUE INDEX metadataschema_idx_short_id on metadataschemaregistry(short_id);

CREATE INDEX metadatafield_idx_elem_qual on metadatafieldregistry(element, qualifier);

CREATE INDEX resourcepolicy_idx_rptype on resourcepolicy(rptype);

-- Add "ON DELETE CASCADE" to foreign key constraint to Item
ALTER TABLE RESOURCEPOLICY ADD DSPACE_OBJECT_NEW RAW(16);
UPDATE RESOURCEPOLICY SET DSPACE_OBJECT_NEW = DSPACE_OBJECT;
ALTER TABLE RESOURCEPOLICY DROP COLUMN DSPACE_OBJECT;
ALTER TABLE RESOURCEPOLICY RENAME COLUMN DSPACE_OBJECT_NEW to DSPACE_OBJECT;

ALTER TABLE RESOURCEPOLICY
ADD CONSTRAINT RESOURCEPOLICY_DSPACE_OBJ_FK
FOREIGN KEY (DSPACE_OBJECT)
REFERENCES dspaceobject(uuid)
ON DELETE CASCADE;

-- Add "ON DELETE CASCADE" to foreign key constraint to Item
ALTER TABLE METADATAVALUE ADD DSPACE_OBJECT_NEW RAW(16);
UPDATE METADATAVALUE SET DSPACE_OBJECT_NEW = DSPACE_OBJECT_ID;
ALTER TABLE METADATAVALUE DROP COLUMN DSPACE_OBJECT_ID;
ALTER TABLE METADATAVALUE RENAME COLUMN DSPACE_OBJECT_NEW to DSPACE_OBJECT_ID;

ALTER TABLE METADATAVALUE
ADD CONSTRAINT METADATAVALUE_DSPACE_OBJECT_FK
FOREIGN KEY (DSPACE_OBJECT_ID)
REFERENCES DSPACEOBJECT(UUID)
ON DELETE CASCADE;