--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DECLARE
	system-orcid-token-orcid-works-create varchar2(255);
	system-orcid-token-funding-create varchar2(255);
	system-orcid-token-funding-update varchar2(255);
	system-orcid-token-activities-update varchar2(255);
	orcid-profile-pref-biography varchar2(255);
	orcid-profile-pref-email varchar2(255);
	orcid-profile-pref-fullName varchar2(255);
	orcid-profile-pref-preferredName varchar2(255);
	orcid-profile-pref-otheremails varchar2(255);
	system-orcid-token-orcid-works-update varchar2(255);
	system-orcid-token-activities-update varchar2(255);
	system-orcid-token-person-update varchar2(255);
	system-orcid-token-orcid-bio-update varchar2(255);
	system-orcid-token-read-limited varchar2(255);
	system-orcid-token-orcid-profile-read-limited varchar2(255);
	system-orcid-profile-pref-biography varchar2(255);
	orcid-profile-pref-biography varchar2(255);
	system-orcid-profile-pref-email varchar2(255);
	orcid-profile-pref-email varchar2(255);
	system-orcid-profile-pref-fullName varchar2(255);
	orcid-profile-pref-fullNamevarchar2(255);
	system-orcid-profile-pref-preferredName varchar2(255);
	orcid-profile-pref-preferredName varchar2(255);
	system-orcid-profile-pref-otheremails varchar2(255);
	orcid-profile-pref-otheremails varchar2(255);
	city varchar2(255);
	country varchar2(255);
BEGIN
	system-orcid-token-orcid-works-create := 'system-orcid-token-orcid-works-create';
	system-orcid-token-funding-create := 'system-orcid-token-funding-create';
	system-orcid-token-funding-update := 'system-orcid-token-funding-update';
	system-orcid-token-activities-update := 'system-orcid-token-activities-update';
	orcid-profile-pref-biography := 'orcid-profile-pref-biography';
	orcid-profile-pref-email := 'orcid-profile-pref-email';
	orcid-profile-pref-fullName := 'orcid-profile-pref-fullName';
	orcid-profile-pref-preferredName := 'orcid-profile-pref-preferredName';
	orcid-profile-pref-otheremails := 'orcid-profile-pref-otheremails';
	system-orcid-token-orcid-works-update := 'system-orcid-token-orcid-works-update';
	system-orcid-token-activities-update := 'system-orcid-token-activities-update';
	system-orcid-token-person-update := 'system-orcid-token-person-update';
	system-orcid-token-orcid-bio-update := 'system-orcid-token-orcid-bio-update';
	system-orcid-token-read-limited := 'system-orcid-token-read-limited';
	system-orcid-token-orcid-profile-read-limited := 'system-orcid-token-orcid-profile-read-limited';
	system-orcid-profile-pref-biography := 'system-orcid-profile-pref-biography';
	orcid-profile-pref-biography := 'orcid-profile-pref-biography';
	system-orcid-profile-pref-email := 'system-orcid-profile-pref-email';
	orcid-profile-pref-email := 'orcid-profile-pref-email';
	system-orcid-profile-pref-fullName := 'system-orcid-profile-pref-fullName';
	orcid-profile-pref-fullName := 'orcid-profile-pref-fullName';
	system-orcid-profile-pref-preferredName := 'system-orcid-profile-pref-preferredName';
	orcid-profile-pref-preferredName := 'orcid-profile-pref-preferredName';
	system-orcid-profile-pref-otheremails := 'system-orcid-profile-pref-otheremails';
	orcid-profile-pref-otheremails := 'orcid-profile-pref-otheremails';
	city := 'city';
	country := 'iso-3166-country';
	EXECUTE IMMEDIATE
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('||system-orcid-token-orcid-works-create||','||system-orcid-token-funding-create||','||system-orcid-token-funding-update||','||system-orcid-token-activities-update||')));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('||system-orcid-token-orcid-works-create||','||system-orcid-token-funding-create||','||system-orcid-token-funding-update||','||system-orcid-token-activities-update||'));
	DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('system-orcid-token-orcid-works-create','system-orcid-token-funding-create','system-orcid-token-funding-update','system-orcid-token-activities-update'));
	DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('||system-orcid-token-orcid-works-create||','||system-orcid-token-funding-create||','||system-orcid-token-funding-update||','||system-orcid-token-activities-update||');
	DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('||orcid-profile-pref-biography||','||orcid-profile-pref-email||','||orcid-profile-pref-fullName||','||orcid-profile-pref-preferredName||','||orcid-profile-pref-otheremails||')));
	DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('||orcid-profile-pref-biography||',||'orcid-profile-pref-email||','||orcid-profile-pref-fullName||','||orcid-profile-pref-preferredName||','||orcid-profile-pref-otheremails||'));	
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-token-activities-update||' WHERE SHORTNAME = '||system-orcid-token-orcid-works-update||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-token-person-update||' WHERE SHORTNAME = '||system-orcid-token-orcid-bio-update||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-token-read-limited||' WHERE SHORTNAME = '||system-orcid-token-orcid-profile-read-limited||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-profile-pref-biography||' WHERE SHORTNAME = '||orcid-profile-pref-biography||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-profile-pref-email||' WHERE SHORTNAME = '||orcid-profile-pref-email||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-profile-pref-fullName||' WHERE SHORTNAME = '||orcid-profile-pref-fullName||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-profile-pref-preferredName||' WHERE SHORTNAME = '||orcid-profile-pref-preferredName||';
	UPDATE CRIS_RP_PDEF SET SHORTNAME = '||system-orcid-profile-pref-otheremails||' WHERE SHORTNAME = '||orcid-profile-pref-otheremails||';
	ALTER TABLE CRIS_ORCID_HISTORY ADD COLUMN orcid varchar2(255);
	ALTER TABLE CRIS_ORCID_HISTORY DROP COLUMN IF EXISTS entityid;
	UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = '||city||';
	UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = '||country||';
	DELETE FROM CRIS_ORCID_HISTORY;'	
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;