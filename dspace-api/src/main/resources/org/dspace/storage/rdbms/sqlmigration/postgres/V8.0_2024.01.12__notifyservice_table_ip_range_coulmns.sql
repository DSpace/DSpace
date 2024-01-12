--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--------------------------------------------------------------------------
-- ADD IP Range columns to notifyservice table
--------------------------------------------------------------------------

ALTER TABLE notifyservice ADD COLUMN lower_ip VARCHAR(45);

ALTER TABLE notifyservice ADD COLUMN upper_ip VARCHAR(45);