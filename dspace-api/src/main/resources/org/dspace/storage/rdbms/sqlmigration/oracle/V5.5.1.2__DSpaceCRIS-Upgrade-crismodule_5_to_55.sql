--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'ALTER TABLE imp_record_to_item ADD COLUMN imp_sourceref VARCHAR2(256);
		 ALTER TABLE imp_record ADD COLUMN imp_sourceref VARCHAR2(256);		 
		 ALTER TABLE imp_record ALTER COLUMN imp_record_id TYPE VARCHAR2(256);
		 ALTER TABLE imp_record_to_item ALTER COLUMN imp_record_id TYPE VARCHAR2(256);
		';
		 
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;
