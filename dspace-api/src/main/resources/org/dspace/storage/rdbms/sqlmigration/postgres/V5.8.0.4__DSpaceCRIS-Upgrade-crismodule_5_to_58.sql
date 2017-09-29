--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	-- need mandatory to send affiliation (employment and education) to Orcid Registry
	UPDATE CRIS_OU_PDEF SET MANDATORY = true WHERE SHORTNAME = 'city';
	UPDATE CRIS_OU_PDEF SET MANDATORY = true WHERE SHORTNAME = 'iso-3166-country';
	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
