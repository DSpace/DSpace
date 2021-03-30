--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

SELECT * INTO bkp_schema_version
FROM schema_version;

DELETE FROM schema_version
where version not like '7%' and LOWER(description) like '%cris%';