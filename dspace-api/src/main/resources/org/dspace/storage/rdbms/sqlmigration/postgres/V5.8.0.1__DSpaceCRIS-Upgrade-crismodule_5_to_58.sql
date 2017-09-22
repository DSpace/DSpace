--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	ALTER TABLE CRIS_RP_PROP DROP CONSTRAINT fkc8a841f5e52079d7f40bfc5e;
	ALTER TABLE CRIS_RP_PROP ADD CONSTRAINT fkc8a841f5e52079d7f40bfc5e FOREIGN KEY (value_id) REFERENCES JDYNA_VALUES (id) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;
	ALTER TABLE cris_do_prop DROP CONSTRAINT fkc8a841f5e52079d7dbfe631;
	ALTER TABLE cris_do_prop ADD CONSTRAINT fkc8a841f5e52079d7dbfe631 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;
	ALTER TABLE cris_ou_prop DROP CONSTRAINT fkc8a841f5e52079d75de185b6;
	ALTER TABLE cris_ou_prop ADD CONSTRAINT fkc8a841f5e52079d75de185b6 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;
	ALTER TABLE cris_pj_prop DROP CONSTRAINT fkc8a841f5e52079d780027222;
	ALTER TABLE cris_pj_prop ADD CONSTRAINT fkc8a841f5e52079d780027222 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED;
	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
