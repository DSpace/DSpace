--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- remove constraint so multiple reviewers can be selected for the same workflowitem
ALTER TABLE cwf_in_progress_user DROP CONSTRAINT cwf_in_progress_user_unique;
