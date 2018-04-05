--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-3125 Submitters cannot delete bistreams of workspaceitems
---------------------------------------------------------------
-- This script will add delete rights on all bundles/bitstreams
-- for people who already have REMOVE rights.
-- In previous versions REMOVE rights was enough to ensure that
-- you could delete an object.
---------------------------------------------------------------
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, start_date, end_date, rpname,
rptype, rpdescription, eperson_id, epersongroup_id, dspace_object)
SELECT
resourcepolicy_seq.nextval AS policy_id,
resource_type_id,
resource_id,
-- Insert the Constants.DELETE action
2 AS action_id,
start_date,
end_date,
rpname,
rptype,
rpdescription,
eperson_id,
epersongroup_id,
dspace_object
FROM resourcepolicy WHERE action_id=4 AND (resource_type_id=0 OR resource_type_id=1 OR resource_type_id=2);
