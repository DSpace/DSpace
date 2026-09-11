--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------
-- Add heartbeat column to process table
-- Stores the last time a process reported it was still running
-------------------------------------------------------------------------------

ALTER TABLE process ADD COLUMN heartbeat TIMESTAMP;