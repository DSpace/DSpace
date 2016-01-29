--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

	
			INSERT INTO cris_metrics(
           	 	id, metriccount, enddate, remark, resourceid, resourcetypeid, startdate, 
            	timestampcreated, timestamplastmodified, metrictype, uuid)
			SELECT getnextid('CRIS_METRICS'), pmc.numcitations as count, CURRENT_TIMESTAMP, null, pmc1.element as itemId, 2, null, CURRENT_TIMESTAMP, null, 'pubmed', hh.handle as handle
				from cris_pmc_citation pmc join cris_pmc_citation_itemids pmc1 on pmc.pubmedid = pmc1.cris_pmc_citation_pubmedid join handle hh on pmc1.cris_pmc_citation_pubmedid = hh.resource_id where hh.resource_type_id = 2;
	
           	ALTER TABLE cris_pmc_citation DROP COLUMN numcitations;

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
