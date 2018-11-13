--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

UPDATE cris_rp_pdef set shortname = 'iso-country' where shortname = 'iso-3166-country';
UPDATE cris_ou_pdef set shortname = 'iso-country' where shortname = 'iso-3166-country';
UPDATE cris_rp_pdef set shortname = 'orcid-profile-pref-iso-country' where shortname = 'orcid-profile-pref-iso-3166-country';