--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DELETE FROM ResourcePolicy WHERE eperson_id is null and epersongroup_id is null;

ALTER TABLE ResourcePolicy ADD CONSTRAINT resourcepolicy_eperson_and_epersongroup_not_nullobject_chk
    CHECK (eperson_id is not null or epersongroup_id is not null) ;
