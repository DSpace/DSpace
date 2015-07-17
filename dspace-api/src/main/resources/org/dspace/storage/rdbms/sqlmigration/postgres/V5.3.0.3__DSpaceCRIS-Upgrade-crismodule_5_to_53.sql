--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE jdyna_widget_checkradio ALTER COLUMN dropdown TYPE boolean;

do $$
begin
    
	ALTER TABLE cris_orcid_queue ADD COLUMN fastlookupobjectname text;
	ALTER TABLE cris_orcid_queue ADD COLUMN fastlookupuuid varchar(255);
 
 	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
