--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Update table CrisMetrics
-----------------------------------------------------------------------------------

ALTER TABLE cris_metrics ADD COLUMN deltaPeriod1 FLOAT;

ALTER TABLE cris_metrics ADD COLUMN deltaPeriod2 FLOAT;

ALTER TABLE cris_metrics ADD COLUMN rank FLOAT;
