--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- edit notifyservice table add score column
-----------------------------------------------------------------------------------

ALTER TABLE notifyservice ADD COLUMN score NUMERIC(6, 5) DEFAULT NULL CHECK (score >= 0 AND score <= 1);