--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--------------------------------------------------------------------
-- DS-3730 Index on given tables to speed up resource policy queries
--------------------------------------------------------------------


DROP INDEX IF EXISTS resourcepolicy_idx_start_date;

CREATE INDEX resourcepolicy_idx_start_date ON resourcepolicy(start_date);

DROP INDEX IF EXISTS resourcepolicy_idx_end_date;

CREATE INDEX resourcepolicy_idx_end_date ON resourcepolicy(end_date);

DROP INDEX IF EXISTS resourcepolicy_idx_action_id;

CREATE INDEX resourcepolicy_idx_action_id ON resourcepolicy(action_id);