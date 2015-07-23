--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'alter table cris_orcid_history add column entityUuid varchar2(255)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;