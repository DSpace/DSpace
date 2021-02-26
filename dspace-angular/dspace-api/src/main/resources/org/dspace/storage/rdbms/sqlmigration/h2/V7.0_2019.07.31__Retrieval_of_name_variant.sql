--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create columns leftwardValue and rightwardValue in table relationship
-- Rename columns left_label and right_label to leftward_type and rightward_type
-----------------------------------------------------------------------------------

ALTER TABLE relationship ADD leftward_value VARCHAR;
ALTER TABLE relationship ADD rightward_value VARCHAR;

ALTER TABLE relationship_type ALTER COLUMN left_label RENAME TO leftward_type;
ALTER TABLE relationship_type ALTER COLUMN right_label RENAME TO rightward_type;