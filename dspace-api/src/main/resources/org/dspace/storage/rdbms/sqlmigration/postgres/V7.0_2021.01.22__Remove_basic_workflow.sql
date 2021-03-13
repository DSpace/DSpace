--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Drop the 'workflowitem' and 'tasklistitem' tables
-----------------------------------------------------------------------------------

DROP TABLE workflowitem CASCADE;
DROP TABLE tasklistitem CASCADE;

DROP SEQUENCE workflowitem_seq;
DROP SEQUENCE tasklistitem_seq;