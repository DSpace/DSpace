--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Issue #8517: Metadata positions must not be NULL.

ALTER TABLE metadatavalue
    ALTER COLUMN place SET NOT NULL;
