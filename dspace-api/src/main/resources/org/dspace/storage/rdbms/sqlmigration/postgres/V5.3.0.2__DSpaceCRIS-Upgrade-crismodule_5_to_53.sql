--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

create table IF NOT EXISTS cris_orcid_history (id int4 not null, owner varchar(255), entityId int4, typeId int4, responseMessage text, lastAttempt timestamp, lastSuccess timestamp, primary key (id));
create table IF NOT EXISTS cris_orcid_queue (id int4 not null, owner varchar(255), entityId int4, typeId int4, mode varchar(255), fastlookupobjectname text, fastlookupuuid varchar(255), primary key (id));

create table IF NOT EXISTS jdyna_widget_checkradio (id int4 not null, option4row integer, staticValues text, dropdown boolean, primary key (id));
alter table jdyna_widget_checkradio drop column IF EXISTS query;


do $$
begin
    create sequence CRIS_ORCIDHISTORY_SEQ; 
 
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';

do $$
begin
    create sequence CRIS_ORCIDQUEUE_SEQ; 
 
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';

do $$
begin
    
    alter table jdyna_widget_checkradio add column dropdown boolean;
 
 	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
