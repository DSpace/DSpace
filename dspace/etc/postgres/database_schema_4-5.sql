--
-- database_schema_4-5.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2014-03-10
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 4.0 or 4.x
-- to the DSpace 5 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

-------------------------------------------------------
-- BitstreamFormatRegistry table
-------------------------------------------------------

ALTER TABLE BitstreamFormatRegistry ADD streamable BOOLEAN;

update item set streamable=false;
