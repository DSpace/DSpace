--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- Data Migration for XML/Configurable Workflow
--
-- This file will automatically migrate existing
-- classic workflows to XML/Configurable workflows.
-- NOTE however that the corresponding
-- "xml_workflow_migration.sql" script must FIRST be
-- called to create the appropriate database tables.
--
-- This script is called automatically by the following
-- Flyway Java migration class:
-- org.dspace.storage.rdbms.migration.V5_0_2014_01_01__XMLWorkflow_Migration
----------------------------------------------------

-- Convert workflow groups:
-- TODO: is 'to_number' ok? do not forget to change role_id values

INSERT INTO cwf_collectionrole (collectionrole_id, role_id, group_id, collection_id)
SELECT
cwf_collectionrole_seq.nextval as collectionrole_id,
'reviewer' AS role_id,
collection.workflow_step_1 AS group_id,
collection.collection_id AS collection_id
FROM collection
WHERE collection.workflow_step_1 IS NOT NULL;

INSERT INTO cwf_collectionrole  (collectionrole_id, role_id, group_id, collection_id)
SELECT
cwf_collectionrole_seq.nextval as collectionrole_id,
'editor' AS role_id,
collection.workflow_step_2 AS group_id,
collection.collection_id AS collection_id
FROM collection
WHERE collection.workflow_step_2 IS NOT NULL;

INSERT INTO cwf_collectionrole  (collectionrole_id, role_id, group_id, collection_id)
SELECT
cwf_collectionrole_seq.nextval as collectionrole_id,
'finaleditor' AS role_id,
collection.workflow_step_3 AS group_id,
collection.collection_id AS collection_id
FROM collection
WHERE collection.workflow_step_3 IS NOT NULL;


-- Migrate workflow items
INSERT INTO cwf_workflowitem (workflowitem_id, item_id, collection_id, multiple_titles, published_before, multiple_files)
SELECT
workflow_id AS workflowitem_id,
item_id,
collection_id,
multiple_titles,
published_before,
multiple_files
FROM workflowitem;


-- Migrate claimed tasks
INSERT INTO cwf_claimtask (claimtask_id,workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
cwf_claimtask_seq.nextval AS claimtask_id,
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'reviewstep' AS step_id,
'reviewaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 2;

INSERT INTO cwf_claimtask (claimtask_id,workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
cwf_claimtask_seq.nextval AS claimtask_id,
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'editstep' AS step_id,
'editaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 4;

INSERT INTO cwf_claimtask (claimtask_id,workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
cwf_claimtask_seq.nextval AS claimtask_id,
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'finaleditstep' AS step_id,
'finaleditaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 6;


-- Migrate pooled tasks
INSERT INTO cwf_pooltask (pooltask_id,workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
cwf_pooltask_seq.nextval AS pooltask_id,
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'reviewstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 1 AND cwf_collectionrole.role_id = 'reviewer';

INSERT INTO cwf_pooltask (pooltask_id,workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
cwf_pooltask_seq.nextval AS pooltask_id,
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'editstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 3 AND cwf_collectionrole.role_id = 'editor';

INSERT INTO cwf_pooltask (pooltask_id,workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
cwf_pooltask_seq.nextval AS pooltask_id,
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'finaleditstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 5 AND cwf_collectionrole.role_id = 'finaleditor';

-- Delete resource policies for workflowitems before creating new ones
DELETE FROM resourcepolicy
WHERE resource_type_id = 2 AND resource_id IN
  (SELECT item_id FROM workflowitem);

DELETE FROM resourcepolicy
WHERE resource_type_id = 1 AND resource_id IN
  (SELECT item2bundle.bundle_id FROM
    (workflowitem INNER JOIN item2bundle ON workflowitem.item_id = item2bundle.item_id));

DELETE FROM resourcepolicy
WHERE resource_type_id = 0 AND resource_id IN
  (SELECT bundle2bitstream.bitstream_id FROM
    ((workflowitem INNER JOIN item2bundle ON workflowitem.item_id = item2bundle.item_id)
      INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id));
-- Create policies for claimtasks
--     public static final int BITSTREAM = 0;
--     public static final int BUNDLE = 1;
--     public static final int ITEM = 2;

--     public static final int READ = 0;
--     public static final int WRITE = 1;
--     public static final int DELETE = 2;
--     public static final int ADD = 3;
--     public static final int REMOVE = 4;
-- Item
-- TODO: getnextID == SELECT sequence.nextval FROM DUAL!!
-- Create a temporarty table with action ID's
CREATE TABLE temptable(
  action_id INTEGER PRIMARY KEY
);
INSERT ALL
  INTO temptable (action_id) VALUES (0)
  INTO temptable (action_id) VALUES (1)
  INTO temptable (action_id) VALUES (2)
  INTO temptable (action_id) VALUES (3)
  INTO temptable (action_id) VALUES (4)
SELECT * FROM DUAL;

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM (cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id),
temptable;

-- Bundles
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM
(
	(cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
), temptable;


-- Bitstreams
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM
(
	((cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
	INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
), temptable;


-- Create policies for pooled tasks

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM (cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id),
temptable;

-- Bundles
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM
(
	(cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
), temptable;

-- Bitstreams
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM
(
	((cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
	INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
), temptable;

-- Drop the temporary table with the action ID's
DROP TABLE temptable;

-- Create policies for submitter
-- TODO: only add if unique
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM (cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id);

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM ((cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id)
      INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
     );

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
resourcepolicy_seq.nextval AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM (((cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id)
      INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
      INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
);

-- TODO: not tested yet
INSERT INTO cwf_in_progress_user (in_progress_user_id, workflowitem_id, user_id, finished)
SELECT
  cwf_in_progress_user_seq.nextval AS in_progress_user_id,
  cwf_workflowitem.workflowitem_id AS workflowitem_id,
  cwf_claimtask.owner_id AS user_id,
  0 as finished
FROM
  (cwf_claimtask INNER JOIN cwf_workflowitem ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id);

-- TODO: improve this, important is NVL(curr, 1)!! without this function, empty tables (max = [null]) will only result in sequence deletion
DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(workflowitem_id) INTO curr FROM cwf_workflowitem;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_workflowitem_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_workflowitem_seq START WITH ' || NVL(curr, 1);
END;
/

DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(collectionrole_id) INTO curr FROM cwf_collectionrole;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_collectionrole_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_collectionrole_seq START WITH ' || NVL(curr, 1);
END;
/

DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(workflowitemrole_id) INTO curr FROM cwf_workflowitemrole;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_workflowitemrole_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_workflowitemrole_seq START WITH ' || NVL(curr, 1);
END;
/

DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(pooltask_id) INTO curr FROM cwf_pooltask;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_pooltask_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_pooltask_seq START WITH ' || NVL(curr, 1);
END;
/

DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(claimtask_id) INTO curr FROM cwf_claimtask;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_claimtask_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_claimtask_seq START WITH ' || NVL(curr, 1);
END;
/

DECLARE
  curr  NUMBER := 0;
BEGIN
  SELECT max(in_progress_user_id) INTO curr FROM cwf_in_progress_user;

  curr := curr + 1;

  EXECUTE IMMEDIATE 'DROP SEQUENCE cwf_in_progress_user_seq';

  EXECUTE IMMEDIATE 'CREATE SEQUENCE cwf_in_progress_user_seq START WITH ' || NVL(curr, 1);
END;
/
