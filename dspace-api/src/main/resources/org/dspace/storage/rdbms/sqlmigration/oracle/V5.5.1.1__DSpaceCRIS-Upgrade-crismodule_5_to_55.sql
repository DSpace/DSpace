--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE TABLE imp_record
			(
			  imp_id NUMBER(*,0) NOT NULL,		
			  imp_record_id NUMBER(*,0) NOT NULL,
			  imp_eperson_id NUMBER(*,0) NOT NULL,  
			  imp_collection_id NUMBER(*,0) NOT NULL,
			  status VARCHAR2(1),
			  operation VARCHAR2(64),
			  integra       NUMBER(1),
			  last_modified TIMESTAMP(6),
			  handle VARCHAR2(64),
			  CONSTRAINT imp_id_pkey PRIMARY KEY (imp_id),  
			  CONSTRAINT imp_eperson_id_fkey FOREIGN KEY (imp_eperson_id)
			      REFERENCES eperson (eperson_id) INITIALLY DEFERRED DEFERRABLE,
			  CONSTRAINT imp_collection_id_fkey FOREIGN KEY (imp_collection_id)
			      REFERENCES collection (collection_id)
			)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE TABLE imp_record_to_item
			(
			  imp_record_id NUMBER(*,0) NOT NULL,  
			  imp_item_id NUMBER(*,0) NOT NULL,
			  CONSTRAINT imp_record_to_item_pkey PRIMARY KEY (imp_record_id)
			)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;


BEGIN
	EXECUTE IMMEDIATE
    	'CREATE TABLE imp_metadatavalue
		(  
		  imp_metadatavalue_id NUMBER(*,0) NOT NULL,
		  imp_id NUMBER(*,0)  NOT NULL,
		  imp_schema VARCHAR2(128) NOT NULL,
		  imp_element VARCHAR2(128) NOT NULL,
		  imp_qualifier VARCHAR2(128),
		  imp_value CLOB NOT NULL,
		  imp_authority VARCHAR2(256),
		  imp_confidence NUMBER(*,0),
		  imp_share NUMBER(38,0) NULL,
		  metadata_order NUMBER(*,0) NOT NULL,
		  text_lang VARCHAR2(32),
		  CONSTRAINT imp_metadatavalue_id_pkey PRIMARY KEY (imp_metadatavalue_id),
		  CONSTRAINT imp_id_mv_fkey FOREIGN KEY (imp_id)
		      REFERENCES imp_record (imp_id)
		)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE TABLE imp_bitstream
		(
		  imp_bitstream_id NUMBER(*,0) NOT NULL,
		  imp_id NUMBER(*,0) NOT NULL,
		  filepath VARCHAR2(512) NOT NULL,
		  description VARCHAR2(512),
		  bundle VARCHAR2(512),
		  bitstream_order NUMBER(*,0),
		  primary_bitstream NUMBER(1),
		  assetstore INTEGER default -1,
		  "NAME" VARCHAR2(512),
		  imp_blob BLOB,
		  embargo_policy INTEGER DEFAULT -1,
		  embargo_start_date  VARCHAR2(100),
		  CONSTRAINT imp_bitstream_id_pkey PRIMARY KEY (imp_bitstream_id),
		  CONSTRAINT imp_id_bi_fkey FOREIGN KEY (imp_id)
		      REFERENCES imp_record (imp_id)
		)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;


BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX imp_mv_idx_impid ON IMP_METADATAVALUE (IMP_ID)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE UNIQUE INDEX "imp_r2i_idx_itemid" ON "IMP_RECORD_TO_ITEM" ("IMP_ITEM_ID" ASC)';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX "imp_bit_idx_impid" ON "IMP_BITSTREAM" ("IMP_ID" ASC)';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE SEQUENCE imp_record_seq';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE SEQUENCE imp_metadatavalue_seq';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE SEQUENCE imp_bitstream_seq';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;
