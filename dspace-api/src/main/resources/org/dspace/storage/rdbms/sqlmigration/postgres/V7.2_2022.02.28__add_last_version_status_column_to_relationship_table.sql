--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- NOTE: default 0 ensures that existing relations have "latest_version_status" set to "both" (first constant in enum, see Relationship class)
ALTER TABLE relationship ADD COLUMN IF NOT EXISTS latest_version_status INTEGER DEFAULT 0 NOT NULL;
