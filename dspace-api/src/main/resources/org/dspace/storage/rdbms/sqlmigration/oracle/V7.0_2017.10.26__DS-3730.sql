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


CREATE INDEX idx_resourcepolicy ON resourcepolicy(start_date, end_date, action_id);