--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

	-- delete scopes	
	DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-orcid-works-update')));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-orcid-works-update'));
    DELETE FROM CRIS_RP_BOX2CON WHERE jdyna_containable_id  IN (SELECT ID FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-orcid-works-update')));
	DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-orcid-works-update'));
	DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-orcid-works-update');

	-- delete metadata configuration
	DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName')));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName'));
	
	-- upgrade scopes
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-token-activities-update' WHERE SHORTNAME = 'system-orcid-token-orcid-works-update';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-token-person-update' WHERE SHORTNAME = 'system-orcid-token-orcid-bio-update';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-token-read-limited' WHERE SHORTNAME = 'system-orcid-token-orcid-profile-read-limited';
	
	-- upgrade metadata not editable
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-profile-pref-biography' WHERE SHORTNAME = 'orcid-profile-pref-biography';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-profile-pref-email' WHERE SHORTNAME = 'orcid-profile-pref-email';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-profile-pref-fullName' WHERE SHORTNAME = 'orcid-profile-pref-fullName';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-profile-pref-preferredName' WHERE SHORTNAME = 'orcid-profile-pref-preferredName';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = 'system-orcid-profile-pref-otheremails' WHERE SHORTNAME = 'orcid-profile-pref-otheremails';

exception when others then
 
	DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited')));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited'));
    DELETE FROM CRIS_RP_BOX2CON WHERE jdyna_containable_id  IN (SELECT ID FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited')));
	DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited'));
	DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited');
	
end;
$$ language 'plpgsql';
