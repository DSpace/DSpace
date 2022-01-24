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

ALTER TABLE relationship ADD leftward_value VARCHAR2(50);
ALTER TABLE relationship ADD rightward_value VARCHAR2(50);

ALTER TABLE relationship_type RENAME COLUMN left_label TO leftward_type;
ALTER TABLE relationship_type RENAME COLUMN right_label TO rightward_type;
