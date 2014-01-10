--
-- database_schema_16-17.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2009-04-23 22:26:59 -0500 (Thu, 23 Apr 2009) $
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 1.6 or 1.6.x
-- to the DSpace 1.7 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.

------------------------------------------------------------------
-- Remove unused / obsolete sequence 'dctyperegistry_seq' (DS-729)
------------------------------------------------------------------
DROP SEQUENCE dctyperegistry_seq;