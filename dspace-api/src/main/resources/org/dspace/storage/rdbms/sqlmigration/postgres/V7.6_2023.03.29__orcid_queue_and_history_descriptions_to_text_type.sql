--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE orcid_history ALTER COLUMN description TYPE TEXT;
ALTER TABLE orcid_queue ALTER COLUMN description TYPE TEXT;
