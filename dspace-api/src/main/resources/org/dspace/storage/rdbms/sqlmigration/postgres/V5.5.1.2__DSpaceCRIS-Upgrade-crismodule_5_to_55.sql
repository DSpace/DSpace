--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	ALTER TABLE imp_record_to_item ADD COLUMN imp_sourceref VARCHAR(256);
	ALTER TABLE imp_record ADD COLUMN imp_sourceref VARCHAR(256);
	ALTER TABLE imp_record ALTER COLUMN imp_record_id TYPE varchar(256);
	ALTER TABLE imp_record_to_item ALTER COLUMN imp_record_id TYPE varchar(256);
	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
