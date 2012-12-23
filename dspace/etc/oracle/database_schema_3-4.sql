--
-- database_schema_3-4.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2012-12-21 (THE END OF THE WORLD)
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 3
-- to the DSpace 4 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

ALTER TABLE Community ADD istop NUMBER(1);
ALTER TABLE Community ADD item_count INTEGER;

ALTER TABLE Collection ADD item_count INTEGER;
ALTER TABLE MetadataValue ADD resource_id INTEGER;
ALTER TABLE MetadataValue ADD resource_type INTEGER;
