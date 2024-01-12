--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--------------------------------------------------------------------------
-- ADD source_ip columns to ldn_message table
--------------------------------------------------------------------------

ALTER TABLE ldn_message ADD COLUMN source_ip VARCHAR(45);