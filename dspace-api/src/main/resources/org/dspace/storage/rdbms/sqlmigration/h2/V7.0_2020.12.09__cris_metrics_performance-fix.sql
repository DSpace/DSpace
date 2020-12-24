--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------

ALTER TABLE cris_metrics DROP CONSTRAINT cris_metrics_resource_id_fkey;
ALTER TABLE cris_metrics ADD CONSTRAINT cris_metrics_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES item ON DELETE CASCADE;
