--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
	CREATE TABLE imp_record
	(
			  imp_id integer NOT NULL,		
			  imp_record_id integer NOT NULL,
			  imp_eperson_id integer NOT NULL,
			  imp_collection_id integer NOT NULL,
			  status VARCHAR(1),
			  operation VARCHAR(64),
			  integra boolean,
			  last_modified timestamp with time zone,
			  handle VARCHAR(64),
			  CONSTRAINT imp_id_pkey PRIMARY KEY (imp_id),  
			  CONSTRAINT imp_eperson_id_fkey FOREIGN KEY (imp_eperson_id)
			      REFERENCES eperson (eperson_id) INITIALLY DEFERRED DEFERRABLE,
			  CONSTRAINT imp_collection_id_fkey FOREIGN KEY (imp_collection_id)
			      REFERENCES collection (collection_id)
	);
	
	CREATE TABLE imp_record_to_item
	(
			  imp_record_id integer NOT NULL,	
			  imp_item_id integer NOT NULL,	
			  CONSTRAINT imp_record_to_item_pkey PRIMARY KEY (imp_record_id)
	);
			
	CREATE TABLE imp_metadatavalue
	(  
		  imp_metadatavalue_id integer NOT NULL,
		  imp_id integer NOT NULL,
		  imp_schema VARCHAR(128) NOT NULL,
		  imp_element VARCHAR(128) NOT NULL,
		  imp_qualifier VARCHAR(128),
		  imp_value text NOT NULL,
		  imp_authority VARCHAR(256),
		  imp_confidence integer,
		  imp_share integer,
		  metadata_order integer NOT NULL,
		  text_lang VARCHAR(32),
		  CONSTRAINT imp_metadatavalue_id_pkey PRIMARY KEY (imp_metadatavalue_id),
		  CONSTRAINT imp_id_mv_fkey FOREIGN KEY (imp_id)
		      REFERENCES imp_record (imp_id)
	);		
	
	CREATE TABLE imp_bitstream
	(
		  imp_bitstream_id integer NOT NULL,
		  imp_id integer NOT NULL,
		  filepath VARCHAR(512) NOT NULL,
		  description VARCHAR(512),
		  bundle VARCHAR(512),
		  bitstream_order integer,
		  primary_bitstream boolean,
		  assetstore integer DEFAULT -1,
		  name VARCHAR(512),
		  imp_blob text,
		  embargo_policy INTEGER DEFAULT -1,
		  embargo_start_date  VARCHAR(100),
		  CONSTRAINT imp_bitstream_id_pkey PRIMARY KEY (imp_bitstream_id),
		  CONSTRAINT imp_id_bi_fkey FOREIGN KEY (imp_id)
		      REFERENCES imp_record (imp_id)
	);		
	
	CREATE INDEX imp_mv_idx_impid ON imp_metadatavalue (imp_id);
	CREATE UNIQUE INDEX imp_r2i_idx_itemid ON imp_record_to_item (imp_item_id ASC);
	CREATE INDEX imp_bit_idx_impid ON imp_bitstream (imp_id ASC);
	
	CREATE SEQUENCE imp_record_seq;
	CREATE SEQUENCE imp_metadatavalue_seq;
	CREATE SEQUENCE imp_bitstream_seq;

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
