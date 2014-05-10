-- http://www.dspace.org/license/
-- SQL commands to upgrade the Postgres database schema from DSpace 4.x to 5.x
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
BEGIN;

------------------------------------------------------
-- DS-1945 RequestItem Helpdesk, store request message
------------------------------------------------------
ALTER TABLE requestitem ADD request_message TEXT;


COMMIT;