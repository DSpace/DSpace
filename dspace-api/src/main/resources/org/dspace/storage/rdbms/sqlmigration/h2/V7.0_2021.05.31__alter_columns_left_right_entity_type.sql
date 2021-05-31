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

ALTER TABLE relationship_type ALTER COLUMN right_type DROP NOT NULL;
ALTER TABLE relationship_type ALTER COLUMN left_type DROP NOT NULL;
