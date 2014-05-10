-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/

-- SQL commands to upgrade the Postgres database schema from DSpace 4.x to 5.x
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
BEGIN;

------------------------------------------------------
-- DS-1945 RequestItem Helpdesk, store request message
------------------------------------------------------
ALTER TABLE requestitem ADD request_message TEXT;


COMMIT;