--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- edit notifyservice table add enabled column
-----------------------------------------------------------------------------------

ALTER TABLE notifyservice ADD COLUMN enabled BOOLEAN NOT NULL;