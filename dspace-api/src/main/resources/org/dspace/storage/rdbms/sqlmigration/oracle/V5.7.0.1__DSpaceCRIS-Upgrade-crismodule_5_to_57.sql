--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ("system-orcid-token-orcid-works-create","system-orcid-token-funding-create","system-orcid-token-funding-update","system-orcid-token-activities-update")));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ("system-orcid-token-orcid-works-create","system-orcid-token-funding-create","system-orcid-token-funding-update","system-orcid-token-activities-update"));
	DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN ("system-orcid-token-orcid-works-create","system-orcid-token-funding-create","system-orcid-token-funding-update","system-orcid-token-activities-update");
	UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-activities-update" WHERE SHORTNAME = "system-orcid-token-orcid-works-update";
	UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-person-update" WHERE SHORTNAME = "system-orcid-token-orcid-bio-update";
	UPDATE CRIS_RPAGE_PDEF SET SHORTNAME = "system-orcid-token-read-limited" WHERE SHORTNAME = "system-orcid-token-orcid-profile-read-limited";'
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;