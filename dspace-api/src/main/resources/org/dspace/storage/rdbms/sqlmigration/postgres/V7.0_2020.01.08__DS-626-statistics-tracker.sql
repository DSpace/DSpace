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
-- This will create the setup for the IRUS statistics harvester
-------------------------------------------------------------

-- Because UM crated this before, but now we want it slightly different
-- This is the migration I added when we had 6.3
-- V5.0_2015.06.18__statistics-harvester.sql

-- CREATE SEQUENCE openurltracker_seq;

-- CREATE TABLE openurltracker
--(
--    tracker_id INTEGER,
--    tracker_url VARCHAR(1000),
--    uploaddate DATE,
--    CONSTRAINT  openurltracker_PK PRIMARY KEY (tracker_id)
--);
