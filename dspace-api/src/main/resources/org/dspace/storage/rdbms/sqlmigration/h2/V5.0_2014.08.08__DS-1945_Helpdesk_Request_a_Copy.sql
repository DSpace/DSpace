--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

------------------------------------------------------
-- DS-1945 RequestItem Helpdesk, store request message
------------------------------------------------------
ALTER TABLE requestitem ADD COLUMN request_message VARCHAR2(2000);
