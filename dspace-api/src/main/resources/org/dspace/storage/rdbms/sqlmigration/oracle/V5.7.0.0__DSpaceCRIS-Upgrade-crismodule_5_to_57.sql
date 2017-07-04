--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
'ALTER TABLE imp_bitstream ADD COLUMN md5value VARCHAR2(32);'
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;