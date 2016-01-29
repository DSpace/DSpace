--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_widget_text ALTER COLUMN displayformat varchar2(4000 char)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_widget_text ALTER COLUMN regex varchar2(4000 char)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'ALTER TABLE cris_metrics ADD COLUMN last NUMBER(1)';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

