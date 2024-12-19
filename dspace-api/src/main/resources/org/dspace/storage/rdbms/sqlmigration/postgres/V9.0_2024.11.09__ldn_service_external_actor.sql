--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Alter notifyservice table
-----------------------------------------------------------------------------------

ALTER TABLE notifyservice ADD COLUMN uses_actor_email_id BOOLEAN DEFAULT false;
