--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Drop imp_record_to_item table.
-----------------------------------------------------------------------------------

ALTER TABLE harvested_item
ALTER COLUMN oai_id TYPE CHARACTER VARYING(255);