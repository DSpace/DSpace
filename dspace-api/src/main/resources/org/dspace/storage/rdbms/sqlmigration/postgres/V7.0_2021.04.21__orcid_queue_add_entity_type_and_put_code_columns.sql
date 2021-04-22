--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE orcid_queue ADD COLUMN put_code CHARACTER VARYING(255);
ALTER TABLE orcid_queue ADD COLUMN entity_type CHARACTER VARYING(255);
ALTER TABLE orcid_queue ALTER COLUMN entity_id DROP NOT NULL;