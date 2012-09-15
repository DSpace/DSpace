--
-- database_schema_18-3.sql
--
-- Version: $Revision$
--
-- Date:    $Date: 2012-05-29
--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--
-- SQL commands to upgrade the database schema of a live DSpace 1.8 or 1.8.x
-- to the DSpace 3 database schema
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
--

-------------------------------------------
-- DS-895 Advanced Embargo Project
-------------------------------------------

ALTER TABLE resourcepolicy ADD rpname VARCHAR(30);
ALTER TABLE resourcepolicy ADD rptype VARCHAR(30);
ALTER TABLE resourcepolicy ADD rpdescription VARCHAR(100);

ALTER TABLE item ADD discoverable BOOLEAN;

update item set discoverable=true;


-------------------------------------------
-- New columns and longer hash for salted password hashing DS-861 --
-------------------------------------------

ALTER TABLE EPerson ALTER COLUMN password TYPE VARCHAR(128);
ALTER TABLE EPerson ADD salt VARCHAR(32);
ALTER TABLE EPerson ADD digest_algorithm VARCHAR(16);
