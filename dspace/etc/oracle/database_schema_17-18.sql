--
-- database_schema_17-18.sql
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
-- SQL commands to upgrade the database schema of a live DSpace 1.7 or 1.7.x
-- to the DSpace 1.8 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--
-------------------------------------------
-- New column for bitstream order DS-749 --
-------------------------------------------
ALTER TABLE bundle2bitstream ADD bitstream_order INTEGER;

--Place the sequence id's in the order
UPDATE bundle2bitstream SET bitstream_order=(SELECT sequence_id FROM bitstream WHERE bitstream.bitstream_id=bundle2bitstream.bitstream_id);
