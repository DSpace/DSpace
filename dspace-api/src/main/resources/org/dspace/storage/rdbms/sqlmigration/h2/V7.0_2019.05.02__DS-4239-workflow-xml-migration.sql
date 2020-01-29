--
-- The contents of this file are subject to the license and copyright                                                                                                                   
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

---------------------------------------------------------------
-- DS-4239 Migrate the workflow.xml to spring
---------------------------------------------------------------
-- This script will rename the default workflow "default" name
-- to the new "defaultWorkflow" identifier
---------------------------------------------------------------

UPDATE cwf_pooltask SET workflow_id='defaultWorkflow' WHERE workflow_id='default';
UPDATE cwf_claimtask SET workflow_id='defaultWorkflow' WHERE workflow_id='default';