--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- Cleanup of enabled workflow steps in the basic workflow
--
-- This file will automatically cleanup existing
-- classic workflow steps in the collection table.
--
-- This script is called automatically by the following
-- Flyway Java migration class:
-- org.dspace.storage.rdbms.migration.V5_7_2018_11_20__Remove_Old_Workflow_Steps_From_Collections
----------------------------------------------------

UPDATE collection SET workflow_step_1 = null, workflow_step_2 = null, workflow_step_3 = null;
