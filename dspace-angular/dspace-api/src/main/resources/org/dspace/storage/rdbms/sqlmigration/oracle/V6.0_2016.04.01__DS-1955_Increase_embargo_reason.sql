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
-- DS-1955 resize rpdescription for embargo reason
------------------------------------------------------

-- We cannot alter type between varchar2 & clob directly so an in between column is required
ALTER TABLE resourcepolicy ADD rpdescription_clob CLOB;
UPDATE resourcepolicy SET rpdescription_clob=rpdescription, rpdescription=null;
ALTER TABLE resourcepolicy DROP COLUMN rpdescription;
ALTER TABLE resourcepolicy RENAME COLUMN rpdescription_clob TO rpdescription;