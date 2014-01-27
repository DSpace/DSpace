--
-- database_schema_4-5.sql
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 4.x
-- to the DSpace 5 database schema.
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

-------------------------------------------
-- Generalize metadata value table       --
-------------------------------------------

ALTER TABLE MetadataValue RENAME COLUMN item_id TO object_id;
ALTER INDEX metadatavalue_item_idx RENAME TO metadatavalue_object_idx;
ALTER INDEX metadatavalue_item_idx2 RENAME TO metadatavalue_object_idx2;
