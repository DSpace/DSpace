--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE TABLE dedup_reject
		(
		  dedup_reject_id NUMBER(*,0) NOT NULL,
		  first_item_id NUMBER(*,0),
		  second_item_id NUMBER(*,0),  
		  eperson_id NUMBER(*,0),  
		  fake  NUMBER(1,0),
		  tofix NUMBER(1,0),
		  note VARCHAR2(256 BYTE),
		  admin_id NUMBER(*,0),
		  admin_time TIMESTAMP (6),
		  reader_id NUMBER(*,0),
		  reader_time TIMESTAMP (6),
		  reader_note VARCHAR2(256 BYTE),
		  reject_time TIMESTAMP (6),
		  resource_type_id NUMBER(*,0),		  
	  	  submitter_decision VARCHAR2(256 BYTE),
	  	  workflow_decision VARCHAR2(256 BYTE),
	  	  admin_decision VARCHAR2(256 BYTE),
		  CONSTRAINT dedup_reject_id_pkey PRIMARY KEY (dedup_reject_id)
		)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX dedup_reject_firstid_idx ON dedup_reject(first_item_id)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX dedup_reject_secondid_idx ON dedup_reject(second_item_id)';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE INDEX dedup_reject_combo_idx ON dedup_reject(first_item_id,second_item_id)';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'CREATE SEQUENCE dedup_reject_seq';		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;
