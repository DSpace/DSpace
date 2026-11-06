--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- ADD workflow_id and step_id into cwf_workflowitem table, to store the
-- workflow and step that each workflow item is currently in.
-----------------------------------------------------------------------------------

ALTER TABLE cwf_workflowitem ADD COLUMN IF NOT EXISTS workflow_id VARCHAR2(256);
ALTER TABLE cwf_workflowitem ADD COLUMN IF NOT EXISTS step_id VARCHAR2(256);

-----------------------------------------------------------------------------------
-- Fill the new columns for the workflow items already in workflow, using
-- their claimed tasks first and, if there are none, their pooled tasks.
-- All the tasks of a workflow item belong to the same workflow and step.
-----------------------------------------------------------------------------------

UPDATE cwf_workflowitem SET
    workflow_id = (SELECT MIN(workflow_id) FROM cwf_claimtask
                   WHERE cwf_claimtask.workflowitem_id = cwf_workflowitem.workflowitem_id),
    step_id = (SELECT MIN(step_id) FROM cwf_claimtask
               WHERE cwf_claimtask.workflowitem_id = cwf_workflowitem.workflowitem_id)
WHERE step_id IS NULL;

UPDATE cwf_workflowitem SET
    workflow_id = (SELECT MIN(workflow_id) FROM cwf_pooltask
                   WHERE cwf_pooltask.workflowitem_id = cwf_workflowitem.workflowitem_id),
    step_id = (SELECT MIN(step_id) FROM cwf_pooltask
               WHERE cwf_pooltask.workflowitem_id = cwf_workflowitem.workflowitem_id)
WHERE step_id IS NULL;
