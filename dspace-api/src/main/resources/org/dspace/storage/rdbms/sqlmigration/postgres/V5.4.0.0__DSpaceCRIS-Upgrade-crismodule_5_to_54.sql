--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
    ALTER TABLE jdyna_widget_text ALTER COLUMN displayformat TYPE varchar(4000);
	ALTER TABLE jdyna_widget_text ALTER COLUMN regex TYPE varchar(4000);
	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
