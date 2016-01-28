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

-------------------------------------------
-- New column for bitstream order DS-749 --
-------------------------------------------
ALTER TABLE bundle2bitstream ADD bitstream_order INTEGER;

--Place the sequence id's in the order
UPDATE bundle2bitstream SET bitstream_order=(SELECT sequence_id FROM bitstream WHERE bitstream.bitstream_id=bundle2bitstream.bitstream_id);
