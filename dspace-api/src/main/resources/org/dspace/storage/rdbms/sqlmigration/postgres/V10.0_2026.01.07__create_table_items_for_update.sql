--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create TABLE itemupdate_metadata_enhancement
-----------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS itemupdate_metadata_enhancement
(
    uuid        UUID       NOT NULL PRIMARY KEY,
    date_queued TIMESTAMP  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_date_queued
    ON itemupdate_metadata_enhancement(date_queued);
