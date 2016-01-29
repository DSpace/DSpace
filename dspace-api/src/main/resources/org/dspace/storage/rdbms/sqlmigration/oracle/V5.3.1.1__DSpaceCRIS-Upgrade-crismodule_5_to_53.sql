--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'INSERT INTO cris_metrics(
           	 	id, metriccount, enddate, remark, resourceid, resourcetypeid, startdate, 
            	timestampcreated, timestamplastmodified, metrictype, uuid)
			SELECT CRIS_METRICS_SEQ.nextval, pmc.numcitations as count, CURRENT_TIMESTAMP, null, pmc1.element as itemId, 2, null, CURRENT_TIMESTAMP, null, 'pubmed', hh.handle as handle
				from cris_pmc_citation pmc join cris_pmc_citation_itemids pmc1 on pmc.pubmedid = pmc1.cris_pmc_citation_pubmedid join handle hh on pmc1.cris_pmc_citation_pubmedid = hh.resource_id where hh.resource_type_id = 2;
		';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

ALTER TABLE cris_pmc_citation DROP COLUMN IF EXISTS numcitations;