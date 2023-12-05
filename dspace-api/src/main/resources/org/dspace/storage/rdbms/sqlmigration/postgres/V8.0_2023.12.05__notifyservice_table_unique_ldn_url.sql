--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- ADD CONSTRAINT on notifyservice table: ldn_url as unique
-----------------------------------------------------------------------------------

ALTER TABLE notifyservice ADD CONSTRAINT ldn_url_unique UNIQUE (ldn_url);

