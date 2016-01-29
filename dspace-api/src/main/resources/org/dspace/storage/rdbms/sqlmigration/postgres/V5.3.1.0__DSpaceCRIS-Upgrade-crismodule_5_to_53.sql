--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
    
create table cris_metrics (
        id int4 not null,
        metricCount float8 not null,
        endDate timestamp,
        remark text,
        resourceId int4,
        resourceTypeId int4,
        startDate timestamp,
        timestampCreated timestamp,
        timestampLastModified timestamp,
        metricType varchar(255),
        uuid varchar(255),
        primary key (id)
    );

    create sequence CRIS_METRICS_SEQ;  
 	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';