--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------
-- Create columns leftwardLabel and rightwardLabel in table relationship
-------------------------------------------------------------------------

ALTER TABLE relationship ADD left_ward_label VARCHAR;
ALTER TABLE relationship ADD right_ward_label VARCHAR;