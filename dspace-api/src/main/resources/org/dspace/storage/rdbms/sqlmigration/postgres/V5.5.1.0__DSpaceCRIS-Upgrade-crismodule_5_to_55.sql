--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	-- DEDUPLICATION
	CREATE TABLE dedup_reject
	(
	  dedup_reject_id integer NOT NULL,
	  first_item_id integer,
	  second_item_id integer,  
	  eperson_id integer,  
	  fake boolean,
	  tofix boolean,
	  note VARCHAR(256),
	  admin_id integer,
	  admin_time timestamp with time zone,
	  reader_id integer,
	  reader_time timestamp with time zone,
	  reader_note VARCHAR(256),
	  reject_time timestamp with time zone,
	  resource_type_id integer,
	  submitter_decision VARCHAR(256),
	  workflow_decision VARCHAR(256),
	  admin_decision VARCHAR(256), 
	  CONSTRAINT dedup_reject_id_pkey PRIMARY KEY (dedup_reject_id)
	);
	
	CREATE INDEX dedup_reject_firstid_idx ON dedup_reject(first_item_id);
	CREATE INDEX dedup_reject_secondid_idx  ON dedup_reject(second_item_id);
	CREATE INDEX dedup_reject_combo_idx
	  ON dedup_reject
	  USING btree
	  (first_item_id,second_item_id);
	
	CREATE SEQUENCE dedup_reject_seq;

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
