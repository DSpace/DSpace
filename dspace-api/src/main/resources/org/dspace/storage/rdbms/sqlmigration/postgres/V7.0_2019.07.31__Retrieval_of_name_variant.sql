--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create columns leftwardLabel and rightwardLabel in table relationship
-- Rename columns left_label and right_label to leftward_label and rightward_label
-----------------------------------------------------------------------------------

ALTER TABLE relationship ADD leftward_label VARCHAR;
ALTER TABLE relationship ADD rightward_label VARCHAR;

ALTER TABLE relationship_type RENAME COLUMN left_label TO leftward_label;
ALTER TABLE relationship_type RENAME COLUMN right_label TO rightward_label;