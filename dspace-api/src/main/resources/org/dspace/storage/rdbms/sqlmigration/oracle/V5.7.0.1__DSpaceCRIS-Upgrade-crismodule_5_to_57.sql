--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-activities-update" WHERE SHORTNAME = "system-orcid-token-orcid-works-update";
	UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-person-update" WHERE SHORTNAME = "system-orcid-token-orcid-bio-update";
	UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-read-limited" WHERE SHORTNAME = "system-orcid-token-orcid-profile-read-limited";'
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;