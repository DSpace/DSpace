-- check
select * from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef crpdef on crpdef.id = crp.typo_id where crpdef.shortname = 'orcid';

-- rename orcid metadata link
update cris_rp_pdef set shortname = 'orcidold' where shortname = 'orcid';

-- check
select * from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef crpdef on crpdef.id = crp.typo_id where crpdef.shortname = 'orcidold';

-- WARNING (manual operation) build new metadata text orcid from webapp (reminder: custom rendering to show as a link) e.g. <a class="authority" href="http://sandbox.orcid.org/{0}" target="_blank">{0}&nbsp;<img alt="" src="../../images/mini-icon-orcid.png" style="width: 16px; height: 16px;"></a>

-- move orcid link to orcid text
update cris_rp_prop set typo_id = (select id from cris_rp_pdef where shortname = 'orcid') where typo_id = (select id from cris_rp_pdef where shortname = 'orcidold');
update jdyna_values set dtype = 'text', textvalue = linkdescription, linkdescription = null, linkvalue = null, sortvalue = lower(textvalue) where dtype = 'link' and id in (select jdv.id from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef crpdef on crpdef.id = crp.typo_id where crpdef.shortname = 'orcid');

-- check
select * from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef crpdef on crpdef.id = crp.typo_id where crpdef.shortname = 'orcid';