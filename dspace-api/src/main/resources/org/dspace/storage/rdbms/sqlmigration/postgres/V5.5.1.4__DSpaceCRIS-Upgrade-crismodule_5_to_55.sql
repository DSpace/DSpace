--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
-- DOI2ITEM
CREATE TABLE doi2item
(
  id integer NOT NULL,
  item_id integer,
  eperson_id integer,    
  request_date timestamp with time zone,
  last_modified timestamp with time zone,
  identifier_doi text,
  criteria VARCHAR(256),
  response_code VARCHAR(256),
  service VARCHAR(256),
  note text,
  filename VARCHAR(256),
  CONSTRAINT id PRIMARY KEY (id),
  CONSTRAINT identifier_doi UNIQUE (identifier_doi),
  CONSTRAINT item_id UNIQUE (item_id)
);

CREATE SEQUENCE doi2item_seq;

insert into metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note)
select 
    NEXTVAL('metadatafieldregistry_seq'), 1, 'utils', 
    'nodoi', null
where not exists (
    SELECT * FROM metadatafieldregistry WHERE element = 'utils' AND qualifier = 'nodoi'
);

insert into metadatafieldregistry (metadata_field_id, metadata_schema_id, element, qualifier, scope_note)
select 
    NEXTVAL('metadatafieldregistry_seq'), 1, 'utils', 
    'processdoi', null
where not exists (
    SELECT * FROM metadatafieldregistry WHERE element = 'utils' AND qualifier = 'processdoi'
);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
