  -- Convert workflow groups:
INSERT INTO cwf_collectionrole (role_id, group_id, collection_id)
SELECT
'reviewer' AS role_id,
collection.workflow_step_1 AS group_id,
collection.collection_id AS collection_id
FROM collection
WHERE collection.workflow_step_1 IS NOT NULL;

INSERT INTO cwf_collectionrole  (role_id, group_id, collection_id)
SELECT
'editor' AS role_id,
collection.workflow_step_2 AS group_id,
collection.collection_id AS collection_id
FROM collection
WHERE collection.workflow_step_2 IS NOT NULL;

INSERT INTO cwf_collectionrole  (role_id, group_id, collection_id)
SELECT
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
INSERT INTO cwf_claimtask (workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'reviewstep' AS step_id,
'reviewaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 2;

INSERT INTO cwf_claimtask (workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'editstep' AS step_id,
'editaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 4;

INSERT INTO cwf_claimtask (workflowitem_id, workflow_id, step_id, action_id, owner_id)
SELECT
workflow_id AS workflowitem_id,
'default' AS workflow_id,
'finaleditstep' AS step_id,
'finaleditaction' AS action_id,
owner AS owner_id
FROM workflowitem WHERE owner IS NOT NULL AND state = 6;


-- Migrate pooled tasks
INSERT INTO cwf_pooltask (workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'reviewstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 1 AND cwf_collectionrole.role_id = 'reviewer';

INSERT INTO cwf_pooltask (workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'editstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 3 AND cwf_collectionrole.role_id = 'editor';

INSERT INTO cwf_pooltask (workflowitem_id, workflow_id, step_id, action_id, group_id)
SELECT
workflowitem.workflow_id AS workflowitem_id,
'default' AS workflow_id,
'finaleditstep' AS step_id,
'claimaction' AS action_id,
cwf_collectionrole.group_id AS group_id
FROM workflowitem INNER JOIN cwf_collectionrole ON workflowitem.collection_id = cwf_collectionrole.collection_id
WHERE workflowitem.owner IS NULL AND workflowitem.state = 5 AND cwf_collectionrole.role_id = 'finaleditor';

-- Delete existing workflowitem policies
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
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM (cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id),
(VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);

-- Bundles
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM
(
	(cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
), (VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);

-- Bitstreams
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
temptable.action_id AS action_id,
cwf_claimtask.owner_id AS eperson_id
FROM
(
	((cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
	INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
), (VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);


-- Create policies for pooled tasks

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM (cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id),
(VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);

-- Bundles
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM
(
	(cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
), (VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);

-- Bitstreams
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, epersongroup_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
temptable.action_id AS action_id,
cwf_pooltask.group_id AS epersongroup_id
FROM
(
	((cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id)
	INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
	INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
), (VALUES (0), (1), (2), (3), (4)) AS temptable(action_id);



-- Create policies for submitter
-- TODO: only add if unique
INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
2 AS resource_type_id,
cwf_workflowitem.item_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM (cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id);

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
1 AS resource_type_id,
item2bundle.bundle_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM ((cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id)
      INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id
     );

INSERT INTO resourcepolicy (policy_id, resource_type_id, resource_id, action_id, eperson_id)
SELECT
getnextid('resourcepolicy') AS policy_id,
0 AS resource_type_id,
bundle2bitstream.bitstream_id AS resource_id,
0 AS action_id,
item.submitter_id AS eperson_id
FROM (((cwf_workflowitem INNER JOIN item ON cwf_workflowitem.item_id = item.item_id)
      INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id)
      INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id
     );

INSERT INTO cwf_in_progress_user (in_progress_user_id, workflowitem_id, user_id, finished)
SELECT
  getnextid('cwf_in_progress_user') AS in_progress_user_id,
  cwf_workflowitem.workflowitem_id AS workflowitem_id,
  cwf_claimtask.owner_id AS user_id,
  BOOL(0) as finished
FROM
  (cwf_claimtask INNER JOIN cwf_workflowitem ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id);


-- Delete the old tasks and workflowitems
-- This is important because otherwise the item can not be deleted
DELETE FROM tasklistitem;
DELETE FROM workflowitem;

-- Update the sequences
SELECT setval('cwf_workflowitem_seq', max(workflowitem_id)) FROM cwf_workflowitem;
SELECT setval('cwf_collectionrole_seq', max(collectionrole_id)) FROM cwf_collectionrole;
SELECT setval('cwf_workflowitemrole_seq', max(workflowitemrole_id)) FROM cwf_workflowitemrole;
SELECT setval('cwf_pooltask_seq', max(pooltask_id)) FROM cwf_pooltask;
SELECT setval('cwf_claimtask_seq', max(claimtask_id)) FROM cwf_claimtask;
SELECT setval('cwf_in_progress_user_seq', max(in_progress_user_id)) FROM cwf_in_progress_user;
