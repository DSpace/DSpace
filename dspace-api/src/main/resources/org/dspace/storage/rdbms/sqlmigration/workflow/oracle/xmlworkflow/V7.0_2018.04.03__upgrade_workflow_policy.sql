--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- UPDATE policies for claimtasks
-- Item
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT cwf_workflowitem.item_id FROM cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id JOIN item ON cwf_workflowitem.item_id = item.uuid) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);

-- Bundles
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT item2bundle.bundle_id FROM cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);

-- Bitstreams
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT bundle2bitstream.bitstream_id FROM cwf_workflowitem INNER JOIN cwf_claimtask ON cwf_workflowitem.workflowitem_id = cwf_claimtask.workflowitem_id INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);

-- Create policies for pooled tasks
-- Item
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT cwf_workflowitem.item_id FROM cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);

-- Bundles
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT cwf_workflowitem.item_id FROM cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);

-- Bitstreams
UPDATE RESOURCEPOLICY SET rptype = 'TYPE_WORKFLOW' WHERE dspace_object in (SELECT cwf_workflowitem.item_id FROM cwf_workflowitem INNER JOIN cwf_pooltask ON cwf_workflowitem.workflowitem_id = cwf_pooltask.workflowitem_id INNER JOIN item2bundle ON cwf_workflowitem.item_id = item2bundle.item_id INNER JOIN bundle2bitstream ON item2bundle.bundle_id = bundle2bitstream.bundle_id) AND eperson_id not in (SELECT item.submitter_id FROM cwf_workflowitem JOIN item ON cwf_workflowitem.item_id = item.uuid);
