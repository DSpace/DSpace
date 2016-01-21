--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-2701 Service based API / Hibernate integration
------------------------------------------------------
UPDATE collection SET workflow_step_1 = null;
UPDATE collection SET workflow_step_2 = null;
UPDATE collection SET workflow_step_3 = null;

-- cwf_workflowitem

ALTER TABLE cwf_workflowitem RENAME COLUMN item_id to item_legacy_id;
ALTER TABLE cwf_workflowitem ADD COLUMN item_id UUID REFERENCES Item(uuid);
UPDATE cwf_workflowitem SET item_id = (SELECT item.uuid FROM item WHERE cwf_workflowitem.item_legacy_id = item.item_id);
ALTER TABLE cwf_workflowitem DROP COLUMN item_legacy_id;

ALTER TABLE cwf_workflowitem RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE cwf_workflowitem ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
UPDATE cwf_workflowitem SET collection_id = (SELECT collection.uuid FROM collection WHERE cwf_workflowitem.collection_legacy_id = collection.collection_id);
ALTER TABLE cwf_workflowitem DROP COLUMN collection_legacy_id;

UPDATE cwf_workflowitem SET multiple_titles = FALSE WHERE multiple_titles IS NULL;
UPDATE cwf_workflowitem SET published_before = FALSE WHERE published_before IS NULL;
UPDATE cwf_workflowitem SET multiple_files = FALSE WHERE multiple_files IS NULL;

-- cwf_collectionrole
ALTER TABLE cwf_collectionrole RENAME COLUMN collection_id to collection_legacy_id;
ALTER TABLE cwf_collectionrole ADD COLUMN collection_id UUID REFERENCES Collection(uuid);
UPDATE cwf_collectionrole SET collection_id = (SELECT collection.uuid FROM collection WHERE cwf_collectionrole.collection_legacy_id = collection.collection_id);
ALTER TABLE cwf_collectionrole DROP COLUMN collection_legacy_id;

ALTER TABLE cwf_collectionrole RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_collectionrole ADD COLUMN group_id UUID REFERENCES epersongroup(uuid);
UPDATE cwf_collectionrole SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_collectionrole.group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE cwf_collectionrole DROP COLUMN group_legacy_id;


-- cwf_workflowitemrole
ALTER TABLE cwf_workflowitemrole RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_workflowitemrole ADD COLUMN group_id UUID REFERENCES epersongroup(uuid);
UPDATE cwf_workflowitemrole SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_workflowitemrole.group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE cwf_workflowitemrole DROP COLUMN group_legacy_id;

ALTER TABLE cwf_workflowitemrole RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE cwf_workflowitemrole ADD COLUMN eperson_id UUID REFERENCES eperson(uuid);
UPDATE cwf_workflowitemrole SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE cwf_workflowitemrole.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE cwf_workflowitemrole DROP COLUMN eperson_legacy_id;

-- cwf_pooltask
ALTER TABLE cwf_pooltask RENAME COLUMN group_id to group_legacy_id;
ALTER TABLE cwf_pooltask ADD COLUMN group_id UUID REFERENCES epersongroup(uuid);
UPDATE cwf_pooltask SET group_id = (SELECT epersongroup.uuid FROM epersongroup WHERE cwf_pooltask.group_legacy_id = epersongroup.eperson_group_id);
ALTER TABLE cwf_pooltask DROP COLUMN group_legacy_id;

ALTER TABLE cwf_pooltask RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE cwf_pooltask ADD COLUMN eperson_id UUID REFERENCES eperson(uuid);
UPDATE cwf_pooltask SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE cwf_pooltask.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE cwf_pooltask DROP COLUMN eperson_legacy_id;

-- cwf_claimtask
ALTER TABLE cwf_claimtask RENAME COLUMN owner_id to eperson_legacy_id;
ALTER TABLE cwf_claimtask ADD COLUMN owner_id UUID REFERENCES eperson(uuid);
UPDATE cwf_claimtask SET owner_id = (SELECT eperson.uuid FROM eperson WHERE cwf_claimtask.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE cwf_claimtask DROP COLUMN eperson_legacy_id;


-- cwf_in_progress_user
ALTER TABLE cwf_in_progress_user RENAME COLUMN user_id to eperson_legacy_id;
ALTER TABLE cwf_in_progress_user ADD COLUMN user_id UUID REFERENCES eperson(uuid);
UPDATE cwf_in_progress_user SET user_id = (SELECT eperson.uuid FROM eperson WHERE cwf_in_progress_user.eperson_legacy_id = eperson.eperson_id);
ALTER TABLE cwf_in_progress_user DROP COLUMN eperson_legacy_id;
UPDATE cwf_in_progress_user SET finished = FALSE WHERE finished IS NULL;

