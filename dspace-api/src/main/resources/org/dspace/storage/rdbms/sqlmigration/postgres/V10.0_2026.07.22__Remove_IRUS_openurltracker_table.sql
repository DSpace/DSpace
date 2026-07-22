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

-------------------------------------------------------------
-- This removes the IRUS statistics harvester tracker table.
-- IRUS-UK has been sunsetted, so the OpenURLTracker entity and
-- its supporting table are no longer used.
-------------------------------------------------------------

DROP TABLE IF EXISTS openurltracker;

DROP SEQUENCE IF EXISTS openurltracker_seq;
